package com.aistra.hail.ui.main

import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController

abstract class MainFragment : Fragment() {
    protected val activity: MainActivity get() = requireActivity() as MainActivity

    protected fun setupToolbar(toolbar: Toolbar, state: Lifecycle.State) {
        toolbar.title = findNavController().currentDestination?.label
        if (this is MenuProvider) {
            toolbar.addMenuProvider(this, viewLifecycleOwner, state)
            onPrepareMenu(toolbar.menu)
        }
    }

    protected fun setupToolbar(toolbar: Toolbar) {
        toolbar.title = findNavController().currentDestination?.label
        if (this is MenuProvider) {
            toolbar.addMenuProvider(this, viewLifecycleOwner)
            onPrepareMenu(toolbar.menu)
        }
    }
}