package com.aistra.hail.ui.actions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aistra.hail.app.AppInfo
import com.aistra.hail.databinding.ItemActionBinding
import com.aistra.hail.utils.ActionExecutor
import com.aistra.hail.utils.AppIconCache
import com.aistra.hail.utils.LaunchAction

class ActionsAdapter(
    private val onClick: (LaunchAction) -> Unit,
    private val onLongClick: (LaunchAction) -> Unit
) : RecyclerView.Adapter<ActionsAdapter.ViewHolder>() {
    private val actions = mutableListOf<LaunchAction>()

    fun submitList(value: List<LaunchAction>) {
        actions.clear()
        actions.addAll(value)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemActionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(actions[position])

    override fun getItemCount() = actions.size

    inner class ViewHolder(private val binding: ItemActionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(action: LaunchAction) {
            val launchInfo = AppInfo(action.launchPackage).applicationInfo
            binding.appName.text = launchInfo?.loadLabel(binding.root.context.packageManager) ?: action.launchPackage
            binding.appDependencies.text = action.unfreezePackages.joinToString(", ") { packageName ->
                AppInfo(packageName).name
            }
            launchInfo?.let {
                AppIconCache.loadIconBitmapAsync(binding.root.context, it, com.aistra.hail.utils.HPackages.myUserId, binding.appIcon, false)
            } ?: binding.appIcon.setImageDrawable(binding.root.context.packageManager.defaultActivityIcon)
            binding.root.setOnClickListener { onClick(action) }
            binding.root.setOnLongClickListener { onLongClick(action); true }
        }
    }
}