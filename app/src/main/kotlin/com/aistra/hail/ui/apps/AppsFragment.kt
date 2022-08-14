package com.aistra.hail.ui.apps

import android.content.pm.PackageInfo
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.CompoundButton
import androidx.appcompat.widget.SearchView
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailApi
import com.aistra.hail.app.HailData
import com.aistra.hail.ui.home.HomeAdapter
import com.aistra.hail.ui.main.MainFragment
import com.aistra.hail.utils.*
import com.aistra.hail.work.HWork
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class AppsFragment : MainFragment(), AppsAdapter.OnItemClickListener,
    AppsAdapter.OnItemLongClickListener, AppsAdapter.OnItemCheckedChangeListener, MenuProvider {
    private lateinit var refreshLayout: SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val menuHost = requireActivity() as MenuHost
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
        return SwipeRefreshLayout(activity).apply {
            refreshLayout = this
            addView(RecyclerView(activity).apply {
                layoutManager =
                    GridLayoutManager(activity, resources.getInteger(R.integer.apps_span))
                adapter = AppsAdapter.apply {
                    onItemClickListener = this@AppsFragment
                    onItemLongClickListener = this@AppsFragment
                    onItemCheckedChangeListener = this@AppsFragment
                }
            })
            setOnRefreshListener { AppsAdapter.updateCurrentList(this) }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AppsAdapter.updateCurrentList(refreshLayout)
    }

    override fun onItemClick(info: PackageInfo) {
        val name = info.applicationInfo.loadLabel(app.packageManager)
        val pkg = info.packageName
        val canUninstall = HPackages.canUninstall(pkg)
        MaterialAlertDialogBuilder(activity).setTitle(name)
            .setItems(resources.getStringArray(R.array.apps_action_entries).toMutableList().filter {
                it != getString(R.string.action_uninstall) || canUninstall
            }.toTypedArray()) { _, which ->
                when (which) {
                    0 -> HUI.startActivity(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        HPackages.packageUri(pkg)
                    )
                    1 -> {
                        val target = "${HFiles.DIR_OUTPUT}/$name-${info.versionName}-${
                            PackageInfoCompat.getLongVersionCode(info)
                        }.apk"
                        HUI.showToast(
                            if (HFiles.copy(info.applicationInfo.sourceDir, target))
                                R.string.msg_extract_apk
                            else R.string.operation_failed,
                            target, true
                        )
                    }
                    2 -> when {
                        pkg == app.packageName -> {
                            when {
                                HPolicy.isDeviceOwnerActive ->
                                    MaterialAlertDialogBuilder(activity).setTitle(R.string.title_remove_do)
                                        .setMessage(R.string.msg_remove_do)
                                        .setPositiveButton(android.R.string.ok) { _, _ ->
                                            HPolicy.setOrganizationName()
                                            HPolicy.clearDeviceOwnerApp()
                                            AppManager.uninstallApp(pkg)
                                        }
                                        .setNegativeButton(android.R.string.cancel, null)
                                        .create().show()
                                HPolicy.isAdminActive -> {
                                    HPolicy.removeActiveAdmin()
                                    AppManager.uninstallApp(pkg)
                                }
                                else -> AppManager.uninstallApp(pkg)
                            }
                        }
                        HailData.workingMode == HailData.MODE_DEFAULT ->
                            AppManager.uninstallApp(pkg)
                        else -> MaterialAlertDialogBuilder(activity).setTitle(name)
                            .setMessage(R.string.msg_uninstall)
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                AppManager.uninstallApp(pkg)
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .create().show()
                    }
                }
            }.create().show()
    }

    override fun onItemLongClick(info: AppInfo): Boolean {
        info.applicationInfo ?: return false
        val actions = resources.getStringArray(R.array.home_action_entries)
        val pkg = info.packageName
        val frozen = AppManager.isAppFrozen(pkg)
        val action = getString(if (frozen) R.string.action_unfreeze else R.string.action_freeze)
        MaterialAlertDialogBuilder(activity).setTitle(info.name).setItems(
            actions.toMutableList().filter {
                it == action
                        || (it == getString(R.string.action_copy_packagename))
            }.toTypedArray()
        ) { _, which ->
            when (which) {
                0 -> setListFrozen(!frozen, listOf(info))
                1 -> {
                    HUI.copyText(pkg)
                    HUI.showToast(R.string.msg_text_copied, pkg)
                }
            }
        }.create().show()
        return true
    }

    private fun setListFrozen(
        frozen: Boolean, list: List<AppInfo> = HailData.checkedList, updateList: Boolean = true
    ) {
        var i = 0
        var denied = false
        var name = String()
        list.forEach {
            when {
                AppManager.isAppFrozen(it.packageName) == frozen -> return@forEach
                AppManager.setAppFrozen(it.packageName, frozen) -> {
                    i++
                    name = it.name.toString()
                }
                it.packageName != app.packageName && it.applicationInfo != null -> denied = true
            }
        }
        when {
            denied && i == 0 -> HUI.showToast(R.string.permission_denied)
            i > 0 -> {
                if (updateList) AppsAdapter.updateCurrentList(refreshLayout)
                HUI.showToast(
                    if (frozen) R.string.msg_freeze else R.string.msg_unfreeze,
                    if (i > 1) i.toString() else name
                )
            }
        }
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
            private var once = false
            override fun onQueryTextChange(newText: String): Boolean {
                if (once) AppsAdapter.updateCurrentList(refreshLayout, newText) else once = true
                refreshLayout.isEnabled = newText.isNullOrEmpty()
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
        menu.findItem(R.id.filter_user_apps).isChecked = HailData.filterUserApps
        menu.findItem(R.id.filter_system_apps).isChecked = HailData.filterSystemApps
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
        item.isChecked = !item.isChecked
        HailData.changeAppsFilter(filter, item.isChecked)
        AppsAdapter.updateCurrentList(refreshLayout)
    }

    override fun onDestroy() {
        AppsAdapter.onDestroy()
        super.onDestroy()
    }
}