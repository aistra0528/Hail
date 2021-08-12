package com.aistra.hail.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HData
import com.aistra.hail.ui.main.MainFragment

class HomeFragment : MainFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(false)
        return RecyclerView(activity).apply {
            layoutManager = GridLayoutManager(activity, 4)
            adapter = HomeAdapter(HData.checkedList).apply {
                HomeFragment.adapter = this
                setOnItemClickListener(object : HomeAdapter.OnItemClickListener {
                    override fun onItemClick(itemView: View, position: Int) {
                        this@HomeFragment.onItemClick(itemView, position)
                    }
                })
                setOnItemLongClickListener(object : HomeAdapter.OnItemLongClickListener {
                    override fun onItemLongClick(itemView: View, position: Int): Boolean {
                        return this@HomeFragment.onItemLongClick(itemView, position)
                    }
                })
            }
        }
    }

    private fun onItemClick(itemView: View, position: Int) {
        HData.checkedList[position].let {
            if (AppManager.isAppHiddenOrDisabled(it)) {
                AppManager.setAppHiddenOrDisabled(it, false)
                adapter?.updateItem(itemView, position)
            }
            pm.getLaunchIntentForPackage(it)?.run {
                startActivity(this)
            }
        }
    }

    private fun onItemLongClick(itemView: View, position: Int): Boolean {
        HData.checkedList[position].let {
            AppManager.setAppHiddenOrDisabled(it, !AppManager.isAppHiddenOrDisabled(it))
            adapter?.updateItem(itemView, position)
        }
        return true
    }

    companion object {
        var adapter: HomeAdapter? = null
    }
}