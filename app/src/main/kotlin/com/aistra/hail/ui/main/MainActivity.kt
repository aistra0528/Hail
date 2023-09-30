package com.aistra.hail.ui.main

import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.aistra.hail.R
import com.aistra.hail.app.HailData
import com.aistra.hail.databinding.ActivityMainBinding
import com.aistra.hail.utils.HPolicy
import com.aistra.hail.utils.HUI
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

class MainActivity : AppCompatActivity(), NavController.OnDestinationChangedListener {
    lateinit var fab: ExtendedFloatingActionButton
    lateinit var appbar: AppBarLayout
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val binding = initView()
        if (!HailData.biometricLogin) {
            showGuide()
            return
        }
        binding.root.isVisible = false
        val biometricPrompt = BiometricPrompt(this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                private fun unlock() {
                    binding.root.isVisible = true
                    showGuide()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    HUI.showToast(errString)
                    if (errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS) unlock()
                    else finishAndRemoveTask()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    unlock()
                }
            })
        val promptInfo =
            BiometricPrompt.PromptInfo.Builder().setTitle(getString(R.string.action_biometric))
                .setSubtitle(getString(R.string.msg_biometric))
                .setNegativeButtonText(getString(android.R.string.cancel)).build()
        biometricPrompt.authenticate(promptInfo)
    }

    private fun initView() = ActivityMainBinding.inflate(layoutInflater).apply {
        setContentView(root)
        setSupportActionBar(appBarMain.toolbar)
        fab = appBarMain.fab
        appbar = appBarMain.appBarLayout
        navController = findNavController(R.id.nav_host_fragment)
        navController.addOnDestinationChangedListener(this@MainActivity)
        appBarConfiguration = AppBarConfiguration.Builder(
            R.id.nav_home, R.id.nav_apps, R.id.nav_settings, R.id.nav_about
        ).build()
        setupActionBarWithNavController(navController, appBarConfiguration)
        bottomNav?.setupWithNavController(navController)
        navRail?.setupWithNavController(navController)
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        ViewCompat.setOnApplyWindowInsetsListener(appBarMain.appBarLayout) { view, windowInsets ->
            val insets =
                windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.updatePadding(
                top = insets.top,
                right = insets.right,
                left = if (isLandscape) 0 else insets.left
            )
            windowInsets
        }

        /* ViewCompat.setOnApplyWindowInsetsListener(appBarMain.contentMain.root) { view, windowInsets ->
             *//* val insets =
                windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.updatePadding(right = insets.right, left = insets.left)
            windowInsets *//*
        WindowInsetsCompat.CONSUMED
        } */

        if (bottomNav != null) ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { view, windowInsets ->
            val insets =
                windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.updatePadding(
                left = insets.left, right = insets.right, bottom = insets.bottom
            )
            windowInsets
        }
    }

    private fun showGuide() {
        if (HailData.guideVersion == HailData.GUIDE_VERSION) return
        if (HailData.workingMode != HailData.MODE_DEFAULT) HailData.setGuideVersion()
        else MaterialAlertDialogBuilder(this).setMessage(R.string.msg_guide)
            .setPositiveButton(android.R.string.ok) { _, _ -> HailData.setGuideVersion() }
            .setOnDismissListener { HailData.setGuideVersion() }.show()
    }

    fun ownerRemoveDialog() {
        MaterialAlertDialogBuilder(this).setTitle(R.string.title_remove_owner)
            .setMessage(R.string.msg_remove_owner)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                HPolicy.setOrganizationName()
                HPolicy.clearDeviceOwnerApp()
            }.setNegativeButton(android.R.string.cancel, null).show()
    }

    override fun onStop() {
        super.onStop()
        if (HailData.biometricLogin) finishAndRemoveTask()
    }

    override fun onSupportNavigateUp(): Boolean =
        navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()


    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {
        fab.tag = destination.id == R.id.nav_home
        if (fab.tag == true) fab.show() else fab.hide()
    }
}