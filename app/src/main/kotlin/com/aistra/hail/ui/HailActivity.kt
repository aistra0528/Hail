package com.aistra.hail.ui

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.services.AutoFreezeService

open class HailActivity : AppCompatActivity() {
    fun setAutoFreezeService(hasUnfrozen: Boolean? = null) {
        if (HailData.autoFreezeAfterLock.not()) return
        var start = hasUnfrozen ?: false
        if (hasUnfrozen == null)
            start = HailData.checkedList.any { !AppManager.isAppFrozen(it.packageName) }
        applicationContext.let {
            val intent = Intent(it, AutoFreezeService::class.java)
            when {
                !start -> it.stopService(intent)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> it.startForegroundService(intent)
                else -> it.startService(intent)
            }
        }
    }
}