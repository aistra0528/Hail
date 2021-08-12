package com.aistra.hail.ui.apps

import android.content.pm.PackageInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aistra.hail.R
import com.aistra.hail.app.HData

class AppsAdapter(private val mList: MutableList<PackageInfo>) :
    RecyclerView.Adapter<AppsAdapter.ViewHolder>() {

    private var mOnItemClickListener: OnItemClickListener? = null
    private var mOnItemCheckedChangeListener: OnItemCheckedChangeListener? = null

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    interface OnItemCheckedChangeListener {
        fun onItemCheckedChange(buttonView: CompoundButton, isChecked: Boolean, position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener?) {
        mOnItemClickListener = listener
    }

    fun setOnItemCheckedChangeListener(listener: OnItemCheckedChangeListener?) {
        mOnItemCheckedChangeListener = listener
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_apps, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.run {
            setOnClickListener {
                mOnItemClickListener?.onItemClick(position)
            }
            mList[position].run {
                findViewById<ImageView>(R.id.app_icon).setImageDrawable(
                    applicationInfo.loadIcon(context.packageManager)
                )
                findViewById<TextView>(R.id.app_name).text =
                    applicationInfo.loadLabel(context.packageManager)
                findViewById<TextView>(R.id.app_desc).text = packageName
                findViewById<CheckBox>(R.id.app_star).run {
                    isChecked = HData.isChecked(packageName)
                    setOnCheckedChangeListener { buttonView, isChecked ->
                        mOnItemCheckedChangeListener?.onItemCheckedChange(
                            buttonView,
                            isChecked,
                            position
                        )
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int = mList.size
}