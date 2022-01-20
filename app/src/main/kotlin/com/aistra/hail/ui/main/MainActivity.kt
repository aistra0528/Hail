package com.aistra.hail.ui.main

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.*
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.aistra.hail.HailApp
import com.aistra.hail.R
import com.aistra.hail.app.HailData
import com.aistra.hail.databinding.ActivityMainBinding
import com.aistra.hail.services.AutoFreezeService
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

class MainActivity : AppCompatActivity(), NavController.OnDestinationChangedListener {
    lateinit var fab: ExtendedFloatingActionButton
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        initView()
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
            ViewCompat.setOnApplyWindowInsetsListener(root.getViewById(R.id.app_bar_main)) { view, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                val cutoutInsets = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout())
                view.updatePadding(top = insets.top, right = insets.right + cutoutInsets.right)
                windowInsets
            }
            if (bottomNav != null)
                ViewCompat.setOnApplyWindowInsetsListener(root.getViewById(R.id.bottom_nav)) { view, windowInsets ->
                    val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                    view.updatePadding(left = insets.left, right = insets.right, bottom = insets.bottom)
                    windowInsets
                }
        }
    }

    override fun onSupportNavigateUp(): Boolean =
        findNavController(R.id.nav_host_fragment_content_main).navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()


    override fun onDestinationChanged(
        controller: NavController, destination: NavDestination, arguments: Bundle?
    ) {
        if (destination.id == R.id.nav_home) fab.show()
        else fab.hide()
    }

    fun startAutoFreezeService() {
        if (HailData.autoFreezeAfterLock) {
            val intent = Intent(HailApp.app, AutoFreezeService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                applicationContext.startForegroundService(intent)
            else
                applicationContext.startService(intent)
        }
    }

    fun stopAutoFreezeService() {
        if (HailData.autoFreezeAfterLock) {
            val intent = Intent(HailApp.app, AutoFreezeService::class.java)
            applicationContext.stopService(intent)
        }
    }
}