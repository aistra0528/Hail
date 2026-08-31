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
class ActionDaoTest {

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
    fun insertAndLoadAction() = runBlocking {
        val action = ActionEntity(id = "action-1", launchPackage = "com.example.target")
        dao.upsert(action)

        val all = dao.loadAll()
        assertEquals(1, all.size)
        assertEquals("action-1", all[0].id)
        assertEquals("com.example.target", all[0].launchPackage)
    }

    @Test
    fun insertAndLoadDependencies() = runBlocking {
        val action = ActionEntity(id = "action-1", launchPackage = "com.example.target")
        dao.upsert(action)
        dao.upsertDependencies(
            listOf(
                ActionDependencyEntity(actionId = "action-1", packageName = "com.example.dep1", ordering = 0),
                ActionDependencyEntity(actionId = "action-1", packageName = "com.example.dep2", ordering = 1)
            )
        )

        val deps = dao.loadDependencies("action-1")
        assertEquals(2, deps.size)
        assertEquals("com.example.dep1", deps[0].packageName)
        assertEquals("com.example.dep2", deps[1].packageName)
    }

    @Test
    fun loadAllReturnsMultipleActions() = runBlocking {
        dao.upsert(ActionEntity(id = "action-1", launchPackage = "com.example.target1"))
        dao.upsert(ActionEntity(id = "action-2", launchPackage = "com.example.target2"))

        val all = dao.loadAll()
        assertEquals(2, all.size)
    }

    @Test
    fun upsertUpdatesExistingAction() = runBlocking {
        val action = ActionEntity(id = "action-1", launchPackage = "com.example.old")
        dao.upsert(action)
        dao.upsert(action.copy(launchPackage = "com.example.new"))

        val all = dao.loadAll()
        assertEquals(1, all.size)
        assertEquals("com.example.new", all[0].launchPackage)
    }

    @Test
    fun deleteRemovesAction() = runBlocking {
        dao.upsert(ActionEntity(id = "action-1", launchPackage = "com.example.target"))
        dao.delete("action-1")

        val all = dao.loadAll()
        assertTrue(all.isEmpty())
    }

    @Test
    fun deleteDependenciesRemovesOnlySpecified() = runBlocking {
        dao.upsert(ActionEntity(id = "action-1", launchPackage = "com.example.target1"))
        dao.upsert(ActionEntity(id = "action-2", launchPackage = "com.example.target2"))
        dao.upsertDependencies(
            listOf(
                ActionDependencyEntity(actionId = "action-1", packageName = "com.example.dep1", ordering = 0),
                ActionDependencyEntity(actionId = "action-2", packageName = "com.example.dep2", ordering = 0)
            )
        )

        dao.deleteDependencies("action-1")

        assertEquals(0, dao.loadDependencies("action-1").size)
        assertEquals(1, dao.loadDependencies("action-2").size)
    }

    @Test
    fun saveActionTransaction() = runBlocking {
        val action = ActionEntity(id = "action-1", launchPackage = "com.example.target")
        val deps = listOf(
            ActionDependencyEntity(actionId = "action-1", packageName = "com.example.dep1", ordering = 0),
            ActionDependencyEntity(actionId = "action-1", packageName = "com.example.dep2", ordering = 1)
        )

        dao.saveAction(action, deps)

        val all = dao.loadAll()
        assertEquals(1, all.size)
        assertEquals(2, dao.loadDependencies("action-1").size)
    }

    @Test
    fun saveActionReplacesOldDependencies() = runBlocking {
        val action = ActionEntity(id = "action-1", launchPackage = "com.example.target")
        val oldDeps = listOf(
            ActionDependencyEntity(actionId = "action-1", packageName = "com.example.old", ordering = 0)
        )
        dao.saveAction(action, oldDeps)

        val newDeps = listOf(
            ActionDependencyEntity(actionId = "action-1", packageName = "com.example.new1", ordering = 0),
            ActionDependencyEntity(actionId = "action-1", packageName = "com.example.new2", ordering = 1)
        )
        dao.saveAction(action, newDeps)

        val loaded = dao.loadDependencies("action-1")
        assertEquals(2, loaded.size)
        assertEquals("com.example.new1", loaded[0].packageName)
        assertEquals("com.example.new2", loaded[1].packageName)
    }

    @Test
    fun duplicateActions() = runBlocking {
        dao.upsert(ActionEntity(id = "action-1", launchPackage = "com.example.target1"))
        dao.upsert(ActionEntity(id = "action-2", launchPackage = "com.example.target2"))

        val all = dao.loadAll()
        assertEquals(2, all.size)
        val ids = all.map { it.id }
        assertTrue(ids.contains("action-1"))
        assertTrue(ids.contains("action-2"))
    }
}
