package com.aistra.hail.app

import android.content.pm.ApplicationInfo
import com.aistra.hail.utils.AppMetaCache
import com.aistra.hail.utils.HPackages

class AppInfo(
    val packageName: String,
    var pinned: Boolean = false,
    var whitelisted: Boolean = false,
    val tagIdList: MutableList<Int> = mutableListOf(0)
) {
    enum class State { NOT_FOUND, UNFROZEN, FROZEN }

    val applicationInfo: ApplicationInfo? get() = HPackages.getApplicationInfoOrNull(packageName)
    val name get() = AppMetaCache.get(packageName)?.name ?: packageName
    val isInstalled get() = AppMetaCache.get(packageName)?.installed ?: (applicationInfo != null)
    val state get() = AppMetaCache.get(packageName)?.state ?: when {
        applicationInfo == null -> State.NOT_FOUND
        else -> State.UNFROZEN
    }

    override fun equals(other: Any?): Boolean = other is AppInfo && other.packageName == packageName
    override fun hashCode(): Int = packageName.hashCode()
}