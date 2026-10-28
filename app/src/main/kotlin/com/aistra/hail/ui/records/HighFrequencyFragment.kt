package com.aistra.hail.ui.records

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.ui.main.MainFragment
import com.aistra.hail.ui.theme.AppTheme
import com.aistra.hail.utils.HIsland
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HShizuku
import com.aistra.hail.utils.HShortcuts
import com.aistra.hail.utils.HUI
import com.google.android.material.dialog.MaterialAlertDialogBuilder
class HighFrequencyFragment : MainFragment() {
    private var refreshKey by mutableIntStateOf(0)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { AppTheme { HighFrequencyScreen(::launchApp, refreshKey) } }
        }

    private fun launchApp(packageName: String) {
        if (!AppManager.unfreezeForLaunch(packageName)) {
            HUI.showToast(R.string.permission_denied)
            return
        } else {
            app.setAutoFreezeService()
        }
        if (HailData.workingMode == HailData.MODE_ISLAND_HIDE) {
            HIsland.ensureLaunchIntentExists(packageName)
        }
        app.packageManager.getLaunchIntentForPackage(packageName)?.let {
            HailData.recordAppLaunch(packageName)
            HShortcuts.addDynamicShortcut(packageName)
            startActivity(it)
        } ?: HUI.showToast(R.string.activity_not_found)
    }

    private fun freezeHighFrequencyApps(freezeAll: Boolean = false) {
        val list = if (freezeAll) {
            HailData.checkedList
        } else {
            val frequentPackages = HailData.launchRecords.asSequence()
                .filter { HailData.isChecked(it.packageName) }
                .map { it.packageName }
                .toSet()
            HailData.checkedList.filter { it.packageName in frequentPackages }
        }
        setListFrozen(true, list, skipWhitelisted = !freezeAll)
    }

    private fun setListFrozen(frozen: Boolean, list: List<AppInfo>, skipWhitelisted: Boolean) {
        if (HailData.workingMode == HailData.MODE_DEFAULT) {
            MaterialAlertDialogBuilder(activity).setMessage(R.string.msg_guide)
                .setPositiveButton(android.R.string.ok, null).show()
            return
        } else if (HailData.workingMode == HailData.MODE_SHIZUKU_HIDE) {
            runCatching { HShizuku.isRoot }.onSuccess {
                if (!it) {
                    MaterialAlertDialogBuilder(activity).setMessage(R.string.shizuku_hide_adb)
                        .setPositiveButton(android.R.string.ok, null).show()
                    return
                }
            }
        }
        val filtered = list.filter {
            AppManager.isAppFrozen(it.packageName) != frozen && (!skipWhitelisted || !it.whitelisted)
        }
        when (val result = AppManager.setListFrozen(frozen, *filtered.toTypedArray())) {
            null -> HUI.showToast(R.string.permission_denied)
            else -> {
                refreshKey++
                HUI.showToast(if (frozen) R.string.msg_freeze else R.string.msg_unfreeze, result)
                app.setAutoFreezeService()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshKey++
        activity.fab.setOnClickListener { freezeHighFrequencyApps() }
        activity.fab.setOnLongClickListener {
            freezeHighFrequencyApps(freezeAll = true)
            true
        }
    }

    override fun onPause() {
        activity.fab.setOnClickListener(null)
        activity.fab.setOnLongClickListener(null)
        super.onPause()
    }
}

@Composable
private fun HighFrequencyScreen(onLaunch: (String) -> Unit, refreshKey: Int) {
    val context = LocalContext.current
    val records = remember(refreshKey) { HailData.launchRecords.toList() }
    val summaries = remember(records) {
        records.filter { HailData.isChecked(it.packageName) }
            .groupBy { it.packageName }
            .mapNotNull { (packageName, appRecords) ->
                val appInfo = HPackages.getApplicationInfoOrNull(packageName) ?: return@mapNotNull null
                AppLaunchSummary(
                    packageName = packageName,
                    name = appInfo.loadLabel(context.packageManager).toString(),
                    count = appRecords.size,
                    lastLaunch = appRecords.maxOf { it.timestamp },
                )
            }
            .sortedWith(compareByDescending<AppLaunchSummary> { it.count }.thenByDescending { it.lastLaunch })
    }

    if (summaries.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.AutoMirrored.Outlined.TrendingUp, contentDescription = null, modifier = Modifier.padding(8.dp))
            Text(stringResource(R.string.high_frequency_empty), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.high_frequency_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                stringResource(R.string.high_frequency_subtitle),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        items(summaries, key = { it.packageName }) { summary ->
            LaunchSummaryItem(
                summary,
                onClick = { onLaunch(summary.packageName) },
                showStatusBorder = true,
                statusRefreshKey = refreshKey,
            )
        }
    }
}
