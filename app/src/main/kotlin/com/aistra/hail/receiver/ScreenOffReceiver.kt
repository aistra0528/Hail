package com.aistra.hail.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aistra.hail.app.HailData
import com.aistra.hail.work.HWork

class ScreenOffReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // This receiver is now always registered (see HailApp), unlike the old code where it
        // only existed while AutoFreezeService was running, which itself only started when this
        // setting was on. Gate here to preserve that behavior.
        if (intent.action == Intent.ACTION_SCREEN_OFF && HailData.autoFreezeAfterLock) {
            HWork.setAutoFreeze(true)
        }
    }
}