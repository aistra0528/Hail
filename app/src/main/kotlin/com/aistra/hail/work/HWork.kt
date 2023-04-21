package com.aistra.hail.work

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.app.HailApi
import com.aistra.hail.app.HailData
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

    fun setAutoFreeze(screenOff: Boolean) {
        WorkManager.getInstance(app).enqueueUniqueWork(
            HailApi.ACTION_FREEZE_ALL,
            ExistingWorkPolicy.REPLACE,  // in case the old task has not been executed...
            OneTimeWorkRequestBuilder<AutoFreezeWorker>().run {
                if (screenOff) setInitialDelay(HailData.autoFreezeDelay, TimeUnit.MINUTES)
                setInputData(workDataOf(HailData.ACTION_LOCK to screenOff))
                build()
            }
        )
    }
}