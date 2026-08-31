package com.aistra.hail.utils

import android.content.ActivityNotFoundException
import android.content.Intent
import com.aistra.hail.HailApp
import com.aistra.hail.R
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppActions {
    internal fun app(): HailApp = HailApp.app

    suspend fun ensureUnfrozen(packageName: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (HPackages.getApplicationInfoOrNull(packageName) == null) {
            return@withContext Result.failure(
                IllegalArgumentException(app().getString(R.string.action_app_unavailable, packageName))
            )
        }
        if (AppManager.isAppFrozen(packageName) && !AppManager.setAppFrozen(packageName, false)) {
            return@withContext Result.failure(
                IllegalStateException(app().getString(R.string.action_unfreeze_failed, packageName))
            )
        }
        if (AppManager.isAppFrozen(packageName)) {
            return@withContext Result.failure(
                IllegalStateException(app().getString(R.string.action_unfreeze_failed, packageName))
            )
        }
        Result.success(Unit)
    }

    suspend fun getLaunchIntent(packageName: String): Result<Intent> = withContext(Dispatchers.IO) {
        if (HailData.workingMode == HailData.MODE_ISLAND_HIDE) {
            HIsland.ensureLaunchIntentExists(packageName)
        }
        val launchIntent = app().packageManager.getLaunchIntentForPackage(packageName)
            ?: return@withContext Result.failure(
                ActivityNotFoundException(app().getString(R.string.activity_not_found))
            )
        app().setAutoFreezeService()
        Result.success(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    suspend fun freezePackages(frozen: Boolean, packages: List<String>): Result<Unit> = withContext(Dispatchers.IO) {
        packages.forEach { packageName ->
            if (HPackages.getApplicationInfoOrNull(packageName) == null) {
                return@withContext Result.failure(
                    IllegalArgumentException(app().getString(R.string.action_app_unavailable, packageName))
                )
            }
            if (!AppManager.setAppFrozen(packageName, frozen)) {
                return@withContext Result.failure(
                    IllegalStateException(app().getString(R.string.action_freeze_failed, packageName))
                )
            }
        }
        Result.success(Unit)
    }
}
