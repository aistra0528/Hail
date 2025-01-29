package com.aistra.hail.work
import android.content.Context
import android.content.Intent
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.app.HailApi
import com.aistra.hail.app.HailData
class AutoUnFreezeWorker (context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val checkedList = HailData.checkedList.any()
        return if (checkedList) {
            applicationContext.startActivity(Intent(HailApi.ACTION_UNFREEZE_ALL).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            app.setAutoUnFreezeService()
            Result.success()
        } else {
            Result.failure()
        }
    }
}