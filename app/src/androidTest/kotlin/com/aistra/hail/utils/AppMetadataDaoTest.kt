package com.aistra.hail.utils

import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppMetadataDaoTest {

    private lateinit var database: AppMetadataDatabase
    private lateinit var dao: AppMetadataDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppMetadataDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.appMetadataDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndLoadEntries() = runBlocking {
        val entries = listOf(
            AppMetadataEntity(packageName = "com.example.app1", name = "App 1"),
            AppMetadataEntity(packageName = "com.example.app2", name = "App 2")
        )
        dao.upsertAll(entries)

        val all = dao.loadAll()
        assertEquals(2, all.size)
    }

    @Test
    fun upsertUpdatesExistingEntry() = runBlocking {
        val entry = AppMetadataEntity(packageName = "com.example.app1", name = "Old Name")
        dao.upsertAll(listOf(entry))
        dao.upsertAll(listOf(entry.copy(name = "New Name")))

        val all = dao.loadAll()
        assertEquals(1, all.size)
        assertEquals("New Name", all[0].name)
    }

    @Test
    fun deleteAllClearsTable() = runBlocking {
        dao.upsertAll(
            listOf(
                AppMetadataEntity(packageName = "com.example.app1", name = "App 1"),
                AppMetadataEntity(packageName = "com.example.app2", name = "App 2")
            )
        )
        dao.deleteAll()

        val all = dao.loadAll()
        assertTrue(all.isEmpty())
    }

    @Test
    fun markAllUninstalledUpdatesAll() = runBlocking {
        dao.upsertAll(
            listOf(
                AppMetadataEntity(packageName = "com.example.app1", name = "App 1", installed = true),
                AppMetadataEntity(packageName = "com.example.app2", name = "App 2", installed = true)
            )
        )
        dao.markAllUninstalled()

        val all = dao.loadAll()
        assertTrue(all.none { it.installed })
    }

    @Test
    fun replaceAllTransaction() = runBlocking {
        dao.upsertAll(
            listOf(AppMetadataEntity(packageName = "com.example.old", name = "Old App"))
        )

        val newEntries = listOf(
            AppMetadataEntity(packageName = "com.example.new1", name = "New App 1"),
            AppMetadataEntity(packageName = "com.example.new2", name = "New App 2")
        )
        dao.replaceAll(newEntries)

        val all = dao.loadAll()
        assertEquals(2, all.size)
        val packages = all.map { it.packageName }
        assertTrue(packages.contains("com.example.new1"))
        assertTrue(packages.contains("com.example.new2"))
        assertTrue(packages.none { it == "com.example.old" })
    }
}
