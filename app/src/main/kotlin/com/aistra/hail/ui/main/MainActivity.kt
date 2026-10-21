package com.aistra.hail.ui.main

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.biometric.AuthenticationRequest
import androidx.biometric.AuthenticationRequest.Companion.biometricRequest
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.compose.rememberAuthenticationLauncher
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import com.aistra.hail.R
import com.aistra.hail.app.HailData
import com.aistra.hail.ui.about.AboutScreen
import com.aistra.hail.ui.apps.AppsScreen
import com.aistra.hail.ui.apps.AppsViewModel
import com.aistra.hail.ui.components.AppDestinations
import com.aistra.hail.ui.components.AppDestinations.*
import com.aistra.hail.ui.components.navigateTo
import com.aistra.hail.ui.home.HomeScreen
import com.aistra.hail.ui.settings.SettingsScreen
import com.aistra.hail.ui.theme.AppTheme
import com.aistra.hail.utils.HUI
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.rememberPreferenceState

class MainActivity : ComponentActivity() {
    private val appsViewModel by viewModels<AppsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Comment this line to make 3-button navigation bar transparent
        setContent {
            AppTheme {
                var authenticated by rememberSaveable {
                    mutableStateOf(
                        !HailData.biometricLogin || BiometricManager.from(this)
                            .canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) != BiometricManager.BIOMETRIC_SUCCESS
                    )
                }
                if (!authenticated) {
                    val launcher = rememberAuthenticationLauncher { authResult ->
                        if (authResult.isSuccess()) authenticated = true
                        else {
                            HUI.showToast(authResult.error()!!.errString)
                            finishAndRemoveTask()
                        }
                    }
                    LaunchedEffect(authenticated) {
                        launcher.launch(
                            biometricRequest(
                                title = getString(R.string.action_biometric),
                                AuthenticationRequest.Biometric.Fallback.DeviceCredential,
                            ) {
                                setSubtitle(getString(R.string.msg_biometric))
                            })
                    }
                } else ProvidePreferenceLocals { MainContent() }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        appsViewModel.updateAppList()
    }

    @SuppressLint("PrivateResource")
    @Composable
    private fun MainContent() {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = backStackEntry?.destination?.route
        val showNavigationBar by rememberPreferenceState(HailData.SHOW_NAVIGATION_BAR, HailData.showNavigationBar)
        if (showNavigationBar) {
            val layoutType = with(currentWindowAdaptiveInfo()) {
                if (windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND)) NavigationSuiteType.NavigationRail
                else NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(this)
            }
            NavigationSuiteScaffold(
                navigationSuiteItems = {
                    AppDestinations.entries.forEach {
                        val selected = currentDestination == it.route
                        item(
                            icon = {
                                Icon(
                                    imageVector = if (selected) it.iconSelected else it.icon, contentDescription = null
                                )
                            },
                            label = { Text(stringResource(it.label)) },
                            selected = selected,
                            onClick = { navController.navigateTo(it) })
                    }
                }, layoutType = layoutType
            ) {
                AppNavHost(navController = navController)
            }
        } else AppNavHost(navController = navController)
    }

    @Composable
    private fun AppNavHost(navController: NavHostController) = NavHost(
        navController = navController,
        startDestination = HOME.route,
        modifier = Modifier.fillMaxSize(),
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) },
        popEnterTransition = { fadeIn(animationSpec = tween(300)) },
        popExitTransition = { fadeOut(animationSpec = tween(300)) }) {
        composable(HOME.route) {
            HomeScreen(navController = navController)
        }
        composable(APPS.route) {
            AppsScreen(model = appsViewModel, navController = navController)
        }
        composable(SETTINGS.route) {
            SettingsScreen(navController = navController)
        }
        composable(ABOUT.route) {
            AboutScreen(navController = navController)
        }
    }
}
