package com.aistra.hail.ui.apps

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.*
import kotlinx.coroutines.Job
import net.sourceforge.pinyin4j.PinyinHelper
import java.util.*

object AppsAdapter : ListAdapter<PackageInfo, AppsAdapter.ViewHolder>(
    object : DiffUtil.ItemCallback<PackageInfo>() {
        override fun areItemsTheSame(oldItem: PackageInfo, newItem: PackageInfo): Boolean =
            oldItem.packageName == newItem.packageName

        override fun areContentsTheSame(oldItem: PackageInfo, newItem: PackageInfo): Boolean =
            areItemsTheSame(oldItem, newItem)
    }) {
    lateinit var onItemClickListener: OnItemClickListener
    lateinit var onItemLongClickListener: OnItemLongClickListener
    lateinit var onItemCheckedChangeListener: OnItemCheckedChangeListener
    private var loadIconJob: Job? = null
    private val timer = Timer()
    private var debounce: TimerTask? = null

    private val PackageInfo.isSystemApp: Boolean
        get() = applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == ApplicationInfo.FLAG_SYSTEM

    private fun filterList(query: String? = null, pm: PackageManager): List<PackageInfo> =
        HPackages.getInstalledPackages().filter {
            ((HailData.filterUserApps && !it.isSystemApp)
                    || (HailData.filterSystemApps && it.isSystemApp))
                    && ((HailData.filterFrozenApps && AppManager.isAppFrozen(it.packageName))
                    || (HailData.filterUnfrozenApps && !AppManager.isAppFrozen(it.packageName)))
                    && (query.isNullOrEmpty()
                    || it.packageName.contains(query, true)
                    || it.applicationInfo.loadLabel(pm).toString().contains(query, true)
                    || PinyinSearch.searchCap(it.applicationInfo.loadLabel(pm).toString(), query)
                    || PinyinSearch.searchAllSpell(it.applicationInfo.loadLabel(pm).toString(), query)
                    )
        }.run {
            when (HailData.sortBy) {
                HailData.SORT_INSTALL -> sortedBy { it.firstInstallTime }
                HailData.SORT_UPDATE -> sortedByDescending { it.lastUpdateTime }
                else -> sortedWith(NameComparator)
            }
        }

    fun updateCurrentList(layout: SwipeRefreshLayout, query: String? = null) {
        layout.isRefreshing = true
        debounce?.cancel()
        debounce = object : TimerTask() {
            override fun run() {
                val ms = SystemClock.elapsedRealtime()
                val list = filterList(query, layout.context.packageManager)
                HLog.i("Filter ${list.size} apps in ${SystemClock.elapsedRealtime() - ms}ms")
                layout.post {
                    submitList(list)
                    layout.isRefreshing = false
                }
            }
        }
        timer.schedule(debounce, 100)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_apps, parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val info = currentList[position]
        val app = info.applicationInfo
        val pkg = info.packageName
        val frozen = AppManager.isAppFrozen(pkg)
        holder.itemView.run {
            setOnClickListener { onItemClickListener.onItemClick(info) }
            setOnLongClickListener { onItemLongClickListener.onItemLongClick(AppInfo(pkg,true,0)) }
            findViewById<ImageView>(R.id.app_icon).run {
                loadIconJob = AppIconCache.loadIconBitmapAsync(
                    context,
                    app,
                    app.uid / 100000,
                    this,
                    HailData.grayscaleIcon && frozen
                )
            }
            findViewById<TextView>(R.id.app_name).run {
                text = app.loadLabel(context.packageManager)
                isEnabled = !frozen
            }
            findViewById<TextView>(R.id.app_desc).run {
                text = pkg
                isEnabled = !frozen
            }
            findViewById<CompoundButton>(R.id.app_star).run {
                setOnCheckedChangeListener(null)
                isChecked = HailData.isChecked(pkg)
                setOnCheckedChangeListener { button, isChecked ->
                    onItemCheckedChangeListener.onItemCheckedChange(button, isChecked, pkg)
                }
            }
        }
    }

    fun onDestroy() {
        if (loadIconJob?.isActive == true) loadIconJob?.cancel()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    interface OnItemClickListener {
        fun onItemClick(info: PackageInfo)
    }

    interface OnItemLongClickListener {
        fun onItemLongClick(info: AppInfo): Boolean
    }

    interface OnItemCheckedChangeListener {
        fun onItemCheckedChange(buttonView: CompoundButton, isChecked: Boolean, packageName: String)
    }
}