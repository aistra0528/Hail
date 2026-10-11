package com.aistra.hail.ui.apps

import android.content.pm.ApplicationInfo
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aistra.hail.databinding.ItemAppsBinding
import com.aistra.hail.utils.AppIconCache
import com.aistra.hail.utils.HPackages
import kotlinx.coroutines.Job

class DisabledAppsAdapter(
    private val selectedPackages: Set<String>,
    private val onSelectionChanged: (String, Boolean) -> Unit,
) : ListAdapter<ApplicationInfo, DisabledAppsAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ApplicationInfo>() {
            override fun areItemsTheSame(oldItem: ApplicationInfo, newItem: ApplicationInfo) =
                oldItem.packageName == newItem.packageName

            override fun areContentsTheSame(oldItem: ApplicationInfo, newItem: ApplicationInfo) =
                oldItem.enabled == newItem.enabled && oldItem.flags == newItem.flags
        }
    }

    private var loadIconJob: Job? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemAppsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    fun onDestroy() {
        loadIconJob?.cancel()
    }

    inner class ViewHolder(private val binding: ItemAppsBinding) : RecyclerView.ViewHolder(binding.root) {
        private var updating = false
        private lateinit var info: ApplicationInfo

        init {
            binding.root.setOnClickListener { binding.appStar.toggle() }
            binding.appStar.setOnCheckedChangeListener { _, isChecked ->
                if (!updating) onSelectionChanged(info.packageName, isChecked)
            }
        }

        fun bind(info: ApplicationInfo) {
            updating = true
            this.info = info
            AppIconCache.loadIconBitmapAsync(
                binding.root.context, info, HPackages.myUserId, binding.appIcon, true
            ).also { loadIconJob = it }
            binding.appName.text = info.loadLabel(binding.root.context.packageManager)
            binding.appName.isEnabled = false
            binding.appDesc.text = info.packageName
            binding.appDesc.isEnabled = false
            binding.appStar.isChecked = info.packageName in selectedPackages
            updating = false
        }
    }
}
