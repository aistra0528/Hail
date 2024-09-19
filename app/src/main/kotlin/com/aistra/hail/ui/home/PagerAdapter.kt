package com.aistra.hail.ui.home

import android.util.TypedValue
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
import com.aistra.hail.utils.HPackages.myUserId
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.Job

class PagerAdapter(private val selectedList: List<AppInfo>) :
    ListAdapter<AppInfo, PagerAdapter.ViewHolder>(HomeDiff(selectedList)) {
    private var loadIconJob: Job? = null
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
            info.selected = info in selectedList
            info.state = info.getCurrentState()
            findViewById<ImageView>(R.id.app_icon).run {
                info.applicationInfo?.let {
                    loadIconJob = AppIconCache.loadIconBitmapAsync(
                        context,
                        it,
                        myUserId,
                        this,
                        HailData.grayscaleIcon && info.state == AppInfo.STATE_FROZEN
                    )
                } ?: run {
                    setImageDrawable(context.packageManager.defaultActivityIcon)
                    colorFilter = null
                }
            }
            findViewById<TextView>(R.id.app_name).run {
                text = buildString {
                    if (!HailData.grayscaleIcon && info.state == AppInfo.STATE_FROZEN) append("❄️")
                    if (info.whitelisted) append("\uD83D\uDD12")
                    append(info.name)
                }
                isEnabled = !HailData.grayscaleIcon || info.state != AppInfo.STATE_FROZEN
                when {
                    info.selected -> setTextColor(
                        MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary)
                    )

                    info.state == AppInfo.STATE_NOT_FOUND -> setTextColor(context.getColorStateList(R.color.color_warn))
                    else -> setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
                }
                setTextSize(TypedValue.COMPLEX_UNIT_SP, HailData.homeFontSize.toFloat())
            }
        }
    }

    fun onDestroy() {
        if (loadIconJob?.isActive == true) loadIconJob?.cancel()
    }

    class HomeDiff(private val selectedList: List<AppInfo>) :
        DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean =
            oldItem == newItem

        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean =
            oldItem.state == newItem.getCurrentState()
                    && oldItem.selected == newItem in selectedList
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    interface OnItemClickListener {
        fun onItemClick(info: AppInfo)
    }

    interface OnItemLongClickListener {
        fun onItemLongClick(info: AppInfo): Boolean
    }
}