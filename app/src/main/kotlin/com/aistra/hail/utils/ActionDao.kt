package com.aistra.hail.utils

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction

@Dao
interface ActionDao {
    @Query("SELECT * FROM actions ORDER BY rowid")
    fun loadAll(): List<ActionEntity>

    @Query("SELECT * FROM action_dependencies WHERE actionId = :actionId ORDER BY ordering")
    fun loadDependencies(actionId: String): List<ActionDependencyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(action: ActionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertDependencies(dependencies: List<ActionDependencyEntity>)

    @Query("DELETE FROM action_dependencies WHERE actionId = :actionId")
    fun deleteDependencies(actionId: String)

    @Query("DELETE FROM actions WHERE id = :actionId")
    fun delete(actionId: String)

    @Transaction
    fun saveAction(action: ActionEntity, dependencies: List<ActionDependencyEntity>) {
        upsert(action)
        deleteDependencies(action.id)
        upsertDependencies(dependencies)
    }
}
