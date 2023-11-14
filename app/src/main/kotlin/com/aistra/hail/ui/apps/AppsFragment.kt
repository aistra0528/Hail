package com.aistra.hail.ui.apps

import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.CompoundButton
import androidx.appcompat.widget.SearchView
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.R
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.databinding.FragmentAppsBinding
import com.aistra.hail.extensions.applyInsetsPadding
import com.aistra.hail.extensions.isLandscape
import com.aistra.hail.ui.main.MainFragment
import com.aistra.hail.utils.HFiles
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HPolicy
import com.aistra.hail.utils.HUI
import com.aistra.hail.views.HRecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class AppsFragment : MainFragment(), AppsAdapter.OnItemClickListener, AppsAdapter.OnItemCheckedChangeListener,
    MenuProvider {

    private val model: AppsViewModel by viewModels()

    private var _binding: FragmentAppsBinding? = null
    private val binding get() = _binding!!
    private lateinit var appsAdapter: AppsAdapter

    // Prevent the same data from being filtered twice in `onCreateView`
    private var lastAppsHash: Int = 0
    private lateinit var lastQuery: String
    private val isAppsChanged get() = model.apps.value.hashCode() != lastAppsHash
    private val isQueryChanged get() = model.query.value != lastQuery
    private var contextMenuInfo: ContextMenu.ContextMenuInfo? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val menuHost = requireActivity() as MenuHost
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
        _binding = FragmentAppsBinding.inflate(inflater, container, false)
        appsAdapter = AppsAdapter().apply {
            onItemClickListener = this@AppsFragment
            onItemCheckedChangeListener = this@AppsFragment
        }
        binding.refresh.setOnRefreshListener { updateAppList() }
        binding.recyclerView.apply {
            activity.appbar.setLiftOnScrollTargetView(this)
            layoutManager = GridLayoutManager(activity, resources.getInteger(R.integer.apps_span))
            adapter = appsAdapter
            applyInsetsPadding(
                start = !activity.isLandscape, end = true, bottom = activity.isLandscape
            )
            registerForContextMenu(this)
        }

        model.isRefreshing.observe(viewLifecycleOwner) {
            binding.refresh.isRefreshing = it
        }
        model.apps.apply {
            lastAppsHash = value.hashCode()
            observe(viewLifecycleOwner) {
                if (isAppsChanged) updateDisplayAppList()
                lastAppsHash = it.hashCode()
            }
        }
        model.query.apply {
            lastQuery = value ?: ""
            observe(viewLifecycleOwner) {
                if (isQueryChanged) updateDisplayAppList()
                lastQuery = it
            }
        }
        model.displayApps.observe(viewLifecycleOwner) {
            appsAdapter.submitList(it)
        }

        return binding.root
    }

    override fun onItemClick(buttonView: CompoundButton) {
        buttonView.toggle()
    }

    override fun onCreateContextMenu(
        menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        contextMenuInfo = menuInfo
        val viewHolder = ((menuInfo as HRecyclerView.RecyclerViewContextMenuInfo).viewHolder as AppsAdapter.ViewHolder)
        menu.setHeaderTitle(viewHolder.info.loadLabel(activity.packageManager))
        activity.menuInflater.inflate(R.menu.menu_apps_action, menu)
        super.onCreateContextMenu(menu, v, menuInfo)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val viewHolder =
            ((contextMenuInfo as HRecyclerView.RecyclerViewContextMenuInfo).viewHolder as AppsAdapter.ViewHolder)
        val info = viewHolder.info
        val name = info.loadLabel(app.packageManager)
        val pkg = info.packageName
        when (item.itemId) {
            R.id.action_details -> HUI.startActivity(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS, HPackages.packageUri(pkg)
            )

            R.id.action_export_clipboard -> {
                HUI.copyText(pkg)
                HUI.showToast(R.string.msg_text_copied, pkg)
            }

            R.id.action_extract_apk -> extractApk(name, pkg, info.sourceDir)
            R.id.action_uninstall -> uninstallApp(name, pkg)
            else -> return super.onContextItemSelected(item)
        }
        return true
    }

    private fun extractApk(name: CharSequence, pkg: String, sourceDir: String) {
        lifecycleScope.launch {
            val dialog =
                MaterialAlertDialogBuilder(activity).setView(R.layout.dialog_progress).setCancelable(false).create()
            dialog.show()
            val target = "${HFiles.DIR_OUTPUT}/$name-${
                HPackages.getUnhiddenPackageInfoOrNull(pkg)?.versionName ?: "unknown"
            }-${
                HPackages.getUnhiddenPackageInfoOrNull(pkg)?.let { PackageInfoCompat.getLongVersionCode(it) } ?: 0
            }.apk"
            HUI.showToast(
                if (HFiles.copy(sourceDir, target)) R.string.msg_extract_apk
                else R.string.operation_failed, target, true
            )
            dialog.dismiss()
        }
    }

    private fun uninstallApp(name: CharSequence, pkg: String) {
        when {
            pkg == app.packageName -> {
                when {
                    HPolicy.isDeviceOwnerActive -> activity.ownerRemoveDialog()
                    HPolicy.isAdminActive -> {
                        HPolicy.removeActiveAdmin()
                        showUninstallDialog(name, pkg)
                    }

                    else -> showUninstallDialog(name, pkg)
                }
            }

            HailData.workingMode == HailData.MODE_DEFAULT -> AppManager.uninstallApp(pkg)
            else -> showUninstallDialog(name, pkg)
        }
    }

    private fun showUninstallDialog(name: CharSequence, pkg: String) {
        MaterialAlertDialogBuilder(activity).setTitle(name).setMessage(R.string.msg_uninstall)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                if (AppManager.uninstallApp(pkg)) updateAppList()
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
        val searchView = menu.findItem(R.id.action_search).actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                model.postQuery(newText, if (newText.isEmpty()) 0L else 300L)
                return true
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                model.postQuery(query, 0L)
                return true
            }
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
        updateDisplayAppList()
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
        updateDisplayAppList()
    }

    private fun updateAppList() = model.updateAppList()
    private fun updateDisplayAppList() = model.updateDisplayAppList()

    override fun onDestroy() {
        appsAdapter.onDestroy()
        super.onDestroy()
        _binding = null
    }
}