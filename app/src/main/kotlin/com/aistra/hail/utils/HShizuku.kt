package com.aistra.hail.utils

import android.content.pm.PackageManager
import android.os.IBinder
import android.os.SystemClock
import android.system.Os
import android.view.InputEvent
import android.view.KeyEvent
import com.aistra.hail.BuildConfig
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

object HShizuku {
    private fun asInterface(className: String, serviceName: String): Any? =
        Class.forName("$className\$Stub")
            .getMethod("asInterface", IBinder::class.java)
            .invoke(null, ShizukuBinderWrapper(SystemServiceHelper.getSystemService(serviceName)))

    val lockScreen
        get() = try {
            val proxy = asInterface("android.hardware.input.IInputManager", "input")!!
            val inject = proxy::class.java.getMethod(
                "injectInputEvent",
                InputEvent::class.java,
                Int::class.java
            )
            val now = SystemClock.uptimeMillis()
            inject.invoke(
                proxy,
                KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_POWER, 0),
                0
            )
            inject.invoke(
                proxy,
                KeyEvent(now, now, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_POWER, 0),
                0
            )
            true
        } catch (t: Throwable) {
            HLog.e(t)
            false
        }

    fun setAppDisabledAsUser(packageName: String, disabled: Boolean): Boolean {
        HPackages.getPackageInfoOrNull(packageName) ?: return false
        try {
            val proxy = asInterface("android.content.pm.IPackageManager", "package")!!
            proxy::class.java.getMethod(
                "setApplicationEnabledSetting",
                String::class.java,
                Int::class.java,
                Int::class.java,
                Int::class.java,
                String::class.java
            ).invoke(
                proxy,
                packageName,
                if (disabled) PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
                else PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                0,
                Os.getuid() / 100000,
                BuildConfig.APPLICATION_ID
            )
        } catch (t: Throwable) {
            HLog.e(t)
        }
        return HPackages.isAppDisabled(packageName) == disabled
    }

    fun setAppSuspendedAsUser(packageName: String, suspended: Boolean): Boolean {
        HPackages.getPackageInfoOrNull(packageName) ?: return false
        return try {
            val proxy = asInterface("android.content.pm.IPackageManager", "package")!!
            (HiddenApiBypass.invoke(
                proxy::class.java,
                proxy,
                "setPackagesSuspendedAsUser",
                arrayOf(packageName),
                suspended,
                null,
                null,
                null,
                if (Shizuku.getUid() == 0) BuildConfig.APPLICATION_ID else "com.android.shell",
                Os.getuid() / 100000
            ) as Array<*>).isEmpty()
        } catch (t: Throwable) {
            HLog.e(t)
            false
        }
    }
}