package com.aistra.hail.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.services.AutoFreezeService
import com.aistra.hail.utils.HTarget

open class HailActivity : AppCompatActivity() {
    fun setAutoFreezeService(hasUnfrozen: Boolean? = null) {
        if (HailData.autoFreezeAfterLock.not()) return
        val start = hasUnfrozen
            ?: HailData.checkedList.any { !AppManager.isAppFrozen(it.packageName) && !it.whitelisted }
        applicationContext.let {
            val intent = Intent(it, AutoFreezeService::class.java)
            when {
                !start -> it.stopService(intent)
                HTarget.O -> it.startForegroundService(intent)
                else -> it.startService(intent)
            }
        }
    }
}