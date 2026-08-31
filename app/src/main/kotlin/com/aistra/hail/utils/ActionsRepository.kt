package com.aistra.hail.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

data class LaunchAction(
    val id: String,
    val launchPackage: String,
    val unfreezePackages: List<String>
)

object ActionsRepository {
    suspend fun loadAll(): List<LaunchAction> = withContext(Dispatchers.IO) {
        val dao = AppMetaCache.database().actionDao()
        dao.loadAll().map { entity ->
            LaunchAction(
                id = entity.id,
                launchPackage = entity.launchPackage,
                unfreezePackages = dao.loadDependencies(entity.id).sortedBy { it.ordering }
                    .map { it.packageName }
            )
        }
    }

    suspend fun loadById(id: String): LaunchAction? = withContext(Dispatchers.IO) {
        val dao = AppMetaCache.database().actionDao()
        dao.loadAll().find { it.id == id }?.let { entity ->
            LaunchAction(
                id = entity.id,
                launchPackage = entity.launchPackage,
                unfreezePackages = dao.loadDependencies(entity.id).sortedBy { it.ordering }
                    .map { it.packageName }
            )
        }
    }

    suspend fun save(
        id: String = UUID.randomUUID().toString(),
        launchPackage: String,
        unfreezePackages: List<String>
    ): LaunchAction = withContext(Dispatchers.IO) {
        val action = LaunchAction(
            id = id,
            launchPackage = launchPackage,
            unfreezePackages = unfreezePackages.distinct()
        )
        val dao = AppMetaCache.database().actionDao()
        dao.saveAction(ActionEntity(id = action.id, launchPackage = action.launchPackage), action.unfreezePackages.mapIndexed { position, packageName ->
                ActionDependencyEntity(actionId = action.id, packageName = packageName, ordering = position)
            })
        action
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        AppMetaCache.database().actionDao().delete(id)
    }

    suspend fun duplicate(action: LaunchAction): LaunchAction = save(
        launchPackage = action.launchPackage,
        unfreezePackages = action.unfreezePackages
    )
}