package com.aistra.hail.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aistra.hail.work.HWork
import com.aistra.hail.app.HailData

class ScreenOffReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_SCREEN_OFF) {
            HWork.setAutoFreeze(true)
            if (HailData.biometricLogin) HailData.needsVerify = true
        }
    }
}
