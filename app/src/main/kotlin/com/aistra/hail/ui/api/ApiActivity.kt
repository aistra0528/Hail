package com.aistra.hail.ui.api

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aistra.hail.HailApp
import com.aistra.hail.R
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailApi
import com.aistra.hail.app.HailData
import com.aistra.hail.services.AutoFreezeService
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HUI
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ApiActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            when (intent.action) {
                HailApi.ACTION_LAUNCH -> launchApp(targetPackage)
                HailApi.ACTION_FREEZE -> setAppFrozen(targetPackage, true)
                HailApi.ACTION_UNFREEZE -> setAppFrozen(targetPackage, false)
                HailApi.ACTION_FREEZE_ALL -> setAllFrozen(true)
                HailApi.ACTION_UNFREEZE_ALL -> setAllFrozen(false)
                HailApi.ACTION_LOCK -> lockScreen(false)
                HailApi.ACTION_LOCK_FREEZE -> lockScreen(true)
                HailApi.ACTION_START_AUTO_FREEZE_SERVICE -> startAutoFreezeService()
                HailApi.ACTION_STOP_AUTO_FREEZE_SERVICE -> stopAutoFreezeService()
                else -> throw IllegalArgumentException("unknown action:\n${intent.action}")
            }
            finish()
        } catch (t: Throwable) {
            MaterialAlertDialogBuilder(this)
                .setMessage(t.message ?: t.stackTraceToString())
                .setPositiveButton(android.R.string.ok, null)
                .setOnDismissListener { finish() }
                .create().show()
        }
    }

    private val targetPackage: String
        get() = intent?.extras?.getString(HailData.KEY_PACKAGE)?.also {
            HPackages.getPackageInfoOrNull(it)
                ?: throw NameNotFoundException(getString(R.string.app_not_installed))
        } ?: throw IllegalArgumentException("package must not be null")

    private fun launchApp(target: String) {
        if (AppManager.isAppFrozen(target) && AppManager.setAppFrozen(target, false).not())
            throw IllegalStateException(getString(R.string.permission_denied))
        startAutoFreezeService()
        packageManager.getLaunchIntentForPackage(target)?.let {
            startActivity(it)
        } ?: throw ActivityNotFoundException(getString(R.string.activity_not_found))
    }

    private fun setAppFrozen(target: String, frozen: Boolean) = when {
        frozen && HailData.isChecked(target).not() ->
            throw SecurityException("package not checked")
        AppManager.isAppFrozen(target) != frozen && AppManager.setAppFrozen(target, frozen).not() ->
            throw IllegalStateException(getString(R.string.permission_denied))
        else -> HUI.showToast(
            if (frozen) R.string.msg_freeze else R.string.msg_unfreeze,
            HPackages.getApplicationInfoOrNull(target)?.loadLabel(packageManager) ?: target
        )
    }

    private fun setAllFrozen(frozen: Boolean) {
        var i = 0
        var denied = false
        HailData.checkedList.forEach {
            when {
                AppManager.isAppFrozen(it.packageName) == frozen -> return@forEach
                AppManager.setAppFrozen(it.packageName, frozen) -> i++
                it.packageName != packageName && it.applicationInfo != null -> denied = true
            }
        }
        when {
            denied && i == 0 -> throw IllegalStateException(getString(R.string.permission_denied))
            i > 0 -> {
                HUI.showToast(
                    if (frozen) R.string.msg_freeze else R.string.msg_unfreeze, i.toString()
                )
                if (frozen) stopAutoFreezeService() else startAutoFreezeService()
            }
        }
    }

    private fun lockScreen(freezeAll: Boolean) {
        if (freezeAll) setAllFrozen(true)
        if (AppManager.lockScreen.not())
            throw IllegalStateException(getString(R.string.permission_denied))
    }

    private fun startAutoFreezeService() {
        if (HailData.autoFreezeAfterLock) {
            val intent = Intent(HailApp.app, AutoFreezeService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                applicationContext.startForegroundService(intent)
            else
                applicationContext.startService(intent)
        }
    }

    private fun stopAutoFreezeService() {
        if (HailData.autoFreezeAfterLock) {
            val intent = Intent(HailApp.app, AutoFreezeService::class.java)
            applicationContext.stopService(intent)
        }
    }
}