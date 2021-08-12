package com.aistra.hail.ui.main

import android.content.pm.PackageManager
import androidx.fragment.app.Fragment
import com.aistra.hail.HailApp

abstract class MainFragment : Fragment() {
    protected val activity: MainActivity get() = requireActivity() as MainActivity
    protected val app: HailApp get() = HailApp.app
    protected val pm: PackageManager get() = app.packageManager
}