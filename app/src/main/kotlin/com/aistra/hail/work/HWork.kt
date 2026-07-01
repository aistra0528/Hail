package com.aistra.hail.work

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.SystemClock
import androidx.core.content.getSystemService
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.app.HailApi
import com.aistra.hail.app.HailData
import com.aistra.hail.receiver.AutoFreezeAlarmReceiver
import java.util.concurrent.TimeUnit

object HWork {
    fun cancelWork(name: String) =
        WorkManager.getInstance(app).cancelUniqueWork(name)

    fun setDeferredFrozen(packageName: String, frozen: Boolean = true, minutes: Long) {
        WorkManager.getInstance(app).enqueueUniqueWork(
            packageName,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<FrozenWorker>().setInputData(
                workDataOf(
                    HailData.KEY_PACKAGE to packageName,
                    HailData.KEY_FROZEN to frozen
                )
            ).setInitialDelay(minutes, TimeUnit.MINUTES).build()
        )
    }

    // The screen-off (delayed) case uses AlarmManager.setAndAllowWhileIdle rather than
    // WorkManager: a delayed WorkManager job can be deferred well past its due time once the
    // device settles into Doze/App Standby with the screen off, which is exactly when this
    // fires. The immediate (manual, screen-on) case keeps using WorkManager as before.
    //
    // A zero-minute delay is handled separately (below) rather than as a 0ms AlarmManager
    // trigger: the platform enforces a minimum ~5s futurity on every scheduled alarm, including
    // setAndAllowWhileIdle. Measured on-device, that floor delays a "0" alarm by 5000-5014ms
    // regardless of the requested trigger time. Since AutoFreezeWorker skips freezing when the
    // screen is interactive again by the time it runs, that forced 5s wait raced against a quick
    // unlock and silently no-op'd the "instant" case entirely. There's nothing to wait for here
    // anyway -- the screen is confirmed off at the moment this is called -- so fire the same
    // receiver AlarmManager would have, directly.
    fun setAutoFreeze(screenOff: Boolean) {
        val alarmManager = app.getSystemService<AlarmManager>()!!
        val pendingIntent = autoFreezeAlarmIntent()
        if (screenOff) {
            alarmManager.cancel(pendingIntent)
            if (HailData.autoFreezeDelay <= 0L) {
                app.sendBroadcast(Intent(app, AutoFreezeAlarmReceiver::class.java))
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + TimeUnit.MINUTES.toMillis(HailData.autoFreezeDelay),
                    pendingIntent
                )
            }
        } else {
            alarmManager.cancel(pendingIntent)
            WorkManager.getInstance(app).enqueueUniqueWork(
                HailApi.ACTION_FREEZE_ALL,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<AutoFreezeWorker>().setInputData(
                    workDataOf(HailData.ACTION_LOCK to false)
                ).build()
            )
        }
    }

    // Called when the "after screen locked" setting is turned off, so a delay already scheduled
    // from a prior screen-off doesn't still fire after the user opted out.
    fun cancelAutoFreeze() {
        app.getSystemService<AlarmManager>()!!.cancel(autoFreezeAlarmIntent())
    }

    private fun autoFreezeAlarmIntent(): PendingIntent = PendingIntent.getBroadcast(
        app, 0, Intent(app, AutoFreezeAlarmReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}