package com.aistra.hail.ui.home

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class HomeAdapter(fragment: HomeFragment, val fragments: MutableList<PagerFragment>) :
    FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment = fragments[position]
}