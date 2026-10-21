package com.aistra.hail.ui.main

import androidx.fragment.app.Fragment

abstract class MainFragment : Fragment() {
    protected val activity: MainActivityLegacy get() = requireActivity() as MainActivityLegacy
}