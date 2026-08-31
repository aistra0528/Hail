package com.aistra.hail.utils

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.PrimaryKey

@Entity(
    tableName = "action_dependencies",
    primaryKeys = ["actionId", "packageName"],
    foreignKeys = [ForeignKey(
        entity = ActionEntity::class,
        parentColumns = ["id"],
        childColumns = ["actionId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class ActionDependencyEntity(
    val actionId: String = "",
    val packageName: String = "",
    val ordering: Int = 0
)
