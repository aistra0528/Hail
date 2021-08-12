package com.aistra.hail.ui.apps

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.CompoundButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aistra.hail.R
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HData
import com.aistra.hail.app.HLog
import com.aistra.hail.ui.main.MainFragment
import com.aistra.hail.util.FilesCompat
import com.google.android.material.snackbar.Snackbar

class AppsFragment : MainFragment() {
    private lateinit var mAdapter: AppsAdapter
    private val mList: MutableList<PackageInfo> by lazy { pm.getInstalledPackages(PackageManager.MATCH_UNINSTALLED_PACKAGES) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        sortPackages()
        return RecyclerView(activity).apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = AppsAdapter(mList).apply {
                mAdapter = this
                setOnItemClickListener(object : AppsAdapter.OnItemClickListener {
                    override fun onItemClick(position: Int) {
                        this@AppsFragment.onItemClick(position)
                    }
                })
                setOnItemCheckedChangeListener(object : AppsAdapter.OnItemCheckedChangeListener {
                    override fun onItemCheckedChange(
                        buttonView: CompoundButton,
                        isChecked: Boolean,
                        position: Int
                    ) {
                        this@AppsFragment.onItemCheckedChange(buttonView, isChecked, position)
                    }
                })
            }
        }
    }

    private fun sortPackages() {
        val ms = System.currentTimeMillis()
        if (!HData.showSystemApps) {
            for (pkg in mList.toList()) {
                if (pkg.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == ApplicationInfo.FLAG_SYSTEM)
                    mList.remove(pkg)
            }
        }
        mList.sortByDescending { it.lastUpdateTime }
        HLog.i("Sort ${mList.size} apps in ${System.currentTimeMillis() - ms}ms")
    }

    private fun onItemClick(position: Int) {
        mList[position].run {
            AlertDialog.Builder(activity).run {
                setTitle(applicationInfo.loadLabel(pm))
                setItems(R.array.apps_action_entries) { _, which ->
                    when (which) {
                        0 -> startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:$packageName")
                            )
                        )
                        1 -> {
                            if (!FilesCompat.copy(
                                    applicationInfo.sourceDir,
                                    "/storage/emulated/0/Download/$packageName.apk"
                                )
                            ) {
                                Snackbar.make(
                                    activity.nav,
                                    R.string.operation_failed,
                                    Snackbar.LENGTH_SHORT
                                ).setAnchorView(R.id.nav_view).show()
                            }
                        }
                        2 -> {
                            if (packageName == app.packageName && AppManager.isDeviceOwnerApp) {
                                AlertDialog.Builder(activity).run {
                                    setTitle(R.string.title_remove_dpm)
                                    setMessage(R.string.msg_remove_dpm)
                                    setPositiveButton(android.R.string.ok) { _, _ ->
                                        AppManager.clearDeviceOwnerApp()
                                    }
                                    setNegativeButton(android.R.string.cancel, null)
                                    create().show()
                                }
                            } else {
                                startActivity(
                                    Intent(
                                        Intent.ACTION_DELETE,
                                        Uri.parse("package:$packageName")
                                    )
                                )
                            }
                        }
                    }
                }
                create().show()
            }
        }
    }

    private fun onItemCheckedChange(
        buttonView: CompoundButton,
        isChecked: Boolean,
        position: Int
    ) {
        mList[position].run {
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
        (menu.findItem(R.id.action_search).actionView as SearchView).apply {
            setOnQueryTextFocusChangeListener { _, hasFocus ->
                activity.nav.visibility = if (hasFocus) View.INVISIBLE else View.VISIBLE
            }
            setOnQueryTextListener(object :
                SearchView.OnQueryTextListener {
                override fun onQueryTextChange(str: String?): Boolean {
                    if (str.isNullOrBlank()) return false
                    Toast.makeText(activity, R.string.coming_soon, Toast.LENGTH_SHORT).show()
                    return true
                }

                override fun onQueryTextSubmit(query: String?): Boolean = false
            })
        }
    }
}