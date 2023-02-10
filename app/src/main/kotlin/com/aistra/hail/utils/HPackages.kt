package com.aistra.hail.utils

import android.annotation.SuppressLint
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.aistra.hail.HailApp

@Suppress("DEPRECATION")
object HPackages {
    @SuppressLint("InlinedApi")
    private const val MATCH_UNINSTALLED = PackageManager.MATCH_UNINSTALLED_PACKAGES

    val myUserId get() = android.os.Process.myUserHandle().hashCode()

    fun packageUri(packageName: String) = "package:$packageName"

    fun getInstalledPackages(flags: Int = MATCH_UNINSTALLED): List<PackageInfo> =
        if (HTarget.T) HailApp.app.packageManager.getInstalledPackages(
            PackageManager.PackageInfoFlags.of(
                flags.toLong()
            )
        )
        else HailApp.app.packageManager.getInstalledPackages(flags)

    fun getPackageInfoOrNull(packageName: String, flags: Int = MATCH_UNINSTALLED) = try {
        if (HTarget.T) HailApp.app.packageManager.getPackageInfo(
            packageName, PackageManager.PackageInfoFlags.of(flags.toLong())
        )
        else HailApp.app.packageManager.getPackageInfo(packageName, flags)
    } catch (t: Throwable) {
        null
    }

    fun getApplicationInfoOrNull(packageName: String, flags: Int = MATCH_UNINSTALLED) =
        getPackageInfoOrNull(packageName, flags)?.applicationInfo

    fun isAppDisabled(packageName: String): Boolean =
        getApplicationInfoOrNull(packageName)?.enabled?.not() ?: false

    fun isAppSuspended(packageName: String): Boolean = getPackageInfoOrNull(packageName)?.let {
        val pm = HailApp.app.packageManager
        when {
            HTarget.Q -> pm.isPackageSuspended(packageName)
            HTarget.N -> pm::class.java.getMethod(
                "isPackageSuspendedForUser", String::class.java, Int::class.java
            ).invoke(pm, packageName, myUserId) as Boolean
            else -> false
        }
    } ?: false

    fun canUninstallNormally(packageName: String): Boolean =
        getApplicationInfoOrNull(packageName)?.sourceDir?.startsWith("/data") ?: false
}