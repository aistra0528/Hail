package com.aistra.hail.ui.apps

import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.appcompat.widget.SearchView
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.R
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.ui.main.MainFragment
import com.aistra.hail.utils.HFiles
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HPolicy
import com.aistra.hail.utils.HUI
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class AppsFragment : MainFragment(), AppsAdapter.OnItemClickListener,
    AppsAdapter.OnItemLongClickListener, AppsAdapter.OnItemCheckedChangeListener, MenuProvider {
    private lateinit var refreshLayout: SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val menuHost = requireActivity() as MenuHost
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
        return SwipeRefreshLayout(activity).apply {
            id = R.id.scrollView
            refreshLayout = this
            addView(RecyclerView(activity).apply {
                activity.appbar.setLiftOnScrollTargetView(this)

                layoutManager =
                    GridLayoutManager(activity, resources.getInteger(R.integer.apps_span))
                adapter = AppsAdapter.apply {
                    onItemClickListener = this@AppsFragment
                    onItemLongClickListener = this@AppsFragment
                    onItemCheckedChangeListener = this@AppsFragment
                }
                val isLandscape =
                    resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                ViewCompat.setOnApplyWindowInsetsListener(this) { v, windowInsets ->
                    val insets =
                        windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
                    v.updatePadding(
                        left = if (isLandscape) 0 else insets.left,
                        right = insets.right,
                        bottom = if (isLandscape) insets.bottom else 0
                    )
                    windowInsets
                }
            })
            setOnRefreshListener { AppsAdapter.updateCurrentList(this) }

        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AppsAdapter.updateCurrentList(refreshLayout)
    }

    override fun onItemClick(buttonView: CompoundButton) {
        buttonView.toggle()
    }

    override fun onItemLongClick(info: ApplicationInfo): Boolean = true.also {
        val name = info.loadLabel(app.packageManager)
        val pkg = info.packageName
        MaterialAlertDialogBuilder(activity).setTitle(name)
            .setItems(resources.getStringArray(R.array.apps_action_entries)) { _, which ->
                when (which) {
                    0 -> HUI.startActivity(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS, HPackages.packageUri(pkg)
                    )

                    1 -> {
                        HUI.copyText(pkg)
                        HUI.showToast(R.string.msg_text_copied, pkg)
                    }

                    2 -> lifecycleScope.launch {
                        val dialog =
                            MaterialAlertDialogBuilder(activity).setView(R.layout.dialog_progress)
                                .setCancelable(false).create()
                        dialog.show()
                        val target = "${HFiles.DIR_OUTPUT}/$name-${
                            HPackages.getUnhiddenPackageInfoOrNull(pkg)?.versionName ?: "unknown"
                        }-${
                            HPackages.getUnhiddenPackageInfoOrNull(pkg)
                                ?.let { PackageInfoCompat.getLongVersionCode(it) } ?: 0
                        }.apk"
                        HUI.showToast(
                            if (HFiles.copy(info.sourceDir, target)) R.string.msg_extract_apk
                            else R.string.operation_failed, target, true
                        )
                        dialog.dismiss()
                    }

                    3 -> when {
                        pkg == app.packageName -> {
                            when {
                                HPolicy.isDeviceOwnerActive -> activity.ownerRemoveDialog()
                                HPolicy.isAdminActive -> {
                                    HPolicy.removeActiveAdmin()
                                    uninstallDialog(name, pkg)
                                }

                                else -> uninstallDialog(name, pkg)
                            }
                        }

                        HailData.workingMode == HailData.MODE_DEFAULT -> AppManager.uninstallApp(pkg)
                        else -> uninstallDialog(name, pkg)
                    }
                }
            }.show()
    }

    private fun uninstallDialog(name: CharSequence, pkg: String) {
        MaterialAlertDialogBuilder(activity).setTitle(name).setMessage(R.string.msg_uninstall)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                if (AppManager.uninstallApp(pkg)) AppsAdapter.updateCurrentList(refreshLayout)
            }.setNegativeButton(android.R.string.cancel, null).show()
    }

    override fun onItemCheckedChange(
        buttonView: CompoundButton, isChecked: Boolean, packageName: String
    ) {
        if (isChecked) HailData.addCheckedApp(packageName)
        else HailData.removeCheckedApp(packageName)
        buttonView.isChecked = HailData.isChecked(packageName)
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_apps, menu)
        (menu.findItem(R.id.action_search).actionView as SearchView).setOnQueryTextListener(object :
            SearchView.OnQueryTextListener {
            private var inited = false
            override fun onQueryTextChange(newText: String): Boolean {
                if (inited) AppsAdapter.updateCurrentList(refreshLayout, newText) else inited = true
                return true
            }

            override fun onQueryTextSubmit(query: String): Boolean = true
        })
    }

    override fun onPrepareMenu(menu: Menu) {
        super.onPrepareMenu(menu)
        menu.findItem(
            when (HailData.sortBy) {
                HailData.SORT_INSTALL -> R.id.sort_by_install
                HailData.SORT_UPDATE -> R.id.sort_by_update
                else -> R.id.sort_by_name
            }
        ).isChecked = true
        menu.findItem(
            if (HailData.filterSystemApps) R.id.filter_system_apps else R.id.filter_user_apps
        ).isChecked = true
        menu.findItem(R.id.filter_frozen_apps).isChecked = HailData.filterFrozenApps
        menu.findItem(R.id.filter_unfrozen_apps).isChecked = HailData.filterUnfrozenApps
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.sort_by_name -> changeAppsSort(HailData.SORT_NAME, item)
            R.id.sort_by_install -> changeAppsSort(HailData.SORT_INSTALL, item)
            R.id.sort_by_update -> changeAppsSort(HailData.SORT_UPDATE, item)
            R.id.filter_user_apps -> changeAppsFilter(HailData.FILTER_USER_APPS, item)
            R.id.filter_system_apps -> changeAppsFilter(HailData.FILTER_SYSTEM_APPS, item)
            R.id.filter_frozen_apps -> changeAppsFilter(HailData.FILTER_FROZEN_APPS, item)
            R.id.filter_unfrozen_apps -> changeAppsFilter(HailData.FILTER_UNFROZEN_APPS, item)
        }
        return false
    }

    private fun changeAppsSort(sort: String, item: MenuItem) {
        item.isChecked = true
        HailData.changeAppsSort(sort)
        AppsAdapter.updateCurrentList(refreshLayout)
    }

    private fun changeAppsFilter(filter: String, item: MenuItem) {
        when (item.itemId) {
            R.id.filter_user_apps -> {
                item.isChecked = true
                HailData.changeAppsFilter(filter, item.isChecked)
                HailData.changeAppsFilter(HailData.FILTER_SYSTEM_APPS, false)
            }

            R.id.filter_system_apps -> {
                item.isChecked = true
                HailData.changeAppsFilter(filter, item.isChecked)
                HailData.changeAppsFilter(HailData.FILTER_USER_APPS, false)
            }

            else -> {
                item.isChecked = !item.isChecked
                HailData.changeAppsFilter(filter, item.isChecked)
            }
        }
        AppsAdapter.updateCurrentList(refreshLayout)
    }

    override fun onDestroy() {
        AppsAdapter.onDestroy()
        super.onDestroy()
    }
}