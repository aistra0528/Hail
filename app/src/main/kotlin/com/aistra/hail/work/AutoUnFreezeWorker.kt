package com.aistra.hail.work
import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
class AutoUnFreezeWorker (context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val checkedList = HailData.checkedList;
        val result = AppManager.setListFrozen(false, *checkedList.toTypedArray())
        return if (result == null) {
            Result.failure()
        } else {
            app.setAutoUnFreezeService()
            Result.success()
        }
    }
}