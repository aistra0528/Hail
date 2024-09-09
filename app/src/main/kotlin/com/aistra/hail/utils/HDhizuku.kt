package com.aistra.hail.utils

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import com.aistra.hail.HailApp.Companion.app
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.dhizuku.shared.DhizukuVariables
import org.lsposed.hiddenapibypass.HiddenApiBypass

object HDhizuku {
    private val dpm = app.getSystemService<DevicePolicyManager>()!!

    fun init() {
        if (Dhizuku.init(app) && Dhizuku.isPermissionGranted() && HTarget.O) setDelegatedScopes()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun setDelegatedScopes() {
        if (!Dhizuku.getDelegatedScopes().contains(DevicePolicyManager.DELEGATION_PACKAGE_ACCESS))
            Dhizuku.setDelegatedScopes(arrayOf(DevicePolicyManager.DELEGATION_PACKAGE_ACCESS))
    }

    val lockScreen: Boolean
        get() = runCatching {
            dpm.lockNow()
            true
        }.getOrDefault(false)

    fun setAppHidden(packageName: String, hidden: Boolean): Boolean = runCatching {
        HTarget.O && dpm::class.java.getMethod(
            "setApplicationHidden",
            ComponentName::class.java,
            String::class.java,
            Boolean::class.java
        ).invoke(dpm, null, packageName, hidden) as Boolean
    }.getOrDefault(false)

    fun setAppSuspended(packageName: String, suspended: Boolean): Boolean = runCatching {
        HTarget.O && (dpm::class.java.getMethod(
            "setPackagesSuspended",
            ComponentName::class.java,
            Array<String>::class.java,
            Boolean::class.java
        ).invoke(dpm, null, arrayOf(packageName), suspended) as Array<*>).isEmpty()
    }.getOrDefault(false)

    @SuppressLint("PrivateApi")
    fun uninstallApp(packageName: String): Boolean = runCatching {
        val installer = app.packageManager.packageInstaller
        if (HTarget.P) HiddenApiBypass.setHiddenApiExemptions("")
        val mPackageName = installer::class.java.getDeclaredField("mInstallerPackageName")
        mPackageName.isAccessible = true
        if (mPackageName.get(installer) != DhizukuVariables.OFFICIAL_PACKAGE_NAME) {
            mPackageName.set(installer, DhizukuVariables.OFFICIAL_PACKAGE_NAME)
            val mInstaller = installer::class.java.getDeclaredField("mInstaller")
            mInstaller.isAccessible = true
            val origin = mInstaller.get(installer)
            val proxy = Class.forName("android.content.pm.IPackageInstaller\$Stub")
                .getMethod("asInterface", IBinder::class.java)
                .invoke(
                    null,
                    Dhizuku.binderWrapper(
                        origin::class.java.getMethod("asBinder").invoke(origin) as IBinder
                    )
                )
            mInstaller.set(installer, proxy)
        }
        installer.uninstall(
            packageName,
            PendingIntent.getActivity(
                app,
                0,
                Intent(),
                PendingIntent.FLAG_IMMUTABLE
            ).intentSender
        )
        true
    }.getOrDefault(false)
}