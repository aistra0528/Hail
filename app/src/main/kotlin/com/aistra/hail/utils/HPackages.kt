package com.aistra.hail.utils

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.aistra.hail.HailApp.Companion.app

object HPackages {
    val myUserId get() = android.os.Process.myUserHandle().hashCode()

    fun packageUri(packageName: String) = "package:$packageName"

    @SuppressLint("InlinedApi")
    fun getInstalledApplications(flags: Int = PackageManager.MATCH_UNINSTALLED_PACKAGES): List<ApplicationInfo> =
        if (HTarget.T) app.packageManager.getInstalledApplications(
            PackageManager.ApplicationInfoFlags.of(flags.toLong())
        )
        else app.packageManager.getInstalledApplications(flags)

    @SuppressLint("InlinedApi")
    fun getUnhiddenPackageInfoOrNull(
        packageName: String, flags: Int = PackageManager.MATCH_UNINSTALLED_PACKAGES
    ) = runCatching {
        if (HTarget.T) app.packageManager.getPackageInfo(
            packageName, PackageManager.PackageInfoFlags.of(flags.toLong())
        )
        else app.packageManager.getPackageInfo(packageName, flags)
    }.getOrNull()

    @SuppressLint("InlinedApi")
    fun getApplicationInfoOrNull(
        packageName: String, flags: Int = PackageManager.MATCH_UNINSTALLED_PACKAGES
    ) = runCatching {
        if (HTarget.T) app.packageManager.getApplicationInfo(
            packageName, PackageManager.ApplicationInfoFlags.of(flags.toLong())
        )
        else app.packageManager.getApplicationInfo(packageName, flags)
    }.getOrNull()

    fun isAppDisabled(packageName: String): Boolean = getApplicationInfoOrNull(packageName)?.enabled?.not() ?: false

    fun isAppHidden(packageName: String): Boolean = getApplicationInfoOrNull(packageName)?.let {
        (ApplicationInfo::class.java.getField("privateFlags").get(it) as Int) and 1 == 1
    } ?: false

    fun isAppSuspended(packageName: String): Boolean = getApplicationInfoOrNull(packageName)?.let {
        when {
//            This method will cause NameNotFoundException with uninstalled packages
//            HTarget.Q -> app.packageManager.isPackageSuspended(packageName)
            HTarget.N -> it.flags and ApplicationInfo.FLAG_SUSPENDED == ApplicationInfo.FLAG_SUSPENDED
            else -> false
        }
    } ?: false

    fun canUninstallNormally(packageName: String): Boolean =
        getApplicationInfoOrNull(packageName)?.sourceDir?.startsWith("/data") ?: false
}