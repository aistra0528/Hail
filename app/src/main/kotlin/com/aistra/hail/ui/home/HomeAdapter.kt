package com.aistra.hail.ui.home

import android.annotation.SuppressLint
import android.content.pm.PackageManager
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

class HomeAdapter(private val mList: MutableList<String>) :
    RecyclerView.Adapter<HomeAdapter.ViewHolder>() {

    private var mOnItemClickListener: OnItemClickListener? = null
    private var mOnItemLongClickListener: OnItemLongClickListener? = null

    interface OnItemClickListener {
        fun onItemClick(itemView: View, position: Int)
    }

    interface OnItemLongClickListener {
        fun onItemLongClick(itemView: View, position: Int): Boolean
    }

    fun setOnItemClickListener(listener: OnItemClickListener?) {
        mOnItemClickListener = listener
    }

    fun setOnItemLongClickListener(listener: OnItemLongClickListener?) {
        mOnItemLongClickListener = listener
    }

    fun updateItem(itemView: View, position: Int) {
        itemView.run {
            findViewById<ImageView>(R.id.app_icon).colorFilter =
                if (AppManager.isAppHiddenOrDisabled(mList[position]))
                    ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
                else null
            findViewById<TextView>(R.id.app_name).run {
                if (AppManager.isAppHiddenOrDisabled(mList[position]))
                    setTextColor(context.getColorStateList(R.color.colorPrimary))
                else
                    setTextAppearance(R.style.TextAppearance_MaterialComponents_Body2)
            }
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_home, parent, false)
        )
    }


    @SuppressLint("InlinedApi")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.run {
            setOnClickListener {
                mOnItemClickListener?.onItemClick(it, position)
            }
            setOnLongClickListener {
                mOnItemLongClickListener?.onItemLongClick(it, position) ?: false
            }
            try {
                context.packageManager.getPackageInfo(
                    mList[position],
                    PackageManager.MATCH_UNINSTALLED_PACKAGES
                ).run {
                    findViewById<ImageView>(R.id.app_icon).run {
                        setImageDrawable(
                            applicationInfo.loadIcon(context.packageManager)
                        )
                        if (AppManager.isAppHiddenOrDisabled(packageName))
                            colorFilter =
                                ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
                    }
                    findViewById<TextView>(R.id.app_name).run {
                        text = applicationInfo.loadLabel(context.packageManager)
                        if (AppManager.isAppHiddenOrDisabled(packageName))
                            setTextColor(context.getColorStateList(R.color.colorPrimary))
                    }
                }
            } catch (e: Exception) {
                findViewById<ImageView>(R.id.app_icon).setImageResource(R.drawable.ic_baseline_android_24)
                findViewById<TextView>(R.id.app_name).run {
                    text = mList[position]
                    setTextColor(context.getColorStateList(R.color.error_color_material_light))
                }
            }
        }
    }

    override fun getItemCount(): Int = mList.size
}