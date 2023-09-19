package com.aistra.hail.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.aistra.hail.R
import com.aistra.hail.app.HailData
import com.aistra.hail.databinding.ActivityMainBinding
import com.aistra.hail.utils.HPolicy
import com.aistra.hail.utils.HUI
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import rikka.insets.WindowInsetsHelper
import rikka.layoutinflater.view.LayoutInflaterFactory

class MainActivity : AppCompatActivity(), NavController.OnDestinationChangedListener {
    lateinit var fab: ExtendedFloatingActionButton
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        layoutInflater.factory2 = LayoutInflaterFactory(delegate)
            .addOnViewCreatedListener(WindowInsetsHelper.LISTENER)
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
        this@MainActivity.fab = fab

        navController = findNavController(R.id.nav_host_fragment)
        navController.addOnDestinationChangedListener(this@MainActivity)

        bottomNav?.setupWithNavController(navController)
        navRail?.setupWithNavController(navController)

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

    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {
        fab.tag = destination.id == R.id.nav_home
        if (fab.tag == true) fab.show() else fab.hide()
    }
}