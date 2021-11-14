package com.aistra.hail.ui.apps

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aistra.hail.HailApp
import com.aistra.hail.R
import com.aistra.hail.app.HData
import com.aistra.hail.app.HLog

object AppsAdapter : RecyclerView.Adapter<AppsAdapter.ViewHolder>() {
    private val pm = HailApp.app.packageManager
    val list = mutableListOf<PackageInfo>()
    lateinit var onItemClickListener: OnItemClickListener
    lateinit var onItemCheckedChangeListener: OnItemCheckedChangeListener

    init {
        updateList(HData.showSystemApps, null)
    }

    @SuppressLint("InlinedApi")
    fun updateList(sysApp: Boolean, query: String?) {
        list.clear()
        val ms = System.currentTimeMillis()
        pm.getInstalledPackages(PackageManager.MATCH_UNINSTALLED_PACKAGES).forEach {
            if ((sysApp || it.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != ApplicationInfo.FLAG_SYSTEM)
                && (query.isNullOrEmpty()
                        || it.applicationInfo.loadLabel(pm).toString().lowercase()
                    .contains(query.lowercase())
                        || it.packageName.lowercase().contains(query.lowercase()))
            )
                list.add(it)
        }
        list.sortByDescending { it.lastUpdateTime }
        HLog.i("Update ${list.size} items in ${System.currentTimeMillis() - ms}ms")
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_apps, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        list[position].run {
            holder.itemView.run {
                setOnClickListener {
                    onItemClickListener.onItemClick(position)
                }
                findViewById<ImageView>(R.id.app_icon).setImageDrawable(
                    applicationInfo.loadIcon(pm)
                )
                findViewById<TextView>(R.id.app_name).text =
                    applicationInfo.loadLabel(pm)
                findViewById<TextView>(R.id.app_desc).text = packageName
                findViewById<CheckBox>(R.id.app_star).run {
                    setOnCheckedChangeListener(null)
                    isChecked = HData.isChecked(packageName)
                    setOnCheckedChangeListener { buttonView, isChecked ->
                        onItemCheckedChangeListener.onItemCheckedChange(
                            buttonView,
                            isChecked,
                            position
                        )
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int = list.size

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    interface OnItemCheckedChangeListener {
        fun onItemCheckedChange(buttonView: CompoundButton, isChecked: Boolean, position: Int)
    }
}