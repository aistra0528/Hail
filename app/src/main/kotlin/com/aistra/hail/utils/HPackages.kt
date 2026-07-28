package com.aistra.hail.utils

import android.app.ActivityManager
import android.app.AppOpsManager
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Build
import android.os.UserHandle
import android.os.Process
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.app.AppInfo
import org.lsposed.hiddenapibypass.HiddenApiBypass

object HPackages {
    val myUserId get() = Process.myUserHandle().hashCode()

    private val launcherApps get() = app.getSystemService<LauncherApps>()

    @Suppress("DEPRECATION")
    fun userHandle(userId: Int): UserHandle =
        if (HTarget.N) UserHandle::class.java.getMethod("of", Int::class.java).invoke(null, userId) as UserHandle
        else Process.myUserHandle()

    fun packageUri(packageName: String) = "package:$packageName"

    @RequiresApi(Build.VERSION_CODES.N)
    fun packageUid(packageName: String, userId: Int = myUserId): Int = runCatching {
        if (userId == myUserId) {
            if (HTarget.T) app.packageManager.getPackageUid(
                packageName, PackageManager.PackageInfoFlags.of(PackageManager.MATCH_UNINSTALLED_PACKAGES.toLong())
            ) else app.packageManager.getPackageUid(packageName, PackageManager.MATCH_UNINSTALLED_PACKAGES)
        } else {
            val method = app.packageManager::class.java.getMethod(
                "getPackageUidAsUser", String::class.java, Int::class.java, Int::class.java
            )
            method.invoke(app.packageManager, packageName, PackageManager.MATCH_UNINSTALLED_PACKAGES, userId) as Int
        }
    }.getOrElse {
        getApplicationInfoOrNull(packageName, userId = userId)?.uid ?: throw it
    }

    fun getInstalledApplications(flags: Int = if (HTarget.N) PackageManager.MATCH_UNINSTALLED_PACKAGES else 8192): List<ApplicationInfo> =
        if (HTarget.T) app.packageManager.getInstalledApplications(
            PackageManager.ApplicationInfoFlags.of(flags.toLong())
        )
        else app.packageManager.getInstalledApplications(flags)

    fun getInstalledApps(flags: Int = if (HTarget.N) PackageManager.MATCH_UNINSTALLED_PACKAGES else 8192): List<AppInfo> {
        val currentApps = getInstalledApplications(flags).map { AppInfo(it.packageName, myUserId) }
        val profileApps = runCatching {
            launcherApps?.profiles.orEmpty()
                .filter { it.hashCode() != myUserId }
                .flatMap { user ->
                    launcherApps?.getActivityList(null, user).orEmpty().map {
                        AppInfo(it.applicationInfo.packageName, user.hashCode())
                    }
                }
        }.getOrElse {
            HLog.e(it)
            emptyList()
        }
        return (currentApps + profileApps).distinct()
    }

    fun getUnhiddenPackageInfoOrNull(
        packageName: String,
        flags: Int = if (HTarget.N) PackageManager.MATCH_UNINSTALLED_PACKAGES else 8192,
        userId: Int = myUserId
    ) = runCatching {
        if (userId != myUserId) app.packageManager::class.java.getMethod(
            "getPackageInfoAsUser", String::class.java, Int::class.java, Int::class.java
        ).invoke(app.packageManager, packageName, flags, userId) as android.content.pm.PackageInfo
        else if (HTarget.T) app.packageManager.getPackageInfo(
            packageName, PackageManager.PackageInfoFlags.of(flags.toLong())
        )
        else app.packageManager.getPackageInfo(packageName, flags)
    }.getOrNull()

    fun getApplicationInfoOrNull(
        packageName: String,
        flags: Int = if (HTarget.N) PackageManager.MATCH_UNINSTALLED_PACKAGES else 8192,
        userId: Int = myUserId
    ) = runCatching {
        if (userId != myUserId) app.packageManager::class.java.getMethod(
            "getApplicationInfoAsUser", String::class.java, Int::class.java, Int::class.java
        ).invoke(app.packageManager, packageName, flags, userId) as ApplicationInfo
        else if (HTarget.T) app.packageManager.getApplicationInfo(
            packageName, PackageManager.ApplicationInfoFlags.of(flags.toLong())
        )
        else app.packageManager.getApplicationInfo(packageName, flags)
    }.getOrElse {
        if (userId == myUserId) null
        else launcherApps?.getActivityList(packageName, userHandle(userId))?.firstOrNull()?.applicationInfo
    }

