package com.aistra.hail.app

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import com.aistra.hail.HailApp
import com.aistra.hail.R
import com.aistra.hail.receiver.DeviceAdminReceiver

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

    fun isAppHiddenOrDisabled(packageName: String): Boolean {
        return when (HData.runningMode) {
            HData.MODE_DPM_HIDE -> isAppHidden(packageName)
            HData.MODE_ROOT_DISABLE -> isAppDisabled(packageName)
            else -> false
        }
    }

    private fun getPackageInfo(packageName: String): PackageInfo =
        app.packageManager.getPackageInfo(
            packageName,
            PackageManager.MATCH_UNINSTALLED_PACKAGES
        )

    private fun isAppHidden(packageName: String): Boolean =
        isDeviceOwnerApp && dpm.isApplicationHidden(admin, packageName)

    private fun isAppDisabled(packageName: String): Boolean =
        !getPackageInfo(packageName).applicationInfo.enabled

    fun setAppHiddenOrDisabled(packageName: String, hiddenOrDisabled: Boolean) {
        if (packageName == app.packageName) return
        when (HData.runningMode) {
            HData.MODE_DPM_HIDE -> setAppHidden(packageName, hiddenOrDisabled)
            HData.MODE_ROOT_DISABLE -> setAppDisabled(packageName, hiddenOrDisabled)
        }
    }

    @SuppressLint("InlinedApi")
    private fun setAppHidden(packageName: String, hidden: Boolean) {
        Toast.makeText(
            app,
            if (isDeviceOwnerApp && dpm.setApplicationHidden(
                    admin,
                    packageName,
                    hidden
                )
            ) {
                app.getString(
                    if (hidden) R.string.toast_dpm_hide else R.string.toast_dpm_unhide,
                    getPackageInfo(packageName).applicationInfo.loadLabel(app.packageManager)
                )
            } else {
                app.getString(R.string.operation_failed)
            }, Toast.LENGTH_SHORT
        ).show()
    }

    private fun setAppDisabled(packageName: String, disabled: Boolean) {
        var i = -1
        try {
            i = Runtime.getRuntime()
                .exec(
                    arrayOf(
                        "su",
                        "-c",
                        "pm ${if (disabled) "disable" else "enable"} $packageName"
                    )
                ).waitFor()
        } catch (e: Exception) {
        }
        Toast.makeText(
            app,
            if (i == 0) {
                app.getString(
                    if (disabled) R.string.toast_root_disable else R.string.toast_root_enable,
                    getPackageInfo(packageName).applicationInfo.loadLabel(app.packageManager)
                )
            } else app.getString(R.string.operation_failed), Toast.LENGTH_SHORT
        ).show()
    }

    fun clearDeviceOwnerApp() {
        if (isDeviceOwnerApp)
            dpm.clearDeviceOwnerApp(app.packageName)
    }
}