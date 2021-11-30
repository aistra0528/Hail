package com.aistra.hail.utils

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.IBinder
import android.system.Os
import com.aistra.hail.BuildConfig
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

object HShizuku {
    @SuppressLint("PrivateApi")
    fun setAppDisabledAsUser(packageName: String, disabled: Boolean): Boolean {
        HPackages.getPackageInfoOrNull(packageName) ?: return false
        try {
            val proxy = Class.forName("android.content.pm.IPackageManager\$Stub")
                .getMethod("asInterface", IBinder::class.java)
                .invoke(
                    null,
                    ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package"))
                )
            proxy::class.java.getMethod(
                "setApplicationEnabledSetting",
                String::class.java,
                Int::class.java,
                Int::class.java,
                Int::class.java,
                String::class.java
            ).invoke(
                proxy, packageName,
                if (disabled) PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
                else PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                0, Os.getuid() / 100000, BuildConfig.APPLICATION_ID
            )
        } catch (t: Throwable) {
            HLog.e(t)
        }
        return HPackages.isAppDisabled(packageName) == disabled
    }
}