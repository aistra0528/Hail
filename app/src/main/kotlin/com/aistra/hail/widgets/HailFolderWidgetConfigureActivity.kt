package com.aistra.hail.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo
import com.aistra.hail.ui.theme.AppTheme
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.NameComparator

class HailFolderWidgetConfigureActivity : ComponentActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)
        appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        setContent {
            AppTheme {
                ConfigureScreen(
                    initialConfig = HailFolderWidgetStore.loadConfig(this, appWidgetId),
                    onCancel = ::finish,
                    onSave = ::saveWidget
                )
            }
        }
    }

    private fun saveWidget(config: HailFolderWidgetStore.Config) {
        HailFolderWidgetStore.save(this, appWidgetId, config)
        HailFolderWidgetProvider.update(this, AppWidgetManager.getInstance(this), intArrayOf(appWidgetId))
        setResult(
            Activity.RESULT_OK,
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        )
        finish()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ConfigureScreen(
        initialConfig: HailFolderWidgetStore.Config,
        onCancel: () -> Unit,
        onSave: (HailFolderWidgetStore.Config) -> Unit
    ) {
        val allApps = remember {
            HailFolderWidgetStore.checkedApps().sortedWith(NameComparator)
        }
        var title by remember { mutableStateOf(initialConfig.title) }
        var iconSize by remember { mutableFloatStateOf(initialConfig.iconSize.toFloat()) }
        var backgroundAlpha by remember { mutableFloatStateOf(initialConfig.backgroundAlpha / 255f) }
        var showNames by remember { mutableStateOf(initialConfig.showNames) }
        val selectedKeys = remember {
            mutableStateListOf<String>().apply {
                addAll(initialConfig.apps.map { it.key }.filter { key -> allApps.any { it.key == key } })
            }
        }
        val selectedApps = allApps.filter { it.key in selectedKeys }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(title = { Text(stringResource(R.string.widget_folder_configure)) })
            },
            bottomBar = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(dimensionResource(R.dimen.padding_medium)),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                ) {
                    TextButton(onClick = onCancel) {
                        Text(stringResource(android.R.string.cancel))
                    }
                    Button(
                        enabled = selectedApps.isNotEmpty(),
                        onClick = {
                            onSave(
                                    HailFolderWidgetStore.Config(
                                        title = title.trim(),
                                    iconSize = iconSize.toInt(),
                                    backgroundAlpha = (backgroundAlpha * 255).toInt().coerceIn(0, 255),
                                    showNames = showNames,
                                    apps = selectedApps
                                )
                            )
                        }
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(
                    start = dimensionResource(R.dimen.padding_medium),
                    end = dimensionResource(R.dimen.padding_medium),
                    top = 12.dp,
                    bottom = 12.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(R.string.widget_folder_title_label)) },
                        supportingText = { Text(stringResource(R.string.widget_folder_title_hint)) }
                    )
                }
                item {
                    Text(
                        text = stringResource(R.string.widget_icon_size_value, iconSize.toInt()),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Slider(
                        value = iconSize,
                        onValueChange = { iconSize = it },
                        valueRange = HailFolderWidgetStore.ICON_SIZE_MIN.toFloat()..HailFolderWidgetStore.ICON_SIZE_MAX.toFloat()
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showNames = !showNames }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.widget_show_app_names),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Switch(checked = showNames, onCheckedChange = { showNames = it })
                    }
                }
                item {
                    Text(
                        text = stringResource(R.string.widget_background_alpha, (backgroundAlpha * 100).toInt()),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Slider(
                        value = backgroundAlpha,
                        onValueChange = { backgroundAlpha = it },
                        valueRange = 0f..1f
                    )
                }
                item {
                    Text(stringResource(R.string.widget_folder_apps), style = MaterialTheme.typography.titleSmall)
                }
                if (allApps.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.widget_folder_no_checked_apps),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    items(allApps, key = { it.key }) { appInfo ->
                        AppRow(
                            appInfo = appInfo,
                            checked = appInfo.key in selectedKeys,
                            onCheckedChange = { checked ->
                                if (checked && appInfo.key !in selectedKeys) selectedKeys.add(appInfo.key)
                                else if (!checked) selectedKeys.remove(appInfo.key)
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun AppRow(appInfo: AppInfo, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(appInfo.name.toString(), style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = if (appInfo.userId == HPackages.myUserId) appInfo.packageName else "${appInfo.packageName} (${appInfo.userId})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    private val AppInfo.key get() = "${packageName}#${userId}"
}
