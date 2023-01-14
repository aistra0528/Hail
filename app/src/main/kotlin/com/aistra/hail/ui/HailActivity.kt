package com.aistra.hail.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.services.AutoFreezeService
import com.aistra.hail.utils.HTarget

open class HailActivity : AppCompatActivity() {
    fun setAutoFreezeService() {
        if (HailData.autoFreezeAfterLock.not()) return
        val hasUnfrozen =
            HailData.checkedList.any { !AppManager.isAppFrozen(it.packageName) && !it.whitelisted }
        applicationContext.let {
            val intent = Intent(it, AutoFreezeService::class.java)
            when {
                !hasUnfrozen -> it.stopService(intent)
                HTarget.O -> it.startForegroundService(intent)
                else -> it.startService(intent)
            }
        }
    }
}