package com.aistra.hail.ui.apps

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.CompoundButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.aistra.hail.R
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HData
import com.aistra.hail.ui.main.MainFragment
import com.aistra.hail.utils.Files
import com.google.android.material.snackbar.Snackbar

class AppsFragment : MainFragment(), AppsAdapter.OnItemClickListener,
    AppsAdapter.OnItemCheckedChangeListener {
    private lateinit var refreshLayout: SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        return SwipeRefreshLayout(activity).apply {
            refreshLayout = this
            addView(RecyclerView(activity).apply {
                layoutManager = LinearLayoutManager(activity)
                adapter = AppsAdapter.apply {
                    onItemClickListener = this@AppsFragment
                    onItemCheckedChangeListener = this@AppsFragment
                }
            })
            setOnRefreshListener {
                AppsAdapter.run {
                    updateList(HData.showSystemApps, null)
                    notifyDataSetChanged()
                }
                isRefreshing = false
            }
        }
    }

    override fun onItemClick(position: Int) {
        AppsAdapter.list[position].run {
            AlertDialog.Builder(activity).run {
                val name = applicationInfo.loadLabel(app.packageManager)
                setTitle(name)
                setItems(R.array.apps_action_entries) { _, which ->
                    when (which) {
                        0 -> startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:$packageName")
                            )
                        )
                        1 -> {
                            val target = "/storage/emulated/0/Download/$packageName.apk"
                            Snackbar.make(
                                activity.nav, getString(
                                    if (Files.copy(applicationInfo.sourceDir, target))
                                        R.string.toast_extract_apk
                                    else
                                        R.string.operation_failed,
                                    target
                                ), Snackbar.LENGTH_LONG
                            ).setAnchorView(activity.nav).show()
                        }
                        2 -> {
                            if (packageName == app.packageName && AppManager.isDeviceOwnerApp) {
                                AlertDialog.Builder(activity).run {
                                    setTitle(R.string.title_remove_do)
                                    setMessage(R.string.msg_remove_do)
                                    setPositiveButton(android.R.string.ok) { _, _ ->
                                        AppManager.clearDeviceOwnerApp()
                                    }
                                    setNegativeButton(android.R.string.cancel, null)
                                    create().show()
                                }
                            } else if (HData.runningMode == HData.MODE_DEFAULT) {
                                AppManager.uninstallApp(packageName)
                            } else {
                                AlertDialog.Builder(activity).run {
                                    setTitle(name)
                                    setMessage(R.string.msg_uninstall)
                                    setPositiveButton(android.R.string.ok) { _, _ ->
                                        AppManager.uninstallApp(packageName)
                                    }
                                    setNegativeButton(android.R.string.cancel, null)
                                    create().show()
                                }
                            }
                        }
                    }
                }
                create().show()
            }
        }
    }

    override fun onItemCheckedChange(
        buttonView: CompoundButton,
        isChecked: Boolean,
        position: Int
    ) {
        AppsAdapter.list[position].run {
            if (isChecked)
                HData.addCheckedApp(packageName)
            else
                HData.removeCheckedApp(packageName)
            buttonView.isChecked = HData.isChecked(packageName)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_apps, menu)
        (menu.findItem(R.id.action_search).actionView as SearchView).run {
            setOnQueryTextFocusChangeListener { _, hasFocus ->
                refreshLayout.isEnabled = !hasFocus
                activity.nav.visibility = if (hasFocus) View.INVISIBLE else View.VISIBLE
            }
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextChange(newText: String?): Boolean {
                    AppsAdapter.run {
                        updateList(HData.showSystemApps, newText)
                        notifyDataSetChanged()
                    }
                    return true
                }

                override fun onQueryTextSubmit(query: String?): Boolean = false
            })
        }
    }
}