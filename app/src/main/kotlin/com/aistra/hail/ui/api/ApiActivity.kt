package com.aistra.hail.ui.api

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailApi
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HShortcuts
import com.aistra.hail.utils.HTarget
import com.aistra.hail.utils.HUI
import com.aistra.hail.work.HWork.setAutoFreeze
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ApiActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching {
            when (intent.action) {
                Intent.ACTION_SHOW_APP_INFO -> {
                    redirect(requirePackage(if (HTarget.N) Intent.EXTRA_PACKAGE_NAME else "android.intent.extra.PACKAGE_NAME"))
                    return
                }

                HailApi.ACTION_LAUNCH -> launchApp(
                    requirePackage(), runCatching { requireTagId }.getOrNull()
                )

                HailApi.ACTION_FREEZE -> setAppFrozen(requirePackage(), true)
                HailApi.ACTION_UNFREEZE -> setAppFrozen(requirePackage(), false)
                HailApi.ACTION_FREEZE_TAG -> setListFrozen(
                    true, HailData.checkedList.filter { it.tagId == requireTagId }, true
                )

                HailApi.ACTION_UNFREEZE_TAG -> setListFrozen(
                    false,
                    HailData.checkedList.filter { it.tagId == requireTagId })

                HailApi.ACTION_FREEZE_ALL -> setListFrozen(true)
                HailApi.ACTION_UNFREEZE_ALL -> setListFrozen(false)
                HailApi.ACTION_FREEZE_NON_WHITELISTED -> setListFrozen(true, skipWhitelisted = true)
                HailApi.ACTION_FREEZE_AUTO -> setAutoFreeze(false)
                HailApi.ACTION_LOCK -> lockScreen(false)
                HailApi.ACTION_LOCK_FREEZE -> lockScreen(true)
                else -> throw IllegalArgumentException("unknown action:\n${intent.action}")
            }
            finish()
        }.onFailure {
            showErrorDialog(it)
        }
    }

    private fun showErrorDialog(t: Throwable) {
        MaterialAlertDialogBuilder(this).setMessage(t.message ?: t.stackTraceToString())
            .setPositiveButton(android.R.string.ok, null).setOnDismissListener { finish() }.show()
    }

    private fun requirePackage(extraName: String = HailData.KEY_PACKAGE): String =
        intent?.getStringExtra(extraName)?.also {
            HPackages.getApplicationInfoOrNull(it)
                ?: throw NameNotFoundException(getString(R.string.app_not_installed))
        } ?: throw IllegalArgumentException("package must not be null")

    private val requireTagId: Int
        get() = intent?.getStringExtra(HailData.KEY_TAG)?.let {
            HailData.tags.find { tag -> tag.first == it }?.second
                ?: throw IllegalStateException("tag unavailable:\n$it")
        } ?: throw IllegalArgumentException("tag must not be null")

    private fun redirect(pkg: String) {
        var shouldFinished = true
        MaterialAlertDialogBuilder(this).setTitle(
            HPackages.getApplicationInfoOrNull(pkg)?.loadLabel(packageManager) ?: pkg
        ).setItems(R.array.api_redirect_action_entries) { _, which ->
            runCatching {
                when (which) {
                    0 -> launchApp(pkg)
                    1 -> {
                        if (!HailData.isChecked(pkg)) HailData.addCheckedApp(pkg)
                        setAppFrozen(pkg, true)
                    }

                    2 -> setAppFrozen(pkg, false)
                }
            }.onFailure {
                shouldFinished = false
                showErrorDialog(it)
            }
        }.setNegativeButton(android.R.string.cancel, null)
            .setOnDismissListener { if (shouldFinished) finish() }.show()
    }

    private fun launchApp(pkg: String, tagId: Int? = null) {
        if (tagId != null) setListFrozen(false, HailData.checkedList.filter { it.tagId == tagId })
        if (AppManager.isAppFrozen(pkg) && AppManager.setAppFrozen(pkg, false)) {
            app.setAutoFreezeService()
        }
        packageManager.getLaunchIntentForPackage(pkg)?.let {
            HShortcuts.addDynamicShortcut(pkg)
            startActivity(it)
        } ?: throw ActivityNotFoundException(getString(R.string.activity_not_found))
    }

    private fun setAppFrozen(pkg: String, frozen: Boolean) = when {
        frozen && !HailData.isChecked(pkg) -> throw SecurityException("package not checked")
        AppManager.isAppFrozen(pkg) != frozen && !AppManager.setAppFrozen(
            pkg, frozen
        ) -> throw IllegalStateException(getString(R.string.permission_denied))

        else -> {
            HUI.showToast(
                if (frozen) R.string.msg_freeze else R.string.msg_unfreeze,
                HPackages.getApplicationInfoOrNull(pkg)?.loadLabel(packageManager) ?: pkg
            )
            app.setAutoFreezeService()
        }
    }

    private fun setListFrozen(
        frozen: Boolean,
        list: List<AppInfo> = HailData.checkedList,
        skipWhitelisted: Boolean = false
    ) {
        val filtered =
            list.filter { AppManager.isAppFrozen(it.packageName) != frozen && !(skipWhitelisted && it.whitelisted) }
        when (val result = AppManager.setListFrozen(frozen, *filtered.toTypedArray())) {
            null -> throw IllegalStateException(getString(R.string.permission_denied))
            else -> {
                HUI.showToast(
                    if (frozen) R.string.msg_freeze else R.string.msg_unfreeze,
                    result
                )
                app.setAutoFreezeService()
            }
        }
    }

    private fun lockScreen(freezeAll: Boolean) {
        if (freezeAll) setListFrozen(true)
        if (AppManager.lockScreen.not()) throw IllegalStateException(getString(R.string.permission_denied))
    }
}