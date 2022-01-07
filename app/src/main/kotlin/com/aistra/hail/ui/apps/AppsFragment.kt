package com.aistra.hail.ui.apps

import android.content.pm.PackageInfo
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.CompoundButton
import androidx.appcompat.widget.SearchView
import androidx.core.content.pm.PackageInfoCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.aistra.hail.R
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.ui.main.MainFragment
import com.aistra.hail.utils.HFiles
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HPolicy
import com.aistra.hail.utils.HUI
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.*

class AppsFragment : MainFragment(), AppsAdapter.OnItemClickListener,
    AppsAdapter.OnItemLongClickListener, AppsAdapter.OnItemCheckedChangeListener {
    private lateinit var refreshLayout: SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
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
        MaterialAlertDialogBuilder(activity).setTitle(name)
            .setItems(R.array.apps_action_entries) { _, which ->
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

    override fun onItemLongClick(packageName: String): Boolean = true.also {
        HUI.copyText(packageName)
        HUI.showToast(R.string.msg_text_copied, packageName)
    }

    override fun onItemCheckedChange(
        buttonView: CompoundButton, isChecked: Boolean, packageName: String
    ) {
        if (isChecked) HailData.addCheckedApp(packageName)
        else HailData.removeCheckedApp(packageName)
        buttonView.isChecked = HailData.isChecked(packageName)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
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
}