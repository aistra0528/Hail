package com.aistra.hail.app

import android.content.pm.ApplicationInfo
import com.aistra.hail.HailApp
import com.aistra.hail.utils.HPackages

class AppInfo(val packageName: String) {
    val applicationInfo: ApplicationInfo? get() = HPackages.getApplicationInfoOrNull(packageName)
    val name get() = applicationInfo?.loadLabel(HailApp.app.packageManager) ?: packageName
    val icon
        get() = applicationInfo?.loadIcon(HailApp.app.packageManager)
            ?: HailApp.app.packageManager.defaultActivityIcon

    override fun equals(other: Any?): Boolean = other is AppInfo && other.packageName == packageName
    override fun hashCode(): Int = packageName.hashCode()
}