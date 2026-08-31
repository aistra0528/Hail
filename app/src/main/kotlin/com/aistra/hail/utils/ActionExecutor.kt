package com.aistra.hail.utils

import android.content.Intent
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

object ActionExecutor {
    private val mutexes = ConcurrentHashMap<String, Mutex>()

    suspend fun prepare(action: LaunchAction): Result<Intent> = withContext(Dispatchers.IO) {
        val mutex = mutexes.computeIfAbsent(action.id) { Mutex() }
        mutex.withLock {
            action.unfreezePackages.forEach { packageName ->
                AppActions.ensureUnfrozen(packageName).onFailure {
                    return@withContext Result.failure(
                        IllegalStateException(app.getString(R.string.action_unfreeze_failed, packageName))
                    )
                }
            }
            AppActions.ensureUnfrozen(action.launchPackage).onFailure {
                return@withContext Result.failure(
                    IllegalStateException(app.getString(R.string.action_unfreeze_failed, action.launchPackage))
                )
            }
            AppActions.getLaunchIntent(action.launchPackage)
        }
    }
}
