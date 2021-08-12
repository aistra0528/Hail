package com.aistra.hail.ui.main

import android.os.Bundle
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.aistra.hail.R
import com.aistra.hail.databinding.ActivityMainBinding
import com.aistra.hail.ui.HailActivity
import com.aistra.hail.ui.apps.AppsFragment
import com.aistra.hail.ui.home.HomeFragment
import com.aistra.hail.ui.settings.SettingsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : HailActivity() {
    lateinit var nav: BottomNavigationView
    private val array by lazy {
        arrayOf(
            R.id.navigation_home to HomeFragment(),
            R.id.navigation_apps to AppsFragment(),
            R.id.navigation_settings to SettingsFragment()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
    }

    private fun initView() {
        ActivityMainBinding.inflate(layoutInflater).run {
            setContentView(root)
            setSupportActionBar(toolbar)
            viewpager.run {
                isUserInputEnabled = false
                adapter = object : FragmentStateAdapter(activity) {
                    override fun createFragment(position: Int): Fragment {
                        return array[position].second
                    }

                    override fun getItemCount(): Int {
                        return array.size
                    }
                }
                registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        super.onPageSelected(position)
                        navView.menu[position].isChecked = true
                    }
                })
            }
            nav = navView
            navView.setOnItemSelectedListener {
                viewpager.currentItem = with(array) {
                    for (i in 0 until size) {
                        if (it.itemId == get(i).first) return@with i
                    }
                    0
                }
                true
            }
        }
    }
}