package com.aistra.hail.utils

import androidx.room3.Database
import androidx.room3.RoomDatabase

@Database(
    entities = [AppMetadataEntity::class, ActionEntity::class, ActionDependencyEntity::class],
    version = 5,
    exportSchema = false
)
abstract class AppMetadataDatabase : RoomDatabase() {
    abstract fun appMetadataDao(): AppMetadataDao
    abstract fun actionDao(): ActionDao
}
