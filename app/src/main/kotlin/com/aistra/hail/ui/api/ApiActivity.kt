package com.aistra.hail.ui.api

import android.content.ActivityNotFoundException
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Bundle
import com.aistra.hail.R
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailApi
import com.aistra.hail.app.HailData
import com.aistra.hail.ui.HailActivity
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HShortcuts
import com.aistra.hail.utils.HUI
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ApiActivity : HailActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            when (intent.action) {
                HailApi.ACTION_LAUNCH -> launchApp(targetPackage)
                HailApi.ACTION_FREEZE -> setAppFrozen(targetPackage, true)
                HailApi.ACTION_UNFREEZE -> setAppFrozen(targetPackage, false)
                HailApi.ACTION_FREEZE_ALL -> setAllFrozen(true)
                HailApi.ACTION_UNFREEZE_ALL -> setAllFrozen(false)
                HailApi.ACTION_FREEZE_NON_WHITELISTED -> setAllFrozen(true, skipWhitelisted = true)
                HailApi.ACTION_LOCK -> lockScreen(false)
                HailApi.ACTION_LOCK_FREEZE -> lockScreen(true)
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
        if (AppManager.isAppFrozen(target)) {
            if (AppManager.setAppFrozen(target, false)) setAutoFreezeService()
            else throw IllegalStateException(getString(R.string.permission_denied))
        }
        packageManager.getLaunchIntentForPackage(target)?.let {
            HShortcuts.addDynamicShortcut(target)
            startActivity(it)
        } ?: throw ActivityNotFoundException(getString(R.string.activity_not_found))
    }

    private fun setAppFrozen(target: String, frozen: Boolean) = when {
        frozen && HailData.isChecked(target).not() ->
            throw SecurityException("package not checked")
        AppManager.isAppFrozen(target) != frozen && AppManager.setAppFrozen(target, frozen).not() ->
            throw IllegalStateException(getString(R.string.permission_denied))
        else -> {
            HUI.showToast(
                if (frozen) R.string.msg_freeze else R.string.msg_unfreeze,
                HPackages.getApplicationInfoOrNull(target)?.loadLabel(packageManager) ?: target
            )
            setAutoFreezeService()
        }
    }

    private fun setAllFrozen(frozen: Boolean, skipWhitelisted: Boolean = false) {
        var i = 0
        var denied = false
        var name = String()
        HailData.checkedList.forEach {
            when {
                AppManager.isAppFrozen(it.packageName) == frozen || (skipWhitelisted && it.whitelisted) -> return@forEach
                AppManager.setAppFrozen(it.packageName, frozen) -> {
                    i++
                    name = it.name.toString()
                }
                it.packageName != packageName && it.applicationInfo != null -> denied = true
            }
        }
        when {
            denied && i == 0 -> throw IllegalStateException(getString(R.string.permission_denied))
            i > 0 -> {
                HUI.showToast(
                    if (frozen) R.string.msg_freeze else R.string.msg_unfreeze,
                    if (i > 1) i.toString() else name
                )
                setAutoFreezeService(!frozen)
            }
        }
    }

    private fun lockScreen(freezeAll: Boolean) {
        if (freezeAll) setAllFrozen(true)
        if (AppManager.lockScreen.not()) throw IllegalStateException(getString(R.string.permission_denied))
    }
}