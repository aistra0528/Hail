package com.aistra.hail.ui.whitelist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.HailData
import com.aistra.hail.ui.main.MainFragment

class WhitelistFragment : MainFragment(), WhitelistAdapter.OnItemCheckedChangeListener {
    private lateinit var refreshLayout: SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return SwipeRefreshLayout(activity).apply {
            refreshLayout = this
            addView(RecyclerView(activity).apply {
                layoutManager =
                    GridLayoutManager(activity, resources.getInteger(R.integer.apps_span))
                adapter = WhitelistAdapter.apply {
                    onItemCheckedChangeListener = this@WhitelistFragment
                }
            })
            setOnRefreshListener { WhitelistAdapter.updateCurrentList(this) }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        WhitelistAdapter.updateCurrentList(refreshLayout)
    }

    override fun onDestroy() {
        WhitelistAdapter.onDestroy()
        super.onDestroy()
    }

    override fun onItemCheckedChange(
        buttonView: CompoundButton,
        isWhitelisted: Boolean,
        appInfo: AppInfo
    ) {
        appInfo.whitelisted = isWhitelisted
        HailData.saveApps()
    }
}