package com.aistra.hail.ui.whitelist

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
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.AppIconCache
import com.aistra.hail.utils.HLog
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.NameComparator
import kotlinx.coroutines.Job
import java.util.*

object WhitelistAdapter : ListAdapter<AppInfo, WhitelistAdapter.ViewHolder>(
    object : DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean =
            oldItem.packageName == newItem.packageName

        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean =
            areItemsTheSame(oldItem, newItem)
    }) {
    lateinit var onItemCheckedChangeListener: OnItemCheckedChangeListener
    private var loadIconJob: Job? = null
    private val timer = Timer()
    private var debounce: TimerTask? = null

    private fun filterList(query: String? = null, pm: PackageManager): List<AppInfo> =
        HailData.checkedList.filter {
            (query.isNullOrEmpty()
                    || it.packageName.contains(query, true)
                    || it.applicationInfo?.loadLabel(pm).toString().contains(query, true))
        }.run {
            when (HailData.sortBy) {
                HailData.SORT_INSTALL -> sortedBy {
                    HPackages.getPackageInfoOrNull(it.packageName)?.firstInstallTime
                }
                HailData.SORT_UPDATE -> sortedByDescending {
                    HPackages.getPackageInfoOrNull(it.packageName)?.lastUpdateTime
                }
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
        holder.itemView.run {
            findViewById<ImageView>(R.id.app_icon).run {
                loadIconJob = app?.let {
                    AppIconCache.loadIconBitmapAsync(
                        context,
                        it,
                        app.uid / 100000,
                        this
                    )
                }
            }
            findViewById<TextView>(R.id.app_name).run {
                text = app?.loadLabel(context.packageManager)
            }
            findViewById<TextView>(R.id.app_desc).run {
                text = pkg
            }
            findViewById<CompoundButton>(R.id.app_star).run {
                setOnCheckedChangeListener(null)
                isChecked = info.whitelisted
                    setOnCheckedChangeListener { button, isWhitelisted ->
                        onItemCheckedChangeListener.onItemCheckedChange(button, isWhitelisted, info)
                    }
            }
        }
    }

    fun onDestroy() {
        if (loadIconJob?.isActive == true) loadIconJob?.cancel()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    interface OnItemCheckedChangeListener {
        fun onItemCheckedChange(buttonView: CompoundButton, isWhitelisted: Boolean, appInfo: AppInfo)
    }
}
