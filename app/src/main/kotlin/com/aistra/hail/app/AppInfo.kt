package com.aistra.hail.app

import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.aistra.hail.HailApp
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HShortcuts

class AppInfo(val packageName: String, var pinned: Boolean, var tagId: Int) {
    val applicationInfo: ApplicationInfo? get() = HPackages.getApplicationInfoOrNull(packageName)
    val name get() = applicationInfo?.loadLabel(HailApp.app.packageManager) ?: packageName
    val icon: Bitmap
        get() = applicationInfo?.let { HailApp.iconLoader.loadIcon(it) }
            ?: HShortcuts.getBitmapFromDrawable(HailApp.app.packageManager.defaultActivityIcon)

    var state: Int = getCurrentState()
    fun getCurrentState(): Int = when {
        applicationInfo == null -> STATE_UNKNOWN
        AppManager.isAppFrozen(packageName) -> STATE_FROZEN
        else -> STATE_UNFROZEN
    }

    var selected: Boolean = false
    fun isNowSelected(selectedList: List<AppInfo>): Boolean = this in selectedList

    override fun equals(other: Any?): Boolean = other is AppInfo && other.packageName == packageName
    override fun hashCode(): Int = packageName.hashCode()

    companion object {
        const val STATE_UNKNOWN = 0
        const val STATE_UNFROZEN = 1
        const val STATE_FROZEN = 2
    }
}