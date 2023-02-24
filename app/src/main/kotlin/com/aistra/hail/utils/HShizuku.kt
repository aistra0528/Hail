package com.aistra.hail.utils

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.InputEvent
import android.view.KeyEvent
import com.aistra.hail.BuildConfig
import com.aistra.hail.utils.HPackages.myUserId
import moe.shizuku.server.IShizukuService
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

object HShizuku {
    private val isRoot get() = Shizuku.getUid() == 0
    private val userId get() = if (isRoot) myUserId else 0
    private val callerPackage get() = if (isRoot) BuildConfig.APPLICATION_ID else "com.android.shell"

    private fun asInterface(className: String, serviceName: String): Any =
        ShizukuBinderWrapper(SystemServiceHelper.getSystemService(serviceName)).let {
            Class.forName("$className\$Stub").run {
                if (HTarget.P) HiddenApiBypass.invoke(this, null, "asInterface", it)
                else getMethod("asInterface", IBinder::class.java).invoke(null, it)
            }
        }

    val lockScreen
        get() = try {
            val input = asInterface("android.hardware.input.IInputManager", "input")
            val inject = input::class.java.getMethod(
                "injectInputEvent", InputEvent::class.java, Int::class.java
            )
            val now = SystemClock.uptimeMillis()
            inject.invoke(
                input, KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_POWER, 0), 0
            )
            inject.invoke(
                input, KeyEvent(now, now, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_POWER, 0), 0
            )
            true
        } catch (t: Throwable) {
            HLog.e(t)
            false
        }

    private fun forceStopApp(packageName: String) = try {
        asInterface("android.app.IActivityManager", "activity").let {
            if (HTarget.P) HiddenApiBypass.invoke(
                it::class.java, it, "forceStopPackage", packageName, userId
            ) else it::class.java.getMethod(
                "forceStopPackage", String::class.java, Int::class.java
            ).invoke(
                it, packageName, userId
            )
        }
        true
    } catch (t: Throwable) {
        HLog.e(t)
        false
    }

    fun setAppDisabled(packageName: String, disabled: Boolean): Boolean {
        HPackages.getPackageInfoOrNull(packageName) ?: return false
        if (disabled) forceStopApp(packageName)
        try {
            val pm = asInterface("android.content.pm.IPackageManager", "package")
            val newState = when {
                !disabled -> PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                isRoot -> PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                else -> PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
            }
            pm::class.java.getMethod(
                "setApplicationEnabledSetting",
                String::class.java,
                Int::class.java,
                Int::class.java,
                Int::class.java,
                String::class.java
            ).invoke(pm, packageName, newState, 0, myUserId, BuildConfig.APPLICATION_ID)
        } catch (t: Throwable) {
            HLog.e(t)
        }
        return HPackages.isAppDisabled(packageName) == disabled
    }

    fun isAppHidden(packageName: String): Boolean {
        HPackages.getPackageInfoOrNull(packageName) ?: return false
        return try {
            val pm = asInterface("android.content.pm.IPackageManager", "package")
            (if (HTarget.P) HiddenApiBypass.invoke(
                pm::class.java, pm, "getApplicationHiddenSettingAsUser", packageName, userId
            ) else pm::class.java.getMethod(
                "getApplicationHiddenSettingAsUser", String::class.java, Int::class.java
            ).invoke(pm, packageName, userId)) as Boolean
        } catch (t: Throwable) {
            HLog.e(t)
            false
        }
    }

    fun setAppHidden(packageName: String, hidden: Boolean): Boolean {
        HPackages.getPackageInfoOrNull(packageName) ?: return false
        if (hidden) forceStopApp(packageName)
        return try {
            val pm = asInterface("android.content.pm.IPackageManager", "package")
            pm::class.java.getMethod(
                "setApplicationHiddenSettingAsUser",
                String::class.java,
                Boolean::class.java,
                Int::class.java
            ).invoke(pm, packageName, hidden, userId) as Boolean
        } catch (t: Throwable) {
            HLog.e(t)
            false
        }
    }

    fun setAppSuspended(packageName: String, suspended: Boolean): Boolean {
        HPackages.getPackageInfoOrNull(packageName) ?: return false
        if (suspended) forceStopApp(packageName)
        return try {
            val pm = asInterface("android.content.pm.IPackageManager", "package")
            (when {
                HTarget.Q -> HiddenApiBypass.invoke(
                    pm::class.java,
                    pm,
                    "setPackagesSuspendedAsUser",
                    arrayOf(packageName),
                    suspended,
                    null,
                    null,
                    if (suspended) suspendDialogInfo else null,
                    callerPackage,
                    userId
                )
                HTarget.P -> HiddenApiBypass.invoke(
                    pm::class.java,
                    pm,
                    "setPackagesSuspendedAsUser",
                    arrayOf(packageName),
                    suspended,
                    null,
                    null,
                    null /*dialogMessage*/,
                    callerPackage,
                    userId
                )
                HTarget.N -> pm::class.java.getMethod(
                    "setPackagesSuspendedAsUser",
                    Array<String>::class.java,
                    Boolean::class.java,
                    Int::class.java
                ).invoke(pm, arrayOf(packageName), suspended, userId)
                else -> return false
            } as Array<*>).isEmpty()
        } catch (t: Throwable) {
            HLog.e(t)
            false
        }
    }

    private val suspendDialogInfo: Any
        @SuppressLint("PrivateApi") get() = HiddenApiBypass.newInstance(Class.forName("android.content.pm.SuspendDialogInfo\$Builder"))
            .let {
                HiddenApiBypass.invoke(
                    it::class.java, it, "setNeutralButtonAction", 1 /*BUTTON_ACTION_UNSUSPEND*/
                )
                HiddenApiBypass.invoke(it::class.java, it, "build")
            }

    fun uninstallApp(packageName: String): Boolean {
        HPackages.getPackageInfoOrNull(packageName) ?: return true
        return try {
            IShizukuService.Stub.asInterface(Shizuku.getBinder())
                .newProcess(arrayOf(if (isRoot) "su" else "sh"), null, null).run {
                    ParcelFileDescriptor.AutoCloseOutputStream(outputStream).use {
                        val uninstall =
                            if (isRoot || HPackages.canUninstallNormally(packageName)) "uninstall"
                            else "uninstall --user current"
                        it.write("pm $uninstall $packageName".toByteArray())
                    }
                    (waitFor() == 0).also {
                        destroy()
                    }
                }
        } catch (t: Throwable) {
            HLog.e(t)
            false
        }
    }
}