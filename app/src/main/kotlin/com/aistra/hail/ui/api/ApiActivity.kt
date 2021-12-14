package com.aistra.hail.ui.api

import android.content.ActivityNotFoundException
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aistra.hail.R
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailApi
import com.aistra.hail.app.HailData
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
        } ?: throw IllegalArgumentException("package cannot be null")

    private fun launchApp(target: String) {
        if (AppManager.isAppFrozen(target) && AppManager.setAppFrozen(target, false).not())
            throw IllegalStateException(getString(R.string.permission_denied))
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
            getString(
                if (frozen) R.string.msg_freeze else R.string.msg_unfreeze,
                HPackages.getApplicationInfoOrNull(target)?.loadLabel(packageManager)
                    ?: target
            )
        )
    }

    private fun setAllFrozen(frozen: Boolean) {
        var i = 0
        HailData.checkedList.forEach {
            when {
                AppManager.isAppFrozen(it.packageName) == frozen -> return@forEach
                AppManager.setAppFrozen(it.packageName, frozen) -> i++
                it.packageName != packageName && it.applicationInfo != null ->
                    throw IllegalStateException(getString(R.string.permission_denied))
            }
        }
        HUI.showToast(
            getString(
                if (frozen) R.string.msg_freeze else R.string.msg_unfreeze, i.toString()
            )
        )
    }

    private fun lockScreen(freezeAll: Boolean) {
        if (freezeAll) setAllFrozen(true)
        if (AppManager.lockScreen.not())
            throw IllegalStateException(getString(R.string.permission_denied))
    }
}