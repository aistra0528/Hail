package com.aistra.hail.ui.home

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aistra.hail.R
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData

object HomeAdapter : RecyclerView.Adapter<HomeAdapter.ViewHolder>() {
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
            with(HailData.checkedList[position]) {
                findViewById<ImageView>(R.id.app_icon).run {
                    setImageDrawable(icon)
                    colorFilter = if (AppManager.isAppFrozen(packageName)) cf else null
                }
                findViewById<TextView>(R.id.app_name).run {
                    text = name
                    isEnabled = if (applicationInfo == null) {
                        setTextColor(context.getColorStateList(R.color.colorWarn))
                        true
                    } else {
                        setTextAppearance(R.style.TextAppearance_MaterialComponents_Body2)
                        !AppManager.isAppFrozen(packageName)
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int = HailData.checkedList.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    interface OnItemLongClickListener {
        fun onItemLongClick(position: Int): Boolean
    }
}