package com.aistra.hail

import android.app.Application
import android.content.Intent
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.DirtyDataUpdater
import com.aistra.hail.app.HailData
import com.aistra.hail.services.AutoFreezeService
import com.aistra.hail.utils.HTarget
import com.google.android.material.color.DynamicColors

class HailApp : Application() {
    override fun onCreate() {
        super.onCreate()
        app = this
        DynamicColors.applyToActivitiesIfAvailable(this)
        DirtyDataUpdater.update(this)
    }

    fun setAutoFreezeService() {
        if (HailData.autoFreezeAfterLock.not()) return
        val hasUnfrozen =
            HailData.checkedList.any { !AppManager.isAppFrozen(it.packageName) && !it.whitelisted }
        val intent = Intent(app, AutoFreezeService::class.java)
        when {
            !hasUnfrozen -> app.stopService(intent)
            HTarget.O -> app.startForegroundService(intent)
            else -> app.startService(intent)
        }
    }

    companion object {
        lateinit var app: HailApp private set
    }
}