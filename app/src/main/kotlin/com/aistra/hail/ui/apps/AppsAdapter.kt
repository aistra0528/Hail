package com.aistra.hail.ui.apps

import android.annotation.SuppressLint
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
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.aistra.hail.R
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.HLog
import com.aistra.hail.utils.NameComparator
import java.util.*

object AppsAdapter : RecyclerView.Adapter<AppsAdapter.ViewHolder>() {
    var list = listOf<PackageInfo>()
    lateinit var onItemClickListener: OnItemClickListener
    lateinit var onItemLongClickListener: OnItemLongClickListener
    lateinit var onItemCheckedChangeListener: OnItemCheckedChangeListener
    private val timer = Timer()
    private var debounce: TimerTask? = null

    @SuppressLint("InlinedApi")
    private fun updateList(query: String? = null, pm: PackageManager) {
        list = pm.getInstalledPackages(PackageManager.MATCH_UNINSTALLED_PACKAGES).filter {
            (HailData.showSystemApps || it.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != ApplicationInfo.FLAG_SYSTEM)
                    && (HailData.showUnfrozenApps || AppManager.isAppFrozen(it.packageName))
                    && (query.isNullOrEmpty()
                    || it.packageName.contains(query, true)
                    || it.applicationInfo.loadLabel(pm).toString().contains(query, true))
        }.sortedWith(NameComparator)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun refreshList(layout: SwipeRefreshLayout, query: String? = null) {
        layout.isRefreshing = true
        debounce?.cancel()
        debounce = object : TimerTask() {
            override fun run() {
                val ms = SystemClock.elapsedRealtime()
                updateList(query, layout.context.packageManager)
                HLog.i("Filter ${list.size} apps in ${SystemClock.elapsedRealtime() - ms}ms")
                layout.post {
                    notifyDataSetChanged()
                    layout.isRefreshing = false
                }
            }
        }
        timer.schedule(debounce, 1000)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_apps, parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.run {
            setOnClickListener { onItemClickListener.onItemClick(position) }
            setOnLongClickListener { onItemLongClickListener.onItemLongClick(position) }
            list[position].applicationInfo.run {
                findViewById<ImageView>(R.id.app_icon).setImageDrawable(loadIcon(context.packageManager))
                findViewById<TextView>(R.id.app_name).text = loadLabel(context.packageManager)
                findViewById<TextView>(R.id.app_desc).text = packageName
                findViewById<CompoundButton>(R.id.app_star).run {
                    setOnCheckedChangeListener(null)
                    isChecked = HailData.isChecked(packageName)
                    setOnCheckedChangeListener { button, isChecked ->
                        onItemCheckedChangeListener.onItemCheckedChange(button, isChecked, position)
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int = list.size

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    interface OnItemLongClickListener {
        fun onItemLongClick(position: Int): Boolean
    }

    interface OnItemCheckedChangeListener {
        fun onItemCheckedChange(buttonView: CompoundButton, isChecked: Boolean, position: Int)
    }
}