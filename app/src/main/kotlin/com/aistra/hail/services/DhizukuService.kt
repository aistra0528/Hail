package com.aistra.hail.services

import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import com.aistra.hail.utils.HTarget
import com.rosan.dhizuku.shared.DhizukuVariables

class DhizukuService(private val context: Context) : IDhizukuService.Stub() {
    private val dpm = context.getSystemService<DevicePolicyManager>()!!
    override fun onCreate() {}

    override fun onDestroy() {}

    override fun lockScreen(): Boolean {
        dpm.lockNow()
        return true
    }

    override fun isAppHidden(packageName: String): Boolean =
        dpm.isApplicationHidden(DhizukuVariables.COMPONENT_NAME, packageName)

    override fun setAppHidden(packageName: String, hidden: Boolean): Boolean =
        dpm.setApplicationHidden(DhizukuVariables.COMPONENT_NAME, packageName, hidden)

    override fun setAppSuspended(packageName: String, suspended: Boolean): Boolean =
        HTarget.N && dpm.setPackagesSuspended(
            DhizukuVariables.COMPONENT_NAME,
            arrayOf(packageName),
            suspended
        ).isEmpty()

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