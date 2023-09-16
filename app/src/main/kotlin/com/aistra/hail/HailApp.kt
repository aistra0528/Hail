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

class HailApp : Application() {
    override fun onCreate() {
        super.onCreate()
        app = this
        // DynamicColors.applyToActivitiesIfAvailable(app)
        // DirtyDataUpdater.update(app)
        if (HailData.workingMode.startsWith(HailData.DHIZUKU)) HDhizuku.init()
    }

    fun setAutoFreezeService(enabled: Boolean? = null) {
        if (HailData.autoFreezeAfterLock.not()) return
        val start = enabled ?: HailData.checkedList.any {
            it.packageName != packageName
                    && it.applicationInfo != null
                    && !AppManager.isAppFrozen(it.packageName)
                    && !it.whitelisted
        }
        val intent = Intent(app, AutoFreezeService::class.java)
        if (start) {
            setAutoFreezeServiceEnabled(true)
            ContextCompat.startForegroundService(app, intent)
        } else {
            stopService(intent)
            setAutoFreezeServiceEnabled(false)
        }
    }

    fun stopAutoFreezeService() {
        stopService(Intent(app, AutoFreezeService::class.java))
        setAutoFreezeServiceEnabled(false)
    }

    fun setAutoFreezeServiceEnabled(enabled: Boolean) {
        packageManager.setComponentEnabledSetting(
            ComponentName(app, AutoFreezeService::class.java),
            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    companion object {
        lateinit var app: HailApp private set
    }
}