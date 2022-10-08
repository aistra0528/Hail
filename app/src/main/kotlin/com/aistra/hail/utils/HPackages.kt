package com.aistra.hail.utils

import android.annotation.SuppressLint
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.aistra.hail.HailApp

@Suppress("DEPRECATION")
object HPackages {
    @SuppressLint("InlinedApi")
    private const val MATCH_UNINSTALLED = PackageManager.MATCH_UNINSTALLED_PACKAGES

    fun packageUri(packageName: String) = "package:$packageName"

    fun getInstalledPackages(flags: Int = MATCH_UNINSTALLED): List<PackageInfo> =
        if (atLeastT()) HailApp.app.packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
        else HailApp.app.packageManager.getInstalledPackages(flags)

    fun getPackageInfoOrNull(packageName: String, flags: Int = MATCH_UNINSTALLED) = try {
        if (atLeastT()) HailApp.app.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
        else HailApp.app.packageManager.getPackageInfo(packageName, flags)
    } catch (t: Throwable) {
        null
    }

    fun getApplicationInfoOrNull(packageName: String, flags: Int = MATCH_UNINSTALLED) =
        getPackageInfoOrNull(packageName, flags)?.applicationInfo

    fun isAppDisabled(packageName: String): Boolean =
        getApplicationInfoOrNull(packageName)?.enabled?.not() ?: false

    fun isAppSuspended(packageName: String): Boolean = try {
        HailApp.app.packageManager.isPackageSuspended(packageName)
    } catch (t: Throwable) {
        false
    }

    fun canUninstall(packageName: String): Boolean =
        getApplicationInfoOrNull(packageName)?.sourceDir?.startsWith("/data") ?: false

    private fun atLeastT(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}