package com.aistra.hail.ui.home

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.aistra.hail.R
import com.aistra.hail.app.HailData
import com.aistra.hail.ui.components.AppDestinations.*
import com.aistra.hail.ui.components.navigateTo
import com.aistra.hail.ui.main.MainActivityLegacy
import com.aistra.hail.utils.HPolicy
import me.zhanghai.compose.preference.rememberPreferenceState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    var expanded by remember { mutableStateOf(false) }
    var addPinShortcut by rememberSaveable { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val showNavigationBar by rememberPreferenceState(HailData.SHOW_NAVIGATION_BAR, HailData.showNavigationBar)
    if (addPinShortcut) TODO()
    Scaffold(modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection), topBar = {
        TopAppBar(
            title = { Text(text = stringResource(R.string.app_name)) }, navigationIcon = {
                if (!showNavigationBar) IconButton(onClick = {
                    navController.navigateTo(APPS)
                }) {
                    Icon(
                        imageVector = APPS.icon, contentDescription = stringResource(R.string.title_apps)
                    )
                }
            }, actions = {
                IconButton(onClick = { TODO() }) {
                    Icon(
                        imageVector = Icons.Outlined.Search, contentDescription = stringResource(R.string.action_search)
                    )
                }
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(androidx.appcompat.R.string.abc_toolbar_collapse_description)
                    )
                }
                DropdownMenu(
                    expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.action_add_pin_shortcut)) },
                        onClick = {
                            expanded = false
                            addPinShortcut = true
                        })
                    if (HPolicy.isDeviceOwnerActive || HPolicy.isProfileOwner || HPolicy.isAdminActive) {
                        DropdownMenuItem(
                            text = { Text(text = stringResource(R.string.title_remove_owner)) },
                            onClick = {
                                expanded = false
                                when {
                                    HPolicy.isDeviceOwnerActive -> TODO() // activity.ownerRemoveDialog()
                                    HPolicy.isProfileOwner -> HPolicy.removeProfileOwner()
                                    HPolicy.isAdminActive -> HPolicy.removeActiveAdmin()
                                }
                            })
                    }
                    if (!showNavigationBar) {
                        DropdownMenuItem(text = { Text(text = stringResource(R.string.title_settings)) }, onClick = {
                            expanded = false
                            navController.navigateTo(SETTINGS)
                        })
                        DropdownMenuItem(text = { Text(text = stringResource(R.string.title_about)) }, onClick = {
                            expanded = false
                            navController.navigateTo(ABOUT)
                        })
                    }
                }
            }, scrollBehavior = scrollBehavior
        )
    }) { contentPadding ->
        HomeContent(
            modifier = Modifier.fillMaxSize().padding(contentPadding)
        )
    }
}

@Composable
fun HomeContent(modifier: Modifier) = Column(
    modifier = modifier, verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
) {
    Text(text = "New UI is working in progress...")
    val context = LocalContext.current
    Button(onClick = {
        context.startActivity(Intent(context, MainActivityLegacy::class.java))
    }) {
        Text(text = "Legacy UI")
    }
}
