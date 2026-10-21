package com.aistra.hail.ui.settings

import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.navigation.NavController
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.R
import com.aistra.hail.app.HailData
import com.aistra.hail.app.HailData.showNavigationBar
import com.aistra.hail.app.HailData.toEntry
import com.aistra.hail.ui.components.AppDestinations.SETTINGS
import com.aistra.hail.ui.components.NavigateUpButton
import com.aistra.hail.utils.HShortcuts
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.ListPreferenceType
import me.zhanghai.compose.preference.SwitchPreference
import me.zhanghai.compose.preference.rememberPreferenceState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection), topBar = {
        TopAppBar(
            title = { Text(text = stringResource(SETTINGS.label)) },
            navigationIcon = { if (!showNavigationBar) NavigateUpButton(navController) },
            scrollBehavior = scrollBehavior
        )
    }) { contentPadding ->
        SettingsContent(modifier = Modifier.fillMaxSize().padding(contentPadding))
    }
}

@Composable
fun SettingsContent(modifier: Modifier) = LazyColumn(modifier = modifier) {
    switchPreference(
        key = HailData.BIOMETRIC_LOGIN, defaultValue = HailData.biometricLogin, onValueChange = { value ->
            if (value) resetDynamicShortcuts()
            true
        }, titleId = R.string.action_biometric, icon = Icons.Outlined.Fingerprint
    )
    listPreference(
        key = HailData.APP_THEME,
        defaultValue = HailData.appTheme,
        onValueChange = { value ->
            app.setAppTheme(value)
            true
        },
        values = HailData.APP_THEME_VALUES,
        entriesId = R.array.app_theme_entries,
        titleId = R.string.app_theme,
        icon = Icons.Outlined.DarkMode
    )
    switchPreference(
        key = HailData.GRAYSCALE_ICON,
        defaultValue = HailData.grayscaleIcon,
        titleId = R.string.grayscale_icon,
        icon = Icons.Outlined.FilterBAndW
    )
    switchPreference(
        key = HailData.SHOW_NAVIGATION_BAR,
        defaultValue = HailData.showNavigationBar,
        titleId = R.string.show_navigation_bar,
        icon = Icons.Outlined.Visibility
    )
    switchPreference(
        key = HailData.NINE_KEY_SEARCH,
        defaultValue = HailData.nineKeySearch,
        titleId = R.string.nine_key,
        icon = Icons.Outlined.Dialpad,
        summaryId = R.string.nine_key_description
    )
}

private fun LazyListScope.switchPreference(
    rememberState: @Composable () -> MutableState<Boolean>,
    onValueChange: (Boolean) -> Boolean = { true },
    @StringRes titleId: Int,
    enabled: Boolean = true,
    icon: ImageVector,
    @StringRes summaryId: Int? = null
) = item(key = titleId, contentType = "SwitchPreference") {
    var state by rememberState()
    SwitchPreference(
        value = state,
        onValueChange = { if (onValueChange(it)) state = it },
        title = { Text(text = stringResource(titleId)) },
        enabled = enabled,
        icon = { Icon(imageVector = icon, contentDescription = null) },
        summary = { if (summaryId != null) Text(text = stringResource(summaryId)) })
}

private fun LazyListScope.switchPreference(
    key: String,
    defaultValue: Boolean,
    onValueChange: (Boolean) -> Boolean = { true },
    @StringRes titleId: Int,
    enabled: Boolean = true,
    icon: ImageVector,
    @StringRes summaryId: Int? = null
) = switchPreference(
    rememberState = { rememberPreferenceState(key, defaultValue) },
    onValueChange = onValueChange,
    titleId = titleId,
    enabled = enabled,
    icon = icon,
    summaryId = summaryId
)

private fun LazyListScope.listPreference(
    key: String,
    defaultValue: String,
    onValueChange: (String) -> Boolean = { true },
    values: List<String>,
    @StringRes titleId: Int,
    icon: ImageVector,
    summary: @Composable (String) -> String,
    type: ListPreferenceType = ListPreferenceType.DROPDOWN_MENU,
    valueToText: (String) -> String
) = item(key = key, contentType = "ListPreference") {
    var state by rememberPreferenceState(key, defaultValue)
    ListPreference(
        value = state,
        onValueChange = { if (onValueChange(it)) state = it },
        values = values,
        title = { Text(text = stringResource(titleId)) },
        icon = { Icon(imageVector = icon, contentDescription = null) },
        summary = { Text(text = summary(state)) },
        type = type,
        valueToText = { AnnotatedString(valueToText(it)) })
}

private fun LazyListScope.listPreference(
    key: String,
    defaultValue: String,
    onValueChange: (String) -> Boolean = { true },
    values: List<String>,
    @ArrayRes entriesId: Int,
    @StringRes titleId: Int,
    icon: ImageVector,
    type: ListPreferenceType = ListPreferenceType.DROPDOWN_MENU
) = listPreference(
    key = key,
    defaultValue = defaultValue,
    onValueChange = onValueChange,
    values = values,
    titleId = titleId,
    icon = icon,
    summary = { it.toEntry(values, entriesId) },
    type = type,
    valueToText = { it.toEntry(values, entriesId) })

private fun resetDynamicShortcuts() {
    HShortcuts.removeAllDynamicShortcuts()
    HShortcuts.addDynamicShortcutAction(HailData.dynamicShortcutAction)
}
