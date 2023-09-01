package com.aistra.hail.ui.apps

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
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
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.AppIconCache
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.NameComparator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object AppsAdapter :
    ListAdapter<ApplicationInfo, AppsAdapter.ViewHolder>(object :
        DiffUtil.ItemCallback<ApplicationInfo>() {
        override fun areItemsTheSame(oldItem: ApplicationInfo, newItem: ApplicationInfo): Boolean =
            oldItem.packageName == newItem.packageName

        override fun areContentsTheSame(
            oldItem: ApplicationInfo,
            newItem: ApplicationInfo
        ): Boolean =
            areItemsTheSame(oldItem, newItem)
    }) {
    lateinit var onItemClickListener: OnItemClickListener
    lateinit var onItemLongClickListener: OnItemLongClickListener
    lateinit var onItemCheckedChangeListener: OnItemCheckedChangeListener
    private var loadIconJob: Job? = null
    private var refreshJob: Job? = null

    private val ApplicationInfo.isSystemApp: Boolean
        get() = flags and ApplicationInfo.FLAG_SYSTEM == ApplicationInfo.FLAG_SYSTEM

    private suspend fun filterList(
        query: String? = null,
        pm: PackageManager
    ): List<ApplicationInfo> =
        withContext(Dispatchers.Default) {
            HPackages.getInstalledApplications().filter {
                ((HailData.filterUserApps && !it.isSystemApp)
                        || (HailData.filterSystemApps && it.isSystemApp))

                        && ((HailData.filterFrozenApps && AppManager.isAppFrozen(it.packageName))
                        || (HailData.filterUnfrozenApps && !AppManager.isAppFrozen(it.packageName)))

                        && (query.isNullOrEmpty() || it.packageName.contains(query, true)
                        || it.loadLabel(pm).toString().contains(query, true))
            }.run {
                when (HailData.sortBy) {
                    HailData.SORT_INSTALL -> sortedBy {
                        HPackages.getUnhiddenPackageInfoOrNull(it.packageName)
                            ?.firstInstallTime ?: 0
                    }

                    HailData.SORT_UPDATE -> sortedByDescending {
                        HPackages.getUnhiddenPackageInfoOrNull(it.packageName)?.lastUpdateTime ?: 0
                    }

                    else -> sortedWith(NameComparator)
                }
            }
        }

    fun updateCurrentList(layout: SwipeRefreshLayout, query: String? = null) {
        refreshJob?.cancel()
        refreshJob = CoroutineScope(Dispatchers.Main).launch {
            if (query.isNullOrEmpty()) layout.isRefreshing = true else delay(500)
            val list = filterList(query, layout.context.packageManager)
            submitList(list)
            layout.isRefreshing = false
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_apps, parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val info = currentList[position]
        val pkg = info.packageName
        val frozen = AppManager.isAppFrozen(pkg)
        holder.itemView.run {
            val btn = findViewById<CompoundButton>(R.id.app_star)
            setOnClickListener { onItemClickListener.onItemClick(btn) }
            setOnLongClickListener { onItemLongClickListener.onItemLongClick(info) }
            findViewById<ImageView>(R.id.app_icon).run {
                loadIconJob = AppIconCache.loadIconBitmapAsync(
                    context, info, HPackages.myUserId, this, HailData.grayscaleIcon && frozen
                )
            }
            findViewById<TextView>(R.id.app_name).run {
                val name = info.loadLabel(context.packageManager)
                text = if (!HailData.grayscaleIcon && frozen) "❄️$name" else name
                isEnabled = !HailData.grayscaleIcon || !frozen
            }
            findViewById<TextView>(R.id.app_desc).run {
                text = pkg
                isEnabled = !HailData.grayscaleIcon || !frozen
            }
            btn.run {
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
        if (refreshJob?.isActive == true) refreshJob?.cancel()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    interface OnItemClickListener {
        fun onItemClick(buttonView: CompoundButton)
    }

    interface OnItemLongClickListener {
        fun onItemLongClick(info: ApplicationInfo): Boolean
    }

    interface OnItemCheckedChangeListener {
        fun onItemCheckedChange(buttonView: CompoundButton, isChecked: Boolean, packageName: String)
    }
}