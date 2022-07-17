package com.aistra.hail.ui.apps

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
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
import com.aistra.hail.HailApp
import com.aistra.hail.R
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.HLog
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.NameComparator
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
    private val cf by lazy { ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) }) }
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
                    || it.applicationInfo.loadLabel(pm).toString().contains(query, true))
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
            setOnLongClickListener { onItemLongClickListener.onItemLongClick(pkg) }
            findViewById<ImageView>(R.id.app_icon).run {
                setImageBitmap(HailApp.iconLoader.loadIcon(app))
                colorFilter = if (HailData.grayscaleIcon && frozen) cf else null
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

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    interface OnItemClickListener {
        fun onItemClick(info: PackageInfo)
    }

    interface OnItemLongClickListener {
        fun onItemLongClick(packageName: String): Boolean
    }

    interface OnItemCheckedChangeListener {
        fun onItemCheckedChange(buttonView: CompoundButton, isChecked: Boolean, packageName: String)
    }
}