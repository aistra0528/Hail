package com.aistra.hail.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.aistra.hail.R
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HData
import com.aistra.hail.ui.main.MainFragment

class HomeFragment : MainFragment(), HomeAdapter.OnItemClickListener,
    HomeAdapter.OnItemLongClickListener {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(false)
        return SwipeRefreshLayout(activity).apply {
            addView(RecyclerView(activity).apply {
                layoutManager =
                    GridLayoutManager(activity, activity.resources.getInteger(R.integer.home_span))
                adapter = HomeAdapter.apply {
                    onItemClickListener = this@HomeFragment
                    onItemLongClickListener = this@HomeFragment
                }
            })
            setOnRefreshListener {
                HomeAdapter.notifyItemRangeChanged(0, HomeAdapter.itemCount)
                isRefreshing = false
            }
        }
    }

    override fun onItemClick(position: Int) {
        HData.checkedList[position].let {
            if (AppManager.isAppHiddenOrDisabled(it)) {
                AppManager.setAppHiddenOrDisabled(it, false)
                HomeAdapter.notifyItemChanged(position)
            }
            app.packageManager.getLaunchIntentForPackage(it)?.run {
                startActivity(this)
            }
        }
    }

    override fun onItemLongClick(position: Int): Boolean {
        HData.checkedList[position].let {
            AppManager.setAppHiddenOrDisabled(it, !AppManager.isAppHiddenOrDisabled(it))
            HomeAdapter.notifyItemChanged(position)
        }
        return true
    }
}