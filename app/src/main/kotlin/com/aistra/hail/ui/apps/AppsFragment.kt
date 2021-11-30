package com.aistra.hail.ui.apps

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
import com.google.android.material.snackbar.Snackbar
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
            setOnRefreshListener { AppsAdapter.refreshList(this) }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AppsAdapter.refreshList(refreshLayout)
    }

    override fun onItemClick(position: Int) {
        with(AppsAdapter.list[position]) {
            val name = applicationInfo.loadLabel(app.packageManager)
            MaterialAlertDialogBuilder(activity).setTitle(name)
                .setItems(R.array.apps_action_entries) { _, which ->
                    when (which) {
                        0 -> HUI.startActivity(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            HPackages.packageUri(packageName)
                        )
                        1 -> {
                            val target = "${HFiles.DIR_OUTPUT}/$packageName-$versionName-${
                                PackageInfoCompat.getLongVersionCode(this)
                            }.apk"
                            Snackbar.make(
                                activity.fab, getString(
                                    if (HFiles.copy(applicationInfo.sourceDir, target))
                                        R.string.msg_extract_apk
                                    else R.string.operation_failed,
                                    target
                                ), Snackbar.LENGTH_LONG
                            ).show()
                        }
                        2 -> {
                            if (packageName == app.packageName && HPolicy.isDeviceOwnerActive) {
                                MaterialAlertDialogBuilder(activity).setTitle(R.string.title_remove_do)
                                    .setMessage(R.string.msg_remove_do)
                                    .setPositiveButton(android.R.string.ok) { _, _ ->
                                        HPolicy.setOrganizationName()
                                        HPolicy.clearDeviceOwnerApp()
                                    }
                                    .setNegativeButton(android.R.string.cancel, null)
                                    .create().show()
                            } else if (HailData.workingMode == HailData.MODE_DEFAULT) {
                                AppManager.uninstallApp(packageName)
                            } else {
                                MaterialAlertDialogBuilder(activity).setTitle(name)
                                    .setMessage(R.string.msg_uninstall)
                                    .setPositiveButton(android.R.string.ok) { _, _ ->
                                        AppManager.uninstallApp(packageName)
                                    }
                                    .setNegativeButton(android.R.string.cancel, null)
                                    .create().show()
                            }
                        }
                    }
                }.create().show()
        }
    }

    override fun onItemLongClick(position: Int): Boolean =
        AppsAdapter.list[position].packageName.let {
            HUI.copyText(it)
            HUI.showToast(getString(R.string.msg_text_copied, it))
            true
        }

    override fun onItemCheckedChange(
        buttonView: CompoundButton, isChecked: Boolean, position: Int
    ) {
        AppsAdapter.list[position].packageName.let {
            if (isChecked) HailData.addCheckedApp(it)
            else HailData.removeCheckedApp(it)
            buttonView.isChecked = HailData.isChecked(it)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_apps, menu)
        (menu.findItem(R.id.action_search).actionView as SearchView).setOnQueryTextListener(object :
            SearchView.OnQueryTextListener {
            private var once = false
            override fun onQueryTextChange(newText: String): Boolean {
                if (once) AppsAdapter.refreshList(refreshLayout, newText) else once = true
                refreshLayout.isEnabled = newText.isNullOrEmpty()
                return true
            }

            override fun onQueryTextSubmit(query: String): Boolean = true
        })
    }
}