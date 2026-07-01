package com.aistra.hail.work

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.services.AutoFreezeService
import com.aistra.hail.utils.HSystem

class AutoFreezeWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result =
        if (run(applicationContext, inputData.getBoolean(HailData.ACTION_LOCK, true))) Result.success()
        else Result.failure()

    companion object {
        // Shared with AutoFreezeAlarmReceiver, which drives the screen-lock delay via
        // AlarmManager instead of WorkManager (see HWork.setAutoFreeze).
        fun run(context: Context, screenOff: Boolean): Boolean {
            if ((screenOff && HSystem.isInteractive(context))
                || isSkipWhileCharging(context)
            ) return true // Not stopping the AutoFreezeService here. The worker will run at some point. Then we'll stop the Service
            val checkedList = HailData.checkedList.filter { !isSkipApp(context, it) }
            val result = AppManager.setListFrozen(true, *checkedList.toTypedArray())
            return if (result == null) {
                false
            } else {
                app.setAutoFreezeService()
                true
            }
        }

        private fun isSkipWhileCharging(context: Context): Boolean =
            HailData.skipWhileCharging && HSystem.isCharging(context)

        private fun isSkipApp(context: Context, appInfo: AppInfo): Boolean =
            AppManager.isAppFrozen(appInfo.packageName) || (HailData.skipForegroundApp && HSystem.isForegroundApp(
                context, appInfo.packageName
            )) || (HailData.skipNotifyingApp && AutoFreezeService.instance.activeNotifications.any { it.packageName == appInfo.packageName }) || appInfo.whitelisted
    }
}