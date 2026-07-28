package com.aistra.hail.app

import android.content.Intent
import com.aistra.hail.BuildConfig
import com.aistra.hail.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppManager {
    val lockScreen: Boolean
        get() = when {
            HailData.workingMode.startsWith(HailData.OWNER) -> HPolicy.lockScreen
            HailData.workingMode.startsWith(HailData.DHIZUKU) -> HDhizuku.lockScreen
            HailData.workingMode.startsWith(HailData.SU) -> HShell.lockScreen
            HailData.workingMode.startsWith(HailData.SHIZUKU) -> HShizuku.lockScreen
            else -> false
        }

    fun isAppFrozen(packageName: String, userId: Int = HPackages.myUserId): Boolean = when {
        HailData.workingMode.endsWith(HailData.STOP) -> HPackages.isAppStopped(packageName, userId)
        HailData.workingMode.endsWith(HailData.DISABLE) -> HPackages.isAppDisabled(packageName, userId)
        HailData.workingMode.endsWith(HailData.HIDE) -> HPackages.isAppHidden(packageName, userId)
        HailData.workingMode.endsWith(HailData.SUSPEND) -> HPackages.isAppSuspended(packageName, userId)
        else -> HPackages.isAppDisabled(packageName, userId)
                || HPackages.isAppHidden(packageName, userId)
                || HPackages.isAppSuspended(packageName, userId)
    }

    fun setListFrozen(frozen: Boolean, vararg appInfo: AppInfo): String? {
        val excludeMe = appInfo.filter { it.packageName != BuildConfig.APPLICATION_ID }
        var i = 0
        var denied = false
        var name = String()
        when (HailData.workingMode) {
            // call setListFrozen for some batch-style working mode here
            // fallback to setAppFrozen otherwise
            else -> {
                excludeMe.forEach {
                    when {
                        setAppFrozen(it.packageName, frozen, it.userId) -> {
                            i++
                            name = it.name.toString()
                        }

                        it.applicationInfo != null -> denied = true
                    }
                }
            }
        }
        return if (denied && i == 0) null else if (i == 1) name else i.toString()
    }

    fun setAppFrozen(packageName: String, frozen: Boolean, userId: Int = HPackages.myUserId): Boolean =
        packageName != BuildConfig.APPLICATION_ID && when (HailData.workingMode) {
            HailData.MODE_OWNER_HIDE -> HPolicy.setAppHidden(packageName, frozen)
            HailData.MODE_OWNER_SUSPEND -> HPolicy.setAppSuspended(packageName, frozen)
            HailData.MODE_DHIZUKU_HIDE -> HDhizuku.setAppHidden(packageName, frozen)
            HailData.MODE_DHIZUKU_SUSPEND -> HDhizuku.setAppSuspended(packageName, frozen)
            HailData.MODE_SU_STOP -> !frozen || HShell.forceStopApp(packageName, userId)
            HailData.MODE_SU_DISABLE -> HShell.setAppDisabled(packageName, frozen, userId)
            HailData.MODE_SU_HIDE -> HShell.setAppHidden(packageName, frozen, userId)
            HailData.MODE_SU_SUSPEND -> HShell.setAppSuspended(packageName, frozen, userId)
            HailData.MODE_SHIZUKU_STOP -> !frozen || HShizuku.forceStopApp(packageName, userId)
            HailData.MODE_SHIZUKU_DISABLE -> HShizuku.setAppDisabled(packageName, frozen, userId)
            HailData.MODE_SHIZUKU_HIDE -> HShizuku.setAppHidden(packageName, frozen, userId)
            HailData.MODE_SHIZUKU_SUSPEND -> HShizuku.setAppSuspended(packageName, frozen, userId)
            HailData.MODE_ISLAND_HIDE -> HIsland.setAppHidden(packageName, frozen)
            HailData.MODE_ISLAND_SUSPEND -> HIsland.setAppSuspended(packageName, frozen)
            HailData.MODE_PRIVAPP_STOP -> !frozen || HPackages.forceStopApp(packageName, userId)
            HailData.MODE_PRIVAPP_DISABLE -> HPackages.setAppDisabled(packageName, frozen, userId)
            else -> false
        }

    fun uninstallApp(packageName: String, userId: Int = HPackages.myUserId): Boolean {
        when {
            HailData.workingMode.startsWith(HailData.OWNER) ->
                if (HPolicy.uninstallApp(packageName)) return true

            HailData.workingMode.startsWith(HailData.DHIZUKU) ->
                if (HDhizuku.uninstallApp(packageName)) return true

            HailData.workingMode.startsWith(HailData.SU) ->
                if (HShell.uninstallApp(packageName, userId)) return true

            HailData.workingMode.startsWith(HailData.SHIZUKU) ->
                if (HShizuku.uninstallApp(packageName, userId)) return true
        }
        HUI.startActivity(Intent.ACTION_DELETE, HPackages.packageUri(packageName))
        return false
    }

    fun reinstallApp(packageName: String, userId: Int = HPackages.myUserId): Boolean = when {
        HailData.workingMode.startsWith(HailData.SU) -> HShell.reinstallApp(packageName, userId)
        HailData.workingMode.startsWith(HailData.SHIZUKU) -> HShizuku.reinstallApp(packageName, userId)
        else -> false
    }

    suspend fun execute(command: String): Pair<Int, String?> = withContext(Dispatchers.IO) {
        when {
            HailData.workingMode.startsWith(HailData.SU) -> HShell.execute(command, true)
            HailData.workingMode.startsWith(HailData.SHIZUKU) -> HShizuku.execute(command)
            else -> 0 to null
        }
    }
}
