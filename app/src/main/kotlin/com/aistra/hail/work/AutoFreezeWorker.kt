package com.aistra.hail.work

import android.content.Context
import android.content.Intent
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.aistra.hail.HailApp
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.services.AutoFreezeService
import com.aistra.hail.utils.HSystem

class AutoFreezeWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        if (HSystem.isInteractive(applicationContext)
            || isSkipWhileCharging(applicationContext)) return Result.success() // Not stopping the AutoFreezeService here. The worker will run at some point. Then we'll stop the Service
        var i = 0
        var denied = false
        HailData.checkedList.forEach {
            when {
                isSkipApp(applicationContext, it) -> return@forEach
                AppManager.setAppFrozen(it.packageName, true) -> i++
                it.packageName != HailApp.app.packageName && it.applicationInfo != null ->
                    denied = true
            }
        }
        return if (denied && i == 0) {
            Result.failure()
        } else {
            applicationContext.stopService(Intent(applicationContext, AutoFreezeService::class.java))
            Result.success()
        }
    }

    private fun isSkipWhileCharging(context: Context): Boolean =
        HailData.skipWhileCharging && HSystem.isCharging(context)

    private fun isSkipApp(context: Context, appInfo: AppInfo): Boolean =
        AppManager.isAppFrozen(appInfo.packageName)
                || (HailData.skipForegroundApp && HSystem.isForegroundApp(
            context,
            appInfo.packageName
        ))
                || (HailData.skipNotifyingApp && appInfo.packageName in AutoFreezeService.instance.activeNotifications.map { it.packageName })
                || appInfo.whitelisted
}