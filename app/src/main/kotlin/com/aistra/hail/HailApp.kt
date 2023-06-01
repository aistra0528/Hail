package com.aistra.hail

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.services.AutoFreezeService
import com.aistra.hail.utils.HDhizuku
import com.google.android.material.color.DynamicColors

class HailApp : Application() {
    override fun onCreate() {
        super.onCreate()
        app = this
        DynamicColors.applyToActivitiesIfAvailable(app)
        // DirtyDataUpdater.update(app)
        if (HailData.workingMode.startsWith(HailData.DHIZUKU)) HDhizuku.init()
    }

    fun setAutoFreezeService(enabled: Boolean? = null) {
        if (HailData.autoFreezeAfterLock.not()) return
        val start = enabled
            ?: HailData.checkedList.any { !AppManager.isAppFrozen(it.packageName) && !it.whitelisted }
        val intent = Intent(app, AutoFreezeService::class.java)
        val name = ComponentName(app, AutoFreezeService::class.java)
        if (start) {
            packageManager.setComponentEnabledSetting(
                name, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP
            )
            ContextCompat.startForegroundService(app, intent)
        } else {
            stopService(intent)
            packageManager.setComponentEnabledSetting(
                name, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP
            )
        }
    }

    companion object {
        lateinit var app: HailApp private set
    }
}