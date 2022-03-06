package com.aistra.hail.ui.main

import android.os.Bundle
import android.view.View
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
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
import com.aistra.hail.ui.HailActivity
import com.aistra.hail.utils.HUI
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

class MainActivity : HailActivity(), NavController.OnDestinationChangedListener {
    lateinit var fab: ExtendedFloatingActionButton
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        initView()
        if (HailData.biometricLogin.not()) return
        val background = findViewById<View>(R.id.toolbar).background.constantState?.newDrawable()?.mutate()
        val view = findViewById<View>(R.id.drawer_layout)
        view.visibility = View.INVISIBLE
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.action_biometric))
            .setSubtitle(getString(R.string.msg_biometric))
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()
        val biometricPrompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    HUI.showToast(errString)
                    finishAndRemoveTask()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    view.visibility = View.VISIBLE
                    findViewById<View>(R.id.toolbar).background = background
                }
            })
        biometricPrompt.authenticate(promptInfo)
    }

    private fun initView() {
        with(ActivityMainBinding.inflate(layoutInflater)) {
            setContentView(root)
            setSupportActionBar(appBarMain.toolbar)
            fab = appBarMain.fab
            val navController = findNavController(R.id.nav_host_fragment_content_main)
            navController.addOnDestinationChangedListener(this@MainActivity)
            appBarConfiguration = AppBarConfiguration(
                setOf(R.id.nav_home, R.id.nav_apps, R.id.nav_settings, R.id.nav_about)
            )
            setupActionBarWithNavController(navController, appBarConfiguration)
            bottomNav?.setupWithNavController(navController)
            navRail?.setupWithNavController(navController)
            ViewCompat.setOnApplyWindowInsetsListener(appBarMain.root) { view, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                val cutoutInsets = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout())
                view.updatePadding(top = insets.top, right = insets.right + cutoutInsets.right)
                windowInsets
            }
            if (bottomNav != null)
                ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { view, windowInsets ->
                    val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                    view.updatePadding(
                        left = insets.left,
                        right = insets.right,
                        bottom = insets.bottom
                    )
                    windowInsets
                }
        }
    }

    override fun onSupportNavigateUp(): Boolean =
        findNavController(R.id.nav_host_fragment_content_main).navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()


    override fun onDestinationChanged(
        controller: NavController, destination: NavDestination, arguments: Bundle?
    ) = fab.run {
        if (destination.id != R.id.nav_home) {
            setOnClickListener(null)
            hide()
        } else show()
    }
}