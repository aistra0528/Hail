package com.aistra.hail.services

import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import com.aistra.hail.utils.HTarget

class DhizukuService(private val context: Context) : IDhizukuService.Stub() {
    private val dpm = context.getSystemService<DevicePolicyManager>()!!
    private lateinit var admin: ComponentName
    override fun onCreate() {
        admin = dpm.activeAdmins?.find { dpm.isDeviceOwnerApp(it.packageName) }
            ?: throw IllegalStateException("dhizuku is not activated")
    }

    override fun onDestroy() {}

    override fun lockScreen(): Boolean {
        dpm.lockNow()
        return true
    }

    override fun isAppHidden(packageName: String): Boolean =
        dpm.isApplicationHidden(admin, packageName)

    override fun setAppHidden(packageName: String, hidden: Boolean): Boolean =
        dpm.setApplicationHidden(admin, packageName, hidden)

    override fun setAppSuspended(packageName: String, suspended: Boolean): Boolean =
        HTarget.N && dpm.setPackagesSuspended(admin, arrayOf(packageName), suspended).isEmpty()

    override fun uninstallApp(packageName: String): Boolean {
        context.packageManager.packageInstaller.uninstall(
            packageName,
            PendingIntent.getActivity(
                context,
                0,
                Intent(),
                PendingIntent.FLAG_IMMUTABLE
            ).intentSender
        )
        return true
    }
}