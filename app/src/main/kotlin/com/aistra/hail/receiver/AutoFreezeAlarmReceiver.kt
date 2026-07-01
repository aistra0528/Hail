package com.aistra.hail.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aistra.hail.work.AutoFreezeWorker
import kotlin.concurrent.thread

// Fired by AlarmManager.setAndAllowWhileIdle from HWork.setAutoFreeze, which survives Doze/App
// Standby deferral in a way a delayed WorkManager job does not.
class AutoFreezeAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        thread {
            try {
                AutoFreezeWorker.run(context, screenOff = true)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
