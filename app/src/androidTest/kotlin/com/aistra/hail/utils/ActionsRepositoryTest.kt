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
class ActionsRepositoryTest {

    private lateinit var database: AppMetadataDatabase
    private lateinit var dao: ActionDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppMetadataDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.actionDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun saveWithDuplicateUnfreezePackages() = runBlocking {
        val action = ActionsRepository.save(
            launchPackage = "com.example.target",
            unfreezePackages = listOf("com.example.dep", "com.example.dep")
        )

        assertEquals(1, action.unfreezePackages.size)
        assertEquals("com.example.dep", action.unfreezePackages[0])
    }

    @Test
    fun saveRemovesLaunchFromUnfreeze() = runBlocking {
        val action = ActionsRepository.save(
            launchPackage = "com.example.target",
            unfreezePackages = listOf("com.example.dep", "com.example.target")
        )

        assertTrue(!action.unfreezePackages.contains("com.example.target"))
        assertEquals(1, action.unfreezePackages.size)
    }

    @Test
    fun savePreservesOrder() = runBlocking {
        val action = ActionsRepository.save(
            launchPackage = "com.example.target",
            unfreezePackages = listOf("com.example.dep1", "com.example.dep2", "com.example.dep3")
        )

        assertEquals(3, action.unfreezePackages.size)
        assertEquals("com.example.dep1", action.unfreezePackages[0])
        assertEquals("com.example.dep2", action.unfreezePackages[1])
        assertEquals("com.example.dep3", action.unfreezePackages[2])
    }

    @Test
    fun loadAllReturnsSavedActions() = runBlocking {
        ActionsRepository.save(launchPackage = "com.example.target1", unfreezePackages = listOf("com.example.dep1"))
        ActionsRepository.save(launchPackage = "com.example.target2", unfreezePackages = listOf("com.example.dep2"))

        val all = ActionsRepository.loadAll()
        assertEquals(2, all.size)
    }

    @Test
    fun deleteRemovesAction() = runBlocking {
        val action = ActionsRepository.save(
            launchPackage = "com.example.target",
            unfreezePackages = listOf("com.example.dep")
        )
        ActionsRepository.delete(action.id)

        val all = ActionsRepository.loadAll()
        assertTrue(all.isEmpty())
    }

    @Test
    fun duplicateCreatesNewAction() = runBlocking {
        val original = ActionsRepository.save(
            launchPackage = "com.example.target",
            unfreezePackages = listOf("com.example.dep")
        )
        val duplicate = ActionsRepository.duplicate(original)

        assertEquals(original.launchPackage, duplicate.launchPackage)
        assertEquals(original.unfreezePackages, duplicate.unfreezePackages)
        assertTrue(duplicate.id != original.id)

        val all = ActionsRepository.loadAll()
        assertEquals(2, all.size)
    }
}
