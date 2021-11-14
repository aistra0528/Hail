package com.aistra.hail.app

import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import com.aistra.hail.HailApp
import com.aistra.hail.R
import com.aistra.hail.receiver.DeviceAdminReceiver
import com.aistra.hail.utils.Shell

object AppManager {
    private val app = HailApp.app
    private val dpm =
        app.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val admin = ComponentName(app, DeviceAdminReceiver::class.java)

    val isDeviceOwnerApp: Boolean
        get() = dpm.isDeviceOwnerApp(app.packageName) && dpm.isAdminActive(admin)

    fun enableBackupService() {
        if (isDeviceOwnerApp
            && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            && !dpm.isBackupServiceEnabled(admin)
        ) {
            dpm.setBackupServiceEnabled(admin, true)
            HLog.i("isBackupServiceEnabled: ${dpm.isBackupServiceEnabled(admin)}")
        }
    }

    fun isAppHiddenOrDisabled(packageName: String): Boolean =
        isAppHidden(packageName) || isAppDisabled(packageName)

    private fun getAppInfo(packageName: String): ApplicationInfo =
        app.packageManager.getPackageInfo(
            packageName,
            PackageManager.MATCH_UNINSTALLED_PACKAGES
        ).applicationInfo

    private fun isAppHidden(packageName: String): Boolean =
        isDeviceOwnerApp && dpm.isApplicationHidden(admin, packageName)

    private fun isAppDisabled(packageName: String): Boolean = !getAppInfo(packageName).enabled

    fun setAppHiddenOrDisabled(packageName: String, hiddenOrDisabled: Boolean) {
        if (packageName == app.packageName) return
        when (HData.runningMode) {
            HData.MODE_DO_HIDE -> setAppHidden(packageName, hiddenOrDisabled)
            HData.MODE_SU_DISABLE -> setAppDisabled(packageName, hiddenOrDisabled)
        }
    }

    private fun setAppHidden(packageName: String, hidden: Boolean) {
        val failed = !isDeviceOwnerApp || !dpm.setApplicationHidden(
            admin,
            packageName,
            hidden
        )
        showOperationToast(
            packageName,
            when {
                failed -> R.string.operation_failed
                hidden -> R.string.toast_do_hide
                else -> R.string.toast_do_unhide
            }
        )
    }

    private fun setAppDisabled(packageName: String, disabled: Boolean) {
        if (!disabled) Shell.execSU("pm unhide $packageName")
        val failed = !Shell.execSU("pm ${if (disabled) "disable" else "enable"} $packageName")
        showOperationToast(
            packageName,
            when {
                failed -> R.string.operation_failed
                disabled -> R.string.toast_su_disable
                else -> R.string.toast_su_enable
            }
        )
    }

    private fun showOperationToast(packageName: String, resId: Int) = Toast.makeText(
        app,
        app.getString(
            resId,
            getAppInfo(packageName).loadLabel(app.packageManager)
        ), Toast.LENGTH_SHORT
    ).show()

    fun uninstallApp(packageName: String) {
        when (HData.runningMode) {
            HData.MODE_DO_HIDE -> if (isDeviceOwnerApp) {
                app.packageManager.packageInstaller.uninstall(
                    packageName,
                    PendingIntent.getActivity(app, 0, Intent(), 0).intentSender
                )
                return
            }
            HData.MODE_SU_DISABLE -> if (Shell.execSU("pm uninstall $packageName")) return
        }
        app.startActivity(
            Intent(
                Intent.ACTION_DELETE,
                Uri.parse("package:$packageName")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun clearDeviceOwnerApp() {
        if (isDeviceOwnerApp) dpm.clearDeviceOwnerApp(app.packageName)
    }
}