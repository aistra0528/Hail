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
    override fun doWork(): Result {
        if ((inputData.getBoolean(HailData.ACTION_LOCK, true)
                    && HSystem.isInteractive(applicationContext))
            || isSkipWhileCharging(applicationContext)
            || HailData.autoFreezeDisabledUntil > System.currentTimeMillis()
        ) return Result.success() // Not stopping the AutoFreezeService here. The worker will run at some point. Then we'll stop the Service
        val checkedList = HailData.checkedList.filter { !isSkipApp(applicationContext, it) }
        val result = AppManager.setListFrozen(true, *checkedList.toTypedArray())
        return if (result == null) {
            Result.failure()
        } else {
            app.setAutoFreezeService()
            Result.success()
        }
    }

    private fun isSkipWhileCharging(context: Context): Boolean =
        HailData.skipWhileCharging && HSystem.isCharging(context)

    private fun isSkipApp(context: Context, appInfo: AppInfo): Boolean =
        AppManager.isAppFrozen(appInfo.packageName) || (HailData.skipForegroundApp && HSystem.isForegroundApp(
            context, appInfo.packageName
        )) || (HailData.skipNotifyingApp && AutoFreezeService.instance.activeNotifications.any { it.packageName == appInfo.packageName }) || appInfo.whitelisted

    class RestartAutoFreezeServiceWorker(private val context: Context, params: WorkerParameters) : Worker(context, params) {
        override fun doWork(): Result {
            app.setAutoFreezeService()
            if (!HSystem.isInteractive(context)) { // Only run freeze directly if screen is off
                HWork.setAutoFreeze(false) // TODO is false correct here? This was added after I implemented it and I'm not sure right now...
            }
            return Result.success()
        }
    }
}