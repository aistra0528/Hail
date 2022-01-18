package com.aistra.hail.work

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.aistra.hail.HailApp
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData

class AutoFreezeWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        var denied = false
        var i = 0
        HailData.checkedList.forEach {
            when {
                AppManager.isAppFrozen(it.packageName) -> return@forEach
                AppManager.setAppFrozen(it.packageName, true) -> i++
                it.packageName != HailApp.app.packageName && it.applicationInfo != null -> denied = true
            }
        }
        return if (denied && i == 0) Result.failure()
        else Result.success()
    }
}