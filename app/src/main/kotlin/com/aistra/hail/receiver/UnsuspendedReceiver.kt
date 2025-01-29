package com.aistra.hail.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aistra.hail.HailApp.Companion.app

class UnsuspendedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_PACKAGE_UNSUSPENDED_MANUALLY) runCatching {
            app.setAutoFreezeService()
            app.setAutoUnFreezeService()
        }
    }

    companion object {
        private const val ACTION_PACKAGE_UNSUSPENDED_MANUALLY =
            "android.intent.action.PACKAGE_UNSUSPENDED_MANUALLY"
    }
}