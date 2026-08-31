package com.aistra.hail.utils

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "app_metadata")
data class AppMetadataEntity(
    @PrimaryKey val packageName: String = "",
    val name: String = "",
    val systemApp: Boolean = false,
    val firstInstallTime: Long = 0L,
    val lastUpdateTime: Long = 0L,
    val flags: Int = 0,
    val enabled: Boolean = false,
    val installed: Boolean = false,
    @ColumnInfo(defaultValue = "0") val frozen: Boolean = false,
    val sourceSignature: String = ""
)
