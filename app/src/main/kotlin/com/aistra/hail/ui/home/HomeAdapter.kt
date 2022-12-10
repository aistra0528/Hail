package com.aistra.hail.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.AppIconCache
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.Job

object HomeAdapter : ListAdapter<AppInfo, HomeAdapter.ViewHolder>(HomeDiff()) {
    private var loadIconJob: Job? = null
    val selectedList = mutableListOf<AppInfo>()
    lateinit var onItemClickListener: OnItemClickListener
    lateinit var onItemLongClickListener: OnItemLongClickListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_home, parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val info = currentList[position]
        holder.itemView.run {
            setOnClickListener { onItemClickListener.onItemClick(info) }
            setOnLongClickListener { onItemLongClickListener.onItemLongClick(info) }
            info.selected = info.isNowSelected(selectedList)
            info.state = info.getCurrentState()
            findViewById<ImageView>(R.id.app_icon).run {
                info.applicationInfo?.let {
                    loadIconJob = AppIconCache.loadIconBitmapAsync(
                        context,
                        it,
                        it.uid / 100000,
                        this,
                        HailData.grayscaleIcon && info.state == AppInfo.STATE_FROZEN
                    )
                } ?: run {
                    setImageDrawable(context.packageManager.defaultActivityIcon)
                    colorFilter = null
                }
            }
            findViewById<TextView>(R.id.app_name).run {
                text = StringBuilder().apply {
                    if (!HailData.grayscaleIcon && info.state == AppInfo.STATE_FROZEN) append("❄️")
                    if (info.whitelisted) append("\uD83D\uDD12")
                    append(info.name)
                }
                isEnabled = !HailData.grayscaleIcon || info.state != AppInfo.STATE_FROZEN
                when {
                    info.selected -> setTextColor(
                        MaterialColors.getColor(this, R.attr.colorPrimary)
                    )
                    info.state == AppInfo.STATE_UNKNOWN -> setTextColor(context.getColorStateList(R.color.colorWarn))
                    else -> setTextAppearance(R.style.TextAppearance_Material3_BodyMedium)
                }
            }
        }
    }

    fun onDestroy() {
        if (loadIconJob?.isActive == true) loadIconJob?.cancel()
    }

    class HomeDiff : DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean =
            oldItem == newItem

        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean =
            oldItem.state == newItem.getCurrentState()
                    && oldItem.selected == newItem.isNowSelected(selectedList)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    interface OnItemClickListener {
        fun onItemClick(info: AppInfo)
    }

    interface OnItemLongClickListener {
        fun onItemLongClick(info: AppInfo): Boolean
    }
}