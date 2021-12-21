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

object HomeAdapter : ListAdapter<AppInfo, HomeAdapter.ViewHolder>(
    object : DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean =
            oldItem == newItem

        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean =
            oldItem.state == newItem.getCurrentState()
    }) {
    private val cf by lazy { ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) }) }
    lateinit var onItemClickListener: OnItemClickListener
    lateinit var onItemLongClickListener: OnItemLongClickListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_home, parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.run {
            setOnClickListener { onItemClickListener.onItemClick(holder.adapterPosition) }
            setOnLongClickListener { onItemLongClickListener.onItemLongClick(holder.adapterPosition) }
            with(currentList[position]) {
                state = getCurrentState()
                findViewById<ImageView>(R.id.app_icon).run {
                    setImageDrawable(icon)
                    colorFilter = if (state == AppInfo.STATE_FROZEN) cf else null
                }
                findViewById<TextView>(R.id.app_name).run {
                    text = name
                    isEnabled = if (state == AppInfo.STATE_UNKNOWN) {
                        setTextColor(context.getColorStateList(R.color.colorWarn))
                        true
                    } else {
                        setTextAppearance(R.style.TextAppearance_MaterialComponents_Body2)
                        state != AppInfo.STATE_FROZEN
                    }
                }
            }
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    interface OnItemLongClickListener {
        fun onItemLongClick(position: Int): Boolean
    }
}