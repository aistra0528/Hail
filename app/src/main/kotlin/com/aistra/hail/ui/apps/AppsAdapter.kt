package com.aistra.hail.ui.apps

import android.content.pm.ApplicationInfo
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aistra.hail.R
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.databinding.ItemAppsBinding
import com.aistra.hail.utils.AppIconCache
import com.aistra.hail.utils.HPackages
import kotlinx.coroutines.Job

class AppsAdapter : ListAdapter<ApplicationInfo, AppsAdapter.ViewHolder>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<ApplicationInfo>() {
            override fun areItemsTheSame(
                oldItem: ApplicationInfo, newItem: ApplicationInfo
            ): Boolean = oldItem.packageName == newItem.packageName

            override fun areContentsTheSame(oldItem: ApplicationInfo, newItem: ApplicationInfo): Boolean =
                oldItem.flags and ApplicationInfo.FLAG_INSTALLED == newItem.flags and ApplicationInfo.FLAG_INSTALLED
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
        lateinit var info: ApplicationInfo
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
                if (!updating) onItemCheckedChangeListener.onItemCheckedChange(button, isChecked, pkg)
            }
        }

        fun bindInfo(info: ApplicationInfo) {
            updating = true
            this.info = info
            val frozen = AppManager.isAppFrozen(pkg)

            binding.appIcon.apply {
                loadIconJob = AppIconCache.loadIconBitmapAsync(
                    context, info, HPackages.myUserId, this, HailData.grayscaleIcon && frozen
                )
            }
            binding.appName.apply {
                val name = info.loadLabel(context.packageManager)
                text = if (!HailData.grayscaleIcon && frozen) "❄️$name" else name
                isEnabled = !HailData.grayscaleIcon || !frozen
                if (HPackages.isAppUninstalled(pkg)) setTextColor(context.getColorStateList(R.color.color_warn))
                else setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
            }
            binding.appDesc.apply {
                text = pkg
                isEnabled = !HailData.grayscaleIcon || !frozen
            }
            binding.appStar.isChecked = HailData.isChecked(pkg)
            updating = false
        }
    }

    interface OnItemClickListener {
        fun onItemClick(buttonView: CompoundButton)
    }

    interface OnItemCheckedChangeListener {
        fun onItemCheckedChange(buttonView: CompoundButton, isChecked: Boolean, packageName: String)
    }
}