package com.aistra.hail.work

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData

class FrozenWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        inputData.getString(HailData.KEY_PACKAGE)?.let {
            AppManager.setAppFrozen(
                it,
                inputData.getBoolean(HailData.KEY_FROZEN, true),
                inputData.getInt(HailData.KEY_USER, com.aistra.hail.utils.HPackages.myUserId)
            )
            return Result.success()
        }
        return Result.failure()
    }
}
