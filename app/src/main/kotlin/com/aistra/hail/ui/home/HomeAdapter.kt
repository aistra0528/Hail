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
import com.aistra.hail.app.HData

object HomeAdapter : RecyclerView.Adapter<HomeAdapter.ViewHolder>() {
    private val list = HData.checkedList
    lateinit var onItemClickListener: OnItemClickListener
    lateinit var onItemLongClickListener: OnItemLongClickListener

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
                onItemClickListener.onItemClick(position)
            }
            setOnLongClickListener {
                onItemLongClickListener.onItemLongClick(position)
            }
            try {
                context.packageManager.getPackageInfo(
                    list[position],
                    PackageManager.MATCH_UNINSTALLED_PACKAGES
                ).run {
                    AppManager.isAppHiddenOrDisabled(packageName).let {
                        findViewById<ImageView>(R.id.app_icon).run {
                            setImageDrawable(applicationInfo.loadIcon(context.packageManager))
                            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
                                setSaturation(if (it) 0f else 1f)
                            })
                        }
                        findViewById<TextView>(R.id.app_name).run {
                            setTextAppearance(R.style.TextAppearance_MaterialComponents_Body2)
                            text = applicationInfo.loadLabel(context.packageManager)
                            isEnabled = !it
                        }
                    }
                }
            } catch (e: Exception) {
                setOnClickListener(null)
                setOnLongClickListener(null)
                findViewById<ImageView>(R.id.app_icon).run {
                    setImageResource(R.drawable.ic_baseline_android_24)
                    colorFilter = null
                }
                findViewById<TextView>(R.id.app_name).run {
                    text = list[position]
                    setTextColor(context.getColorStateList(R.color.colorError))
                }
            }
        }
    }

    override fun getItemCount(): Int = list.size

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    interface OnItemLongClickListener {
        fun onItemLongClick(position: Int): Boolean
    }
}