package com.aistra.hail.work

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailApi
import com.aistra.hail.app.HailData


class AutoUnFreezeWorker (context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val checkedList = HailData.checkedList.any()
        return if (checkedList) {
            AppManager.setListFrozen(false)
            app.setAutoUnFreezeService()
            Log.d("TAG","AutoUnFreezeWorker pass")
            Result.success()
        } else {
            Log.d("TAG","AutoUnFreezeWorker fail")
            Result.failure()
        }
    }
}