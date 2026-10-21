package com.aistra.hail.ui.apps

import android.content.pm.ApplicationInfo
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.aistra.hail.BuildConfig
import com.aistra.hail.R
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.app.HailData.toEntry
import com.aistra.hail.ui.components.*
import com.aistra.hail.ui.components.AppDestinations.APPS
import com.aistra.hail.utils.HFiles
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HPackages.exportFileName
import com.aistra.hail.utils.HPackages.hidden
import com.aistra.hail.utils.HPackages.labelString
import com.aistra.hail.utils.HPackages.stopped
import com.aistra.hail.utils.HPackages.suspended
import com.aistra.hail.utils.HPackages.uninstalled
import com.aistra.hail.utils.HTarget
import com.aistra.hail.utils.HUI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.compose.preference.rememberPreferenceState
import java.io.FileInputStream
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(model: AppsViewModel, navController: NavController) {
    val query by model.query.collectAsState()
    val focusRequester = remember { FocusRequester() }
    var showFilterSettings by rememberSaveable { mutableStateOf(false) }
    var filterState by rememberPreferenceState(HailData.FILTER_STATE, HailData.filterState)
    val showNavigationBar by rememberPreferenceState(HailData.SHOW_NAVIGATION_BAR, HailData.showNavigationBar)
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    LaunchedEffect(query) {
        if (query == "") focusRequester.requestFocus()
    }
    if (showFilterSettings) FilterDialog(
        model = model,
        onDismiss = { showFilterSettings = false },
        filterState = filterState,
        onFilterStateChanged = { filterState = it })
    Scaffold(modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection), topBar = {
        TopAppBar(
            title = {
                if (query == null) Text(
                    text = if (filterState == HailData.ACTION_NONE) stringResource(APPS.label)
                    else filterState.toEntry(HailData.FILTER_STATE_VALUES, R.array.filter_state_entries)
                )
            }, navigationIcon = { if (!showNavigationBar) NavigateUpButton(navController = navController) }, actions = {
                if (query != null) SearchBar(state = rememberSearchBarState(), inputField = {
                    TextField(
                        value = query!!,
                        onValueChange = { model.postQuery(it) },
                        modifier = Modifier.focusRequester(focusRequester),
                        placeholder = { Text(stringResource(R.string.action_search)) },
                        trailingIcon = {
                            IconButton(onClick = { model.postQuery(null) }) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = stringResource(android.R.string.cancel)
                                )
                            }
                        },
                        keyboardOptions = if (HailData.nineKeySearch) KeyboardOptions(keyboardType = KeyboardType.Phone) else KeyboardOptions.Default,
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }, modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 8.dp))
                else {
                    IconButton(onClick = { model.postQuery("") }) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = stringResource(R.string.action_search)
                        )
                    }
                    IconButton(onClick = { showFilterSettings = true }) {
                        Icon(
                            imageVector = Icons.Outlined.FilterList,
                            contentDescription = stringResource(R.string.filter_title)
                        )
                    }
                }
            }, scrollBehavior = scrollBehavior
        )
    }) { contentPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            AppsContent(model = model, filterState = filterState, query = query)
        }
    }
}

@Composable
fun AppsContent(model: AppsViewModel, filterState: String, query: String?) {
    val displayApps by model.displayApps.collectAsState()
    val isRefreshing by model.isRefreshing.collectAsState()
    var selected by remember { mutableStateOf<ApplicationInfo?>(null) }
    var showExtractingIndicator by remember { mutableStateOf(false) }
    var showUninstallDialog by remember { mutableStateOf(false) }
    if (displayApps.isEmpty()) {
        if (filterState != HailData.ACTION_NONE || !query.isNullOrEmpty()) Tips(id = R.string.no_matching_results)
        else Tips(id = R.string.denied_get_installed_apps)
    } else if (displayApps.size == 1 && displayApps[0].packageName == BuildConfig.APPLICATION_ID && filterState == HailData.ACTION_NONE && query.isNullOrEmpty()) {
        Tips(id = R.string.denied_get_installed_apps)
    } else PullToRefreshBox(
        isRefreshing = isRefreshing, onRefresh = { model.updateAppList() }, modifier = Modifier.fillMaxSize()
    ) {
        BoxWithConstraints {
            LazyVerticalGrid(
                columns = GridCells.Fixed(max(1, (maxWidth / 320.dp).toInt())), modifier = Modifier.fillMaxSize()
            ) {
                items(displayApps, key = { it.packageName }) { info ->
                    var checked by remember(info.packageName) { mutableStateOf(HailData.isChecked(info.packageName)) }
                    AppsItem(info = info, isChecked = checked, onCheckedChange = { isChecked ->
                        if (isChecked) HailData.addCheckedApp(info.packageName)
                        else HailData.removeCheckedApp(info.packageName)
                        checked = isChecked
                    }, onClick = { selected = info })
                }
            }
        }
    }
    selected?.let {
        if (showExtractingIndicator) ExtractingIndicator(info = it, onDismiss = {
            selected = null
            showExtractingIndicator = false
        }) else if (showUninstallDialog) UninstallDialog(model = model, info = it, onDismiss = {
            selected = null
            showUninstallDialog = false
        }) else ActionsDialog(
            model = model,
            info = it,
            onDismiss = { selected = null },
            onExtract = { showExtractingIndicator = true },
            onUninstall = { showUninstallDialog = true })
    }
}

