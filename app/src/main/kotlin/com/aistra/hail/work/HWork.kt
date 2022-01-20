package com.aistra.hail.work

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.aistra.hail.HailApp
import com.aistra.hail.app.HailData
import java.util.concurrent.TimeUnit

object HWork {
    fun cancelWork(packageName: String) =
        WorkManager.getInstance(HailApp.app).cancelUniqueWork(packageName)

    fun setDeferredFrozen(packageName: String, frozen: Boolean = true, minutes: Long) {
        WorkManager.getInstance(HailApp.app).enqueueUniqueWork(
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

    fun setAutoFreeze() {
        WorkManager.getInstance(HailApp.app).enqueueUniqueWork(
            "AutoFreeze",
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<AutoFreezeWorker>()
                .setInitialDelay(5, TimeUnit.SECONDS).build()
        )
    }
}