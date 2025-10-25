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
    
  // 转移完成后重新启用必要的服务
        HPolicy.enableBackupService()
        HPolicy.setOrganizationName(context.getString(R.string.app_name))
        
        // 可以发送广播通知应用其他组件
        val transferComplete = Intent("com.aistra.hail.action.OWNERSHIP_TRANSFER_COMPLETE")
        context.sendBroadcast(transferComplete)
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return context.getString(R.string.msg_disable_admin)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.d("Hail", "Device admin disabled")
    }
}