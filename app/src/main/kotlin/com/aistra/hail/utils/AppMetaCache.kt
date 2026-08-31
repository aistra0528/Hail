package com.aistra.hail.utils

import android.content.pm.ApplicationInfo
import androidx.room3.Room
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.aistra.hail.HailApp
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.AppManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.concurrent.ConcurrentHashMap

object AppMetaCache {
    data class Entry(
        val packageName: String,
        val name: String,
        val isSystemApp: Boolean,
        val firstInstallTime: Long,
        val lastUpdateTime: Long,
        val flags: Int,
        val enabled: Boolean,
        val installed: Boolean,
        val state: AppInfo.State,
        val sourceSignature: String
    )

    private val cache = ConcurrentHashMap<String, Entry>()
    private val installedApplications = ConcurrentHashMap<String, ApplicationInfo>()
    private val packageLocks = ConcurrentHashMap<String, Mutex>()
    private val database by lazy {
        Room.databaseBuilder(HailApp.app, AppMetadataDatabase::class.java, "app_metadata.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()
    }

    internal fun database(): AppMetadataDatabase = database
    private val scope = HailApp.app.applicationScope
    private val _revision = MutableStateFlow(0L)
    val revision: StateFlow<Long> = _revision
    private val _installedApplicationsReady = MutableStateFlow(false)
    val installedApplicationsReady: StateFlow<Boolean> = _installedApplicationsReady

    fun get(packageName: String): Entry? = cache[packageName]

    fun cachedPackageNames(): List<String> = cache.values.filter { it.installed }.map { it.packageName }

    fun cachedApplications(): List<ApplicationInfo> = installedApplications.values.toList()

    fun cachedDisplayApplications(): List<ApplicationInfo> = cache.values.filter { it.installed }.map { entry ->
        ApplicationInfo().apply {
            packageName = entry.packageName
            flags = entry.flags
        }
    }

    fun seedFromDatabase(): Job = scope.launch {
        runCatching {
            database.appMetadataDao().loadAll().forEach { entity ->
                cache[entity.packageName] = entity.toEntry()
            }
            _revision.value++
        }
    }

    fun warmUp(): Job = scope.launch {
        runCatching {
            database.appMetadataDao().loadAll().forEach { entity ->
                cache[entity.packageName] = entity.toEntry()
            }
            _revision.value++
            prefetch(HPackages.getInstalledApplications()).join()
        }
    }

    fun prefetch(applicationInfo: Collection<ApplicationInfo>): Job = scope.launch {
        val newPackageMap = applicationInfo.associateBy { it.packageName }
        newPackageMap.forEach { (packageName, info) ->
            installedApplications[packageName] = info
        }
        val toRemove = installedApplications.keys.filterNot { it in newPackageMap.keys }
        toRemove.forEach { installedApplications.remove(it) }
        applicationInfo.map { info ->
            async { loadIfStale(info.packageName, info) }
        }.awaitAll()
        val toRemoveLocks = packageLocks.keys.filterNot { it in newPackageMap.keys }
        toRemoveLocks.forEach { packageLocks.remove(it) }
        persist(newPackageMap.keys)
        AppIconCache.prefetch(HailApp.app, applicationInfo)
        _installedApplicationsReady.value = true
    }

    fun refreshInstalled(): Job = prefetch(HPackages.getInstalledApplications())

    suspend fun getInstalledApplicationsCacheFirst(forceRefresh: Boolean = false): List<ApplicationInfo> {
        val cached = cachedApplications()
        if (cached.isNotEmpty() && !forceRefresh) return cached
        if (!forceRefresh && cache.isNotEmpty()) return cachedDisplayApplications()
        val refreshed = HPackages.getInstalledApplications()
        prefetch(refreshed)
        return refreshed
    }

    fun prefetchPackages(packageNames: Collection<String>): Job = scope.launch {
        packageNames.map { packageName ->
            async { loadIfStale(packageName) }
        }.awaitAll()
        persist()
    }

    fun clearAndRebuild(): Job = scope.launch {
        cache.clear()
        packageLocks.clear()
        database.appMetadataDao().deleteAll()
        _revision.value++
        val applicationInfo = HPackages.getInstalledApplications()
        val installedPackages = applicationInfo.mapTo(HashSet()) { it.packageName }
        applicationInfo.map { info ->
            async { loadIfStale(info.packageName, info) }
        }.awaitAll()
        val toRemove = packageLocks.keys.filterNot { it in installedPackages }
        toRemove.forEach { packageLocks.remove(it) }
        persist(installedPackages)
        AppIconCache.prefetch(HailApp.app, applicationInfo)
        _installedApplicationsReady.value = true
    }

    fun invalidateState(packageNames: Collection<String>) {
        packageNames.forEach { packageName ->
            cache.computeIfPresent(packageName) { _, entry ->
                entry.copy(state = readState(packageName))
            }
        }
        _revision.value++
    }

    fun invalidateAll() {
        cache.clear()
        packageLocks.clear()
        _revision.value++
    }

    private suspend fun loadIfStale(packageName: String, knownInfo: ApplicationInfo? = null) {
        val lock = packageLocks.getOrPut(packageName) { Mutex() }
        lock.withLock {
            val info = knownInfo ?: HPackages.getApplicationInfoOrNull(packageName)
            val packageInfo = HPackages.getUnhiddenPackageInfoOrNull(packageName)
            val sourceSignature = listOf(
                info?.sourceDir,
                info?.sourceDir?.let { File(it).lastModified() },
                packageInfo?.lastUpdateTime
            ).joinToString(":")
            val current = cache[packageName]
            if (info != null && current?.sourceSignature == sourceSignature) {
                cache[packageName] = current.copy(state = readState(packageName))
                return
            }

            val entry = if (info == null) {
                current?.copy(installed = false, state = AppInfo.State.NOT_FOUND)
            } else {
                Entry(
                    packageName = packageName,
                    name = info.loadLabel(HailApp.app.packageManager).toString(),
                    isSystemApp = info.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                    firstInstallTime = packageInfo?.firstInstallTime ?: 0L,
                    lastUpdateTime = packageInfo?.lastUpdateTime ?: 0L,
                    flags = info.flags,
                    enabled = info.enabled,
                    installed = true,
                    state = readState(packageName),
                    sourceSignature = sourceSignature
                )
            }
            if (entry == null) cache.remove(packageName) else cache[packageName] = entry
            _revision.value++
        }
    }

    private fun readState(packageName: String): AppInfo.State = when {
        HPackages.getApplicationInfoOrNull(packageName) == null -> AppInfo.State.NOT_FOUND
        AppManager.isAppFrozen(packageName) -> AppInfo.State.FROZEN
        else -> AppInfo.State.UNFROZEN
    }

    private suspend fun persist(installedPackages: Set<String>? = null) {
        runCatching {
            val dao = database.appMetadataDao()
            if (installedPackages != null) {
                cache.values.forEach { entry ->
                    cache[entry.packageName] = entry.copy(installed = entry.packageName in installedPackages)
                }
            }
            dao.upsertAll(cache.values.map { it.toEntity() })
        }
    }

    private fun AppMetadataEntity.toEntry() = Entry(
        packageName = packageName,
        name = name,
        isSystemApp = systemApp,
        firstInstallTime = firstInstallTime,
        lastUpdateTime = lastUpdateTime,
        flags = flags,
        enabled = enabled,
        installed = installed,
        state = if (frozen) AppInfo.State.FROZEN else AppInfo.State.UNFROZEN,
        sourceSignature = sourceSignature
    )

    private fun Entry.toEntity() = AppMetadataEntity(
        packageName = packageName,
        name = name,
        systemApp = isSystemApp,
        firstInstallTime = firstInstallTime,
        lastUpdateTime = lastUpdateTime,
        flags = flags,
        enabled = enabled,
        installed = installed,
        frozen = state == AppInfo.State.FROZEN,
        sourceSignature = sourceSignature
    )

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE app_metadata ADD COLUMN installed INTEGER NOT NULL DEFAULT 0")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS actions (id TEXT NOT NULL, launchPackage TEXT NOT NULL, PRIMARY KEY(id))"
            )
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS action_dependencies (actionId TEXT NOT NULL, packageName TEXT NOT NULL, ordering INTEGER NOT NULL, PRIMARY KEY(actionId, packageName), FOREIGN KEY(actionId) REFERENCES actions(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
            )
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.execSQL("DROP INDEX IF EXISTS index_action_dependencies_actionId")
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE app_metadata ADD COLUMN frozen INTEGER NOT NULL DEFAULT 0")
        }
    }
}
