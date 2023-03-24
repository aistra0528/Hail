package com.aistra.hail.utils

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.aistra.hail.HailApp.Companion.app

object HPackages {
    val myUserId get() = android.os.Process.myUserHandle().hashCode()

    fun packageUri(packageName: String) = "package:$packageName"

    @Suppress("DEPRECATION")
    fun getInstalledPackages(flags: Int = PackageManager.MATCH_UNINSTALLED_PACKAGES): List<PackageInfo> =
        if (HTarget.T) app.packageManager.getInstalledPackages(
            PackageManager.PackageInfoFlags.of(
                flags.toLong()
            )
        )
        else app.packageManager.getInstalledPackages(flags)

    @Suppress("DEPRECATION")
    fun getPackageInfoOrNull(
        packageName: String, flags: Int = PackageManager.MATCH_UNINSTALLED_PACKAGES
    ) = runCatching {
        if (HTarget.T) app.packageManager.getPackageInfo(
            packageName, PackageManager.PackageInfoFlags.of(flags.toLong())
        )
        else app.packageManager.getPackageInfo(packageName, flags)
    }.getOrNull()

    fun getApplicationInfoOrNull(
        packageName: String, flags: Int = PackageManager.MATCH_UNINSTALLED_PACKAGES
    ) = getPackageInfoOrNull(packageName, flags)?.applicationInfo

    fun isAppDisabled(packageName: String): Boolean =
        getApplicationInfoOrNull(packageName)?.enabled?.not() ?: false

    fun isAppSuspended(packageName: String): Boolean = getPackageInfoOrNull(packageName)?.let {
        val pm = app.packageManager
        when {
            HTarget.Q -> pm.isPackageSuspended(packageName)
            HTarget.N -> it.applicationInfo.flags and ApplicationInfo.FLAG_SUSPENDED == ApplicationInfo.FLAG_SUSPENDED
            else -> false
        }
    } ?: false

    fun canUninstallNormally(packageName: String): Boolean =
        getApplicationInfoOrNull(packageName)?.sourceDir?.startsWith("/data") ?: false
}