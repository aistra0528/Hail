package com.aistra.hail.ui.records

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.aistra.hail.R
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.ui.main.MainFragment
import com.aistra.hail.ui.theme.AppTheme
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.AppIconCache
import com.aistra.hail.utils.HPackages.myUserId
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class LaunchRecordsFragment : MainFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { AppTheme { LaunchRecordsScreen() } }
        }
}

private enum class RecordPeriod {
    DAY, WEEK, YEAR;

    fun startAt(now: Long): Long = Calendar.getInstance().apply {
        timeInMillis = now
        when (this@RecordPeriod) {
            DAY -> {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            WEEK -> {
                val daysFromWeekStart = (get(Calendar.DAY_OF_WEEK) - firstDayOfWeek + 7) % 7
                add(Calendar.DAY_OF_MONTH, -daysFromWeekStart)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            YEAR -> {
                set(Calendar.MONTH, Calendar.JANUARY)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }
    }.timeInMillis
}

internal data class AppLaunchSummary(
    val packageName: String,
    val name: String,
    val count: Int,
    val lastLaunch: Long,
)

@Composable
private fun LaunchRecordsScreen() {
    var selectedPeriod by remember { mutableIntStateOf(0) }
    var refreshKey by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val now = System.currentTimeMillis()
    val records = remember(refreshKey) { HailData.launchRecords.toList() }
    val period = RecordPeriod.entries[selectedPeriod]
    val summaries = remember(records, period, now / 60_000) {
        val start = period.startAt(now)
        records.asSequence()
            .filter { it.timestamp >= start && it.timestamp <= now }
            .groupBy { it.packageName }
            .map { (packageName, appRecords) ->
                val appInfo = HPackages.getApplicationInfoOrNull(packageName)
                AppLaunchSummary(
                    packageName = packageName,
                    name = appInfo?.loadLabel(context.packageManager)?.toString() ?: packageName,
                    count = appRecords.size,
                    lastLaunch = appRecords.maxOf { it.timestamp },
                )
            }
            .sortedWith(compareByDescending<AppLaunchSummary> { it.count }.thenByDescending { it.lastLaunch })
            .toList()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.launch_records_subtitle),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = {
                    HailData.clearLaunchRecords()
                    refreshKey++
                },
                enabled = records.isNotEmpty(),
            ) {
                Icon(Icons.Outlined.DeleteSweep, contentDescription = stringResource(R.string.clear_launch_records))
            }
        }

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            listOf(
                R.string.launch_records_day,
                R.string.launch_records_week,
                R.string.launch_records_year,
            ).forEachIndexed { index, label ->
                SegmentedButton(
                    selected = selectedPeriod == index,
                    onClick = { selectedPeriod = index },
                    shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(index, 3),
                    icon = {},
                ) {
                    Text(stringResource(label))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.History,
                value = summaries.sumOf { it.count }.toString(),
                label = stringResource(R.string.launch_count),
            )
            SummaryCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Apps,
                value = summaries.size.toString(),
                label = stringResource(R.string.launch_app_count),
            )
        }

        if (summaries.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Outlined.History, contentDescription = null, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.launch_records_empty), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.launch_records_empty_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(top = 16.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text(
                        stringResource(R.string.launch_records_ranking),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                items(summaries, key = { it.packageName }) { summary ->
                    LaunchSummaryItem(summary)
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String) {
    Card(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(value, style = MaterialTheme.typography.titleLarge)
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun LaunchSummaryItem(
    summary: AppLaunchSummary,
    onClick: () -> Unit = {},
    showStatusBorder: Boolean = false,
    statusRefreshKey: Int = 0,
) {
    val appInfo: ApplicationInfo? = remember(summary.packageName) {
        HPackages.getApplicationInfoOrNull(summary.packageName)
    }
    val isFrozen = remember(summary.packageName, statusRefreshKey) {
        AppManager.isAppFrozen(summary.packageName)
    }
    val statusColor = if (isFrozen) MaterialTheme.colorScheme.outline else Color(0xFF2E7D32)
    val cardModifier = if (showStatusBorder) {
        Modifier.drawWithContent {
            drawContent()
            val strokeWidth = 4.dp.toPx()
            drawLine(
                color = statusColor,
                start = Offset(strokeWidth / 2, 12.dp.toPx()),
                end = Offset(strokeWidth / 2, size.height - 12.dp.toPx()),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
    } else Modifier
    Card(modifier = cardModifier, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
                AndroidView(
                    factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.CENTER_INSIDE } },
                    update = { imageView ->
                        appInfo?.let {
                            AppIconCache.loadIconBitmapAsync(
                                imageView.context,
                                it,
                                myUserId,
                                imageView,
                                HailData.grayscaleIcon && isFrozen,
                            )
                        } ?: run {
                            imageView.setImageDrawable(imageView.context.packageManager.defaultActivityIcon)
                            imageView.colorFilter = null
                        }
                    },
                    modifier = Modifier.size(48.dp)
                )
                Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                    Text(summary.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.launch_last_time, formatLaunchTime(summary.lastLaunch)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(summary.count.toString(), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.launch_times), style = MaterialTheme.typography.labelMedium)
                }
        }
    }
}

internal fun formatLaunchTime(timestamp: Long): String =
    SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
