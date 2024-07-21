package com.aistra.hail.app

import android.content.pm.ApplicationInfo
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.utils.HPackages

class AppInfo(
    val packageName: String,
    var pinned: Boolean,
    var tagId: Int,
    var whitelisted: Boolean
) {
    val applicationInfo: ApplicationInfo? get() = HPackages.getApplicationInfoOrNull(packageName)
    val name get() = applicationInfo?.loadLabel(app.packageManager) ?: packageName

    var state: Int = STATE_NOT_FOUND
    fun getCurrentState(): Int = when {
        applicationInfo == null -> STATE_NOT_FOUND
        AppManager.isAppFrozen(packageName) -> STATE_FROZEN
        else -> STATE_UNFROZEN
    }

    var selected: Boolean = false

    override fun equals(other: Any?): Boolean = other is AppInfo && other.packageName == packageName
    override fun hashCode(): Int = packageName.hashCode()

    companion object {
        const val STATE_NOT_FOUND = 0
        const val STATE_UNFROZEN = 1
        const val STATE_FROZEN = 2
    }
}