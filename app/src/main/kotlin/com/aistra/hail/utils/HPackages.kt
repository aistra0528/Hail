package com.aistra.hail.utils

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import com.aistra.hail.HailApp

object HPackages {
    @SuppressLint("InlinedApi")
    private const val MATCH_UNINSTALLED = PackageManager.MATCH_UNINSTALLED_PACKAGES

    fun packageUri(packageName: String) = "package:$packageName"

    fun getPackageInfoOrNull(packageName: String, flags: Int = MATCH_UNINSTALLED) = try {
        HailApp.app.packageManager.getPackageInfo(packageName, flags)
    } catch (t: Throwable) {
        null
    }

    fun getApplicationInfoOrNull(packageName: String, flags: Int = MATCH_UNINSTALLED) =
        getPackageInfoOrNull(packageName, flags)?.applicationInfo

    fun isAppDisabled(packageName: String): Boolean =
        getApplicationInfoOrNull(packageName)?.enabled?.not() ?: false
}