@Composable
private fun Tips(@StringRes id: Int) = Column(
    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
) {
    Text(
        text = stringResource(id),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    if (id == R.string.denied_get_installed_apps) TextButton(onClick = {
        HUI.startActivity(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS, HPackages.packageUri(BuildConfig.APPLICATION_ID)
        )
    }) {
        Text(text = stringResource(R.string.action_details))
    }
}

@Composable
private fun AppsItem(
    info: ApplicationInfo, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit, onClick: () -> Unit
) = Row(
    modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = {
        HUI.startActivity(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, HPackages.packageUri(info.packageName))
    }), verticalAlignment = Alignment.CenterVertically
) {
    val frozen = AppManager.isAnyFrozen(info.packageName)
    val contentDisabled = frozen && HailData.grayscaleIcon
    BadgedBox(
        badge = {
            if (info.stopped.not()) Box(
                modifier = Modifier.background(
                    color = MaterialTheme.colorScheme.surface, shape = CircleShape
                )
            ) {
                Badge(
                    modifier = Modifier.size(12.dp).padding(2.dp), containerColor = MaterialTheme.colorScheme.primary
                )
            }
        }, modifier = Modifier.padding(8.dp)
    ) {
        AppIcon(info = info, grayscale = contentDisabled, modifier = Modifier.size(48.dp))
    }
    Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val showFrozenLabel = frozen && !HailData.grayscaleIcon
            if (showFrozenLabel) {
                Label(
                    id = when {
                        info.hidden -> R.string.state_hidden
                        info.enabled.not() -> R.string.state_disabled
                        else -> R.string.state_suspended
                    }
                )
                Spacer(modifier = Modifier.width(2.dp))
            }
            Text(
                text = info.labelString, style = MaterialTheme.typography.titleSmall, color = when {
                    info.uninstalled -> MaterialTheme.colorScheme.error
                    contentDisabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    else -> MaterialTheme.colorScheme.onSurface
                }, overflow = TextOverflow.MiddleEllipsis, maxLines = if (showFrozenLabel) 1 else Int.MAX_VALUE
            )
        }
        if (info.labelString != info.packageName) Text(
            text = info.packageName,
            style = MaterialTheme.typography.bodySmall,
            color = if (contentDisabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    VerticalDivider(modifier = Modifier.height(32.dp).padding(horizontal = 8.dp))
    Checkbox(checked = isChecked, onCheckedChange = onCheckedChange, modifier = Modifier.padding(end = 8.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDialog(
    model: AppsViewModel, onDismiss: () -> Unit, filterState: String, onFilterStateChanged: (String) -> Unit
) = ModalBottomSheet(
    onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var filterType by rememberPreferenceState(HailData.FILTER_TYPE, HailData.filterType)
    var sortOrder by rememberPreferenceState(HailData.SORT_ORDER, HailData.sortOrder)
    val resources = LocalResources.current
    Column(
        modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = stringResource(R.string.sort_order))
        SegmentedPreference(value = sortOrder, values = HailData.SORT_ORDER_VALUES, onValueChange = {
            sortOrder = it
            model.updateDisplayAppList()
        }, entriesId = R.array.sort_order_entries)
        Text(text = stringResource(R.string.filter_title))
        SegmentedPreference(
            value = filterType, values = HailData.FILTER_TYPE_VALUES, onValueChange = {
                filterType = it
                model.updateDisplayAppList()
            }, entriesId = R.array.filter_type_entries
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            resources.getStringArray(R.array.filter_state_entries).forEachIndexed { index, text ->
                val value = HailData.FILTER_STATE_VALUES[index]
                FilterChip(selected = filterState == value, onClick = {
                    when {
                        filterState != value -> {
                            onFilterStateChanged(value)
                            model.updateDisplayAppList()
                        }

                        filterState != HailData.ACTION_NONE -> {
                            onFilterStateChanged(HailData.ACTION_NONE)
                            model.updateDisplayAppList()
                        }
                    }
                    model.updateDisplayAppList()
                }, label = { Text(text = text) }, leadingIcon = {
                    if (filterState == value) Icon(
                        imageVector = if (filterState != HailData.ACTION_NONE) Icons.Outlined.Done else Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.msg_selected, text),
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                })
            }
        }
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.freeze_system_app),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(36.dp)) // + 8dp arrangementSpace = 44dp dragHandleContainerHeight
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionsDialog(
    model: AppsViewModel, info: ApplicationInfo, onDismiss: () -> Unit, onExtract: () -> Unit, onUninstall: () -> Unit
) = ModalBottomSheet(
    onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    Column {
        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            itemVerticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = info.labelString,
                modifier = Modifier.padding(vertical = 4.dp),
                style = MaterialTheme.typography.headlineSmall
            )
            if (info.stopped.not()) Label(id = R.string.state_running)
            if (HTarget.N && info.suspended) Label(id = R.string.state_suspended)
            if (info.enabled.not()) Label(id = R.string.state_disabled)
            if (info.hidden) Label(id = R.string.state_hidden)
            if (info.uninstalled) Label(id = R.string.state_uninstalled)
        }
        ClickableItem(icon = Icons.Outlined.Info, titleId = R.string.action_details) {
            onDismiss()
            HUI.startActivity(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, HPackages.packageUri(info.packageName))
        }
        ClickableItem(icon = Icons.Outlined.ContentCopy, titleId = R.string.action_export_clipboard) {
            onDismiss()
            HUI.copyText(info.packageName)
            HUI.showToast(R.string.msg_text_copied, info.packageName)
        }
        ClickableItem(icon = Icons.Outlined.Unarchive, titleId = R.string.action_extract_apk, onClick = onExtract)
        if (info.uninstalled) ClickableItem(
            icon = Icons.Outlined.RestoreFromTrash, titleId = R.string.action_reinstall
        ) {
            onDismiss()
            if (AppManager.reinstallApp(info.packageName)) model.updateAppList()
            else HUI.showToast(R.string.operation_failed, info.labelString)
        } else ClickableItem(
            icon = Icons.Outlined.Delete, titleId = R.string.action_uninstall
        ) {
            if (HailData.workingMode != HailData.MODE_DEFAULT) onUninstall()
            else {
                onDismiss()
                AppManager.uninstallApp(info.packageName)
            }
        }
    }
}

@Composable
private fun ExtractingIndicator(info: ApplicationInfo, onDismiss: () -> Unit) {
    var isExtracting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = CreateDocument("application/vnd.android.package-archive")
    ) { uri ->
        if (uri == null) onDismiss()
        else scope.launch {
            isExtracting = true
            runCatching {
                withContext(Dispatchers.IO) {
                    FileInputStream(info.sourceDir).use { source ->
                        context.contentResolver.openOutputStream(uri, "rwt").use { target ->
                            if (target == null) return@withContext
                            HFiles.copy(source, target)
                        }
                    }
                }
            }.onSuccess {
                HUI.showToast(R.string.msg_extract_apk, uri.toString())
            }.onFailure {
                HUI.showToast(R.string.operation_failed, it.localizedMessage ?: "Unknown", true)
            }
            onDismiss()
        }
    }
    LaunchedEffect(launcher) {
        launcher.launch(HPackages.getUnhiddenPackageInfoOrNull(info.packageName)?.exportFileName ?: info.packageName)
    }
    if (isExtracting) Dialog(onDismissRequest = { }) {
        Card { CircularProgressIndicator(modifier = Modifier.padding(16.dp)) }
    }
}

@Composable
private fun UninstallDialog(model: AppsViewModel, info: ApplicationInfo, onDismiss: () -> Unit) = AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
        Icon(
            Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error
        )
    },
    title = { Text(text = info.labelString) },
    text = { Text(text = stringResource(R.string.msg_uninstall)) },
    confirmButton = {
        Button(
            onClick = {
                onDismiss()
                if (AppManager.uninstallApp(info.packageName)) model.updateAppList()
            }, colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError
            )
        ) { Text(stringResource(R.string.action_uninstall)) }
    },
    dismissButton = { OutlinedButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } })
