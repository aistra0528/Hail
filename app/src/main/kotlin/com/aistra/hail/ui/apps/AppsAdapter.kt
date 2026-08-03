package com.aistra.hail.ui.apps

import android.content.pm.ApplicationInfo
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.databinding.ItemAppsBinding
import com.aistra.hail.utils.AppIconCache
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.Job

class AppsAdapter : ListAdapter<AppInfo, AppsAdapter.ViewHolder>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<AppInfo>() {
            override fun areItemsTheSame(
                oldItem: AppInfo, newItem: AppInfo
            ): Boolean = oldItem == newItem

            override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean =
                oldItem.applicationInfo?.flags?.and(ApplicationInfo.FLAG_INSTALLED) ==
                        newItem.applicationInfo?.flags?.and(ApplicationInfo.FLAG_INSTALLED)
        }
    }

    lateinit var onItemClickListener: OnItemClickListener
    lateinit var onItemCheckedChangeListener: OnItemCheckedChangeListener
    private var loadIconJob: Job? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        ItemAppsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val info = currentList[position]
        holder.bindInfo(info)
    }

    fun onDestroy() {
        if (loadIconJob?.isActive == true) loadIconJob?.cancel()
    }

    inner class ViewHolder(private val binding: ItemAppsBinding) : RecyclerView.ViewHolder(binding.root) {
        lateinit var info: AppInfo
        private val pkg get() = info.packageName

        /**
         * Flag that view data is being updated to avoid triggering the event.
         * */
        private var updating = false

        init {
            binding.root.apply {
                setOnClickListener { onItemClickListener.onItemClick(binding.appStar) }
                isLongClickable = true
            }
            binding.appStar.setOnCheckedChangeListener { button, isChecked ->
                if (!updating) onItemCheckedChangeListener.onItemCheckedChange(button, isChecked, info)
            }
        }

        fun bindInfo(info: AppInfo) {
            updating = true
            this.info = info
            val appInfo = info.applicationInfo
            val frozen = AppManager.isAppFrozen(pkg, info.userId)

            binding.appIcon.apply {
                appInfo?.let {
                    loadIconJob = AppIconCache.loadIconBitmapAsync(
                        context, it, info.userId, this, HailData.grayscaleIcon && frozen
                    )
                } ?: setImageDrawable(context.packageManager.defaultActivityIcon)
            }
            binding.appName.apply {
                val name = info.name
                text = if (!HailData.grayscaleIcon && frozen) "❄️$name" else name
                isEnabled = !HailData.grayscaleIcon || !frozen
                if (com.aistra.hail.utils.HPackages.isAppUninstalled(pkg, info.userId)) setTextColor(
                    MaterialColors.getColor(
                        this, androidx.appcompat.R.attr.colorError
                    )
                )
                else setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
            }
            binding.appDesc.apply {
                text = if (info.userId == com.aistra.hail.utils.HPackages.myUserId) pkg else "$pkg (${info.userId})"
                isEnabled = !HailData.grayscaleIcon || !frozen
            }
            binding.appStar.isChecked = HailData.isChecked(pkg, info.userId)
            updating = false
        }
    }

    interface OnItemClickListener {
        fun onItemClick(buttonView: CompoundButton)
    }

    interface OnItemCheckedChangeListener {
        fun onItemCheckedChange(buttonView: CompoundButton, isChecked: Boolean, info: AppInfo)
    }
}
