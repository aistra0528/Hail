package com.aistra.hail.ui.home

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
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

object HomeAdapter : ListAdapter<AppInfo, HomeAdapter.ViewHolder>(HomeDiff()) {
    private val cf by lazy { ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) }) }
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
                setImageDrawable(info.icon)
                colorFilter = if (info.state == AppInfo.STATE_FROZEN) cf else null
            }
            findViewById<TextView>(R.id.app_name).run {
                text = info.name
                isEnabled = info.state != AppInfo.STATE_FROZEN
                when {
                    info.selected -> setTextColor(context.getColorStateList(R.color.colorPrimary))
                    info.state == AppInfo.STATE_UNKNOWN -> setTextColor(context.getColorStateList(R.color.colorWarn))
                    else -> setTextAppearance(R.style.TextAppearance_Material3_BodyMedium)
                }
            }
        }
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