    fun isAppDisabled(packageName: String, userId: Int = myUserId): Boolean =
        getApplicationInfoOrNull(packageName, userId = userId)?.enabled?.not() ?: false

    fun isAppHidden(packageName: String, userId: Int = myUserId): Boolean = getApplicationInfoOrNull(packageName, userId = userId)?.let {
        (ApplicationInfo::class.java.getField("privateFlags").get(it) as Int) and 1 == 1
    } ?: false

    fun isAppStopped(packageName: String, userId: Int = myUserId): Boolean =
        getApplicationInfoOrNull(packageName, userId = userId)?.run { flags and ApplicationInfo.FLAG_STOPPED == ApplicationInfo.FLAG_STOPPED }
            ?: false

    fun isAppSuspended(packageName: String, userId: Int = myUserId): Boolean = getApplicationInfoOrNull(packageName, userId = userId)?.let {
        when {
//            This method will cause NameNotFoundException with uninstalled packages
//            HTarget.Q -> app.packageManager.isPackageSuspended(packageName)
            HTarget.N -> it.flags and ApplicationInfo.FLAG_SUSPENDED == ApplicationInfo.FLAG_SUSPENDED
            else -> false
        }
    } ?: false

    fun isAppUninstalled(packageName: String, userId: Int = myUserId): Boolean =
        getApplicationInfoOrNull(packageName, userId = userId)?.run { flags and ApplicationInfo.FLAG_INSTALLED != ApplicationInfo.FLAG_INSTALLED }
            ?: true

    fun isPrivilegedApp(packageName: String): Boolean = getApplicationInfoOrNull(packageName)?.let {
        (ApplicationInfo::class.java.getField("privateFlags").get(it) as Int) and 8 == 8
    } ?: false

    fun canUninstallNormally(packageName: String, userId: Int = myUserId): Boolean =
        getApplicationInfoOrNull(packageName, userId = userId)?.sourceDir?.startsWith("/data") ?: false

    fun forceStopApp(packageName: String, userId: Int = myUserId): Boolean = runCatching {
        app.getSystemService<ActivityManager>()!!.let {
            if (userId != myUserId && HTarget.P) HiddenApiBypass.invoke(it::class.java, it, "forceStopPackageAsUser", packageName, userId)
            else if (userId != myUserId) it::class.java.getMethod("forceStopPackageAsUser", String::class.java, Int::class.java).invoke(it, packageName, userId)
            else if (HTarget.P) HiddenApiBypass.invoke(it::class.java, it, "forceStopPackage", packageName)
            else it::class.java.getMethod("forceStopPackage", String::class.java).invoke(it, packageName)
        }
        true
    }.getOrElse {
        HLog.e(it)
        false
    }

    fun setAppDisabled(packageName: String, disabled: Boolean, userId: Int = myUserId): Boolean {
        getApplicationInfoOrNull(packageName, userId = userId) ?: return false
        if (disabled) forceStopApp(packageName, userId)
        runCatching {
            val newState = when {
                !disabled -> PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                else -> PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            if (userId == myUserId) app.packageManager.setApplicationEnabledSetting(packageName, newState, 0)
            else app.packageManager::class.java.getMethod(
                "setApplicationEnabledSetting", String::class.java, Int::class.java, Int::class.java, Int::class.java
            ).invoke(app.packageManager, packageName, newState, 0, userId)
        }.onFailure {
            HLog.e(it)
        }
        return isAppDisabled(packageName, userId) == disabled
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun setAppRestricted(packageName: String, restricted: Boolean, userId: Int = myUserId): Boolean = runCatching {
        app.getSystemService<AppOpsManager>()!!.let {
            HiddenApiBypass.invoke(
                it::class.java,
                it,
                "setMode",
                "android:run_any_in_background",
                packageUid(packageName, userId),
                packageName,
                if (restricted) AppOpsManager.MODE_IGNORED else AppOpsManager.MODE_ALLOWED
            )
        }
        true
    }.getOrElse {
        HLog.e(it)
        false
    }
}
