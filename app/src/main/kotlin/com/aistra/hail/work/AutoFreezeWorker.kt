package com.aistra.hail.work

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.aistra.hail.HailApp
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.services.AutoFreezeService
import com.aistra.hail.utils.HSystem

class AutoFreezeWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        if (!HailData.autoFreezeAfterLock  // some tasks might be scheduled before disabling auto-freeze
            || HSystem.isInteractive(applicationContext)
            || isSkipWhileCharging(applicationContext)) return Result.success()
        var i = 0
        var denied = false
        HailData.checkedList.forEach {
            when {
                isSkipApp(applicationContext, it.packageName) -> return@forEach
                AppManager.setAppFrozen(it.packageName, true) -> i++
                it.packageName != HailApp.app.packageName && it.applicationInfo != null ->
                    denied = true
            }
        }
        return if (denied && i == 0) Result.failure() else Result.success()
    }

    private fun isSkipWhileCharging(context: Context): Boolean =
        HailData.skipWhileCharging && HSystem.isCharging(context)

    private fun isSkipApp(context: Context, packageName: String): Boolean =
        AppManager.isAppFrozen(packageName)
                || (HailData.skipForegroundApp && HSystem.isForegroundApp(context, packageName))
                || (HailData.skipNotifyingApp && packageName in AutoFreezeService.instance.activeNotifications.map { it.packageName })
}