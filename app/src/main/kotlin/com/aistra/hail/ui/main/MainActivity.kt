package com.aistra.hail.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.aistra.hail.R
import com.aistra.hail.databinding.ActivityMainBinding
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

class MainActivity : AppCompatActivity(), NavController.OnDestinationChangedListener {
    lateinit var fab: ExtendedFloatingActionButton
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                setOf(R.id.nav_home, R.id.nav_apps, R.id.nav_settings), drawerLayout
            )
            setupActionBarWithNavController(navController, appBarConfiguration)
            navView.setupWithNavController(navController)
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
}