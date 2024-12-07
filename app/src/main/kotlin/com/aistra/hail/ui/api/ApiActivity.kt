package com.aistra.hail.ui.api

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Launch
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.BrightnessLow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailApi
import com.aistra.hail.app.HailData
import com.aistra.hail.ui.theme.AppTheme
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HShortcuts
import com.aistra.hail.utils.HTarget
import com.aistra.hail.utils.HUI
import com.aistra.hail.work.HWork.setAutoFreeze

class ApiActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching {
            when (intent.action) {
                Intent.ACTION_SHOW_APP_INFO -> {
                    val pkg =
                        requirePackage(if (HTarget.N) Intent.EXTRA_PACKAGE_NAME else "android.intent.extra.PACKAGE_NAME")
                    setContent { AppTheme { RedirectBottomSheet(pkg) } }
                    return
                }

                HailApi.ACTION_LAUNCH -> launchApp(requirePackage(), runCatching { requireTagId }.getOrNull())

                HailApi.ACTION_FREEZE -> setAppFrozen(requirePackage(), true)
                HailApi.ACTION_UNFREEZE -> setAppFrozen(requirePackage(), false)
                HailApi.ACTION_FREEZE_TAG -> setListFrozen(
                    true, HailData.checkedList.filter { it.tagId == requireTagId }, true
                )

                HailApi.ACTION_UNFREEZE_TAG -> setListFrozen(
                    false, HailData.checkedList.filter { it.tagId == requireTagId })

                HailApi.ACTION_FREEZE_ALL -> setListFrozen(true)
                HailApi.ACTION_UNFREEZE_ALL -> setListFrozen(false)
                HailApi.ACTION_FREEZE_NON_WHITELISTED -> setListFrozen(true, skipWhitelisted = true)
                HailApi.ACTION_FREEZE_AUTO -> setAutoFreeze(false)
                HailApi.ACTION_LOCK -> lockScreen(false)
                HailApi.ACTION_LOCK_FREEZE -> lockScreen(true)
                else -> throw IllegalArgumentException("unknown action:\n${intent.action}")
            }
            finish()
        }.onFailure(::setErrorDialog)
    }

    private fun setErrorDialog(t: Throwable) = setContent { AppTheme { ErrorDialog(t) } }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun RedirectBottomSheet(pkg: String) = ModalBottomSheet(
        onDismissRequest = ::finish, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column {
            Text(
                text = HPackages.getApplicationInfoOrNull(pkg)?.loadLabel(packageManager)?.toString() ?: pkg,
                modifier = Modifier.padding(
                    horizontal = dimensionResource(R.dimen.padding_medium),
                    vertical = dimensionResource(R.dimen.padding_small)
                ),
                style = MaterialTheme.typography.headlineSmall
            )
            ClickableItem(
                icon = Icons.AutoMirrored.Outlined.Launch, title = R.string.action_launch
            ) { launchApp(pkg) }
            ClickableItem(
                icon = Icons.Rounded.AcUnit, title = R.string.action_freeze
            ) {
                if (!HailData.isChecked(pkg)) HailData.addCheckedApp(pkg)
                setAppFrozen(pkg, true)
            }
            ClickableItem(
                icon = Icons.Rounded.BrightnessLow, title = R.string.action_unfreeze
            ) { setAppFrozen(pkg, false) }
        }
    }

    @Composable
    private fun ClickableItem(icon: ImageVector, @StringRes title: Int, onClick: () -> Unit) = Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = {
            runCatching {
                onClick()
                finish()
            }.onFailure(::setErrorDialog)
        }), verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.padding(dimensionResource(R.dimen.padding_medium))
        )
        Text(text = stringResource(title), style = MaterialTheme.typography.bodyLarge)
    }

    @Composable
    private fun ErrorDialog(t: Throwable) = AlertDialog(
        text = { Text(text = t.message ?: t.stackTraceToString()) },
        onDismissRequest = ::finish,
        confirmButton = {
            TextButton(onClick = ::finish) {
                Text(text = stringResource(android.R.string.ok))
            }
        })

    private fun requirePackage(extraName: String = HailData.KEY_PACKAGE): String =
        intent?.getStringExtra(extraName)?.also {
            HPackages.getApplicationInfoOrNull(it) ?: throw NameNotFoundException(getString(R.string.app_not_installed))
        } ?: throw IllegalArgumentException("package must not be null")

    private val requireTagId: Int
        get() = intent?.getStringExtra(HailData.KEY_TAG)?.let {
            HailData.tags.find { tag -> tag.first == it }?.second
                ?: throw IllegalStateException("tag unavailable:\n$it")
        } ?: throw IllegalArgumentException("tag must not be null")

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
        frozen: Boolean, list: List<AppInfo> = HailData.checkedList, skipWhitelisted: Boolean = false
    ) {
        val filtered =
            list.filter { AppManager.isAppFrozen(it.packageName) != frozen && !(skipWhitelisted && it.whitelisted) }
        when (val result = AppManager.setListFrozen(frozen, *filtered.toTypedArray())) {
            null -> throw IllegalStateException(getString(R.string.permission_denied))
            else -> {
                HUI.showToast(
                    if (frozen) R.string.msg_freeze else R.string.msg_unfreeze, result
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