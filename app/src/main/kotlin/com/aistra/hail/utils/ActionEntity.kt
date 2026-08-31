package com.aistra.hail.utils

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.PrimaryKey

@Entity(tableName = "actions")
data class ActionEntity(
    @PrimaryKey val id: String = "",
    val launchPackage: String = ""
)
