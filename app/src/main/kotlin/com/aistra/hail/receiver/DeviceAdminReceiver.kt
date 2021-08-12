package com.aistra.hail.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import com.aistra.hail.app.AppManager

class DeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        AppManager.enableBackupService()
    }
}