package com.aistra.hail.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import com.aistra.hail.R
import com.aistra.hail.utils.HPolicy

class DeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        HPolicy.enableBackupService()
        HPolicy.setOrganizationName(context.getString(R.string.app_name))
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "禁用设备管理员将影响应用功能"
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
    }
}
