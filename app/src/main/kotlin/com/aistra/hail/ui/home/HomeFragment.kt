package com.aistra.hail.ui.home

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailApi
import com.aistra.hail.app.HailData
import com.aistra.hail.databinding.DialogInputBinding
import com.aistra.hail.databinding.FragmentHomeBinding
import com.aistra.hail.ui.main.MainFragment
import com.aistra.hail.utils.HUI
import com.aistra.hail.work.HWork
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout

class HomeFragment : MainFragment(), HomeAdapter.OnItemClickListener,
    HomeAdapter.OnItemLongClickListener {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        binding.recyclerView.run {
            layoutManager =
                GridLayoutManager(activity, resources.getInteger(R.integer.home_span))
            adapter = HomeAdapter.apply {
                onItemClickListener = this@HomeFragment
                onItemLongClickListener = this@HomeFragment
            }
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    when (newState) {
                        RecyclerView.SCROLL_STATE_IDLE -> postDelayed(
                            { activity.fab.show() }, 1000
                        )
                        RecyclerView.SCROLL_STATE_DRAGGING -> activity.fab.hide()
                    }
                }
            })
        }
        binding.refresh.run {
            setOnRefreshListener {
                updateCurrentList()
                isRefreshing = false
            }
        }
        binding.tabs.run {
            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    updateCurrentList()
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {}

                override fun onTabReselected(tab: TabLayout.Tab) {
                    if (tab.position > 0) showTagDialog()
                }
            })
            HailData.tags.forEach { addTab(newTab().setText(it.first), tabCount == 0) }
            if (tabCount == 1) visibility = View.GONE
        }
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        activity.fab.run {
            if (hasOnClickListeners()) updateCurrentList()
            else setOnClickListener { freezeSelectedTab() }
        }
    }

    private fun updateCurrentList() {
        HomeAdapter.submitList(HailData.checkedList.filter {
            it.tagId == HailData.tags[binding.tabs.selectedTabPosition].second
        })
    }

    private fun freezeSelectedTab() = setAllFrozen(true, justSelectedTab = true)

    override fun onItemClick(position: Int) {
        val info = HomeAdapter.currentList[position]
        val pkg = info.packageName
        if (info.applicationInfo == null) {
            Snackbar.make(activity.fab, R.string.app_not_installed, Snackbar.LENGTH_LONG)
                .setAction(R.string.action_remove_home) { removeCheckedApp(pkg) }.show()
            return
        }
        if (AppManager.isAppFrozen(pkg) && AppManager.setAppFrozen(pkg, false))
            HomeAdapter.notifyItemChanged(position)
        app.packageManager.getLaunchIntentForPackage(pkg)?.let {
            startActivity(it)
        } ?: Snackbar.make(activity.fab, R.string.activity_not_found, Snackbar.LENGTH_SHORT).show()
    }

    override fun onItemLongClick(position: Int): Boolean {
        val info = HomeAdapter.currentList[position]
        info.applicationInfo ?: return false
        val pkg = info.packageName
        val frozen = AppManager.isAppFrozen(pkg)
        val action = getString(if (frozen) R.string.action_unfreeze else R.string.action_freeze)
        MaterialAlertDialogBuilder(activity).setTitle(info.name).setItems(
            arrayOf(
                action,
                getString(R.string.action_deferred_task),
                getString(R.string.action_tag_set),
                getString(R.string.action_create_shortcut),
                getString(R.string.action_remove_home)
            )
        ) { _, which ->
            when (which) {
                0 -> setAppFrozen(position, pkg, !frozen)
                1 -> {
                    val values = resources.getIntArray(R.array.deferred_task_values)
                    val entries = arrayOfNulls<String>(values.size)
                    for (i in values.indices)
                        entries[i] = resources.getQuantityString(
                            R.plurals.deferred_task_entry,
                            values[i],
                            values[i]
                        )
                    MaterialAlertDialogBuilder(activity).setTitle(R.string.action_deferred_task)
                        .setItems(entries) { _, i ->
                            HWork.setDeferredFrozen(pkg, !frozen, values[i].toLong())
                            Snackbar.make(
                                activity.fab,
                                resources.getQuantityString(
                                    R.plurals.msg_deferred_task,
                                    values[i], values[i], action, info.name
                                ),
                                Snackbar.LENGTH_INDEFINITE
                            ).setAction(R.string.action_undo) { HWork.cancelWork(pkg) }.show()
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .create().show()
                }
                2 -> {
                    MaterialAlertDialogBuilder(activity).setTitle(R.string.action_tag_set)
                        .setSingleChoiceItems(
                            HailData.tags.map { it.first }.toTypedArray(),
                            binding.tabs.selectedTabPosition
                        ) { dialog, index ->
                            if (info.tagId != HailData.tags[index].second) {
                                info.setTag(HailData.tags[index].second)
                                updateCurrentList()
                            }
                            dialog.cancel()
                        }
                        .setNeutralButton(R.string.action_tag_add) { _, _ ->
                            showTagDialog(info)
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .create().show()
                }
                3 -> createShortcut(
                    info.icon, pkg, info.name,
                    HailApi.getIntentForPackage(HailApi.ACTION_LAUNCH, pkg)
                )
                4 -> removeCheckedApp(pkg)
            }
        }.create().show()
        return true
    }

    private fun setAppFrozen(position: Int, pkg: String, frozen: Boolean) = HUI.showToast(
        when {
            AppManager.isAppFrozen(pkg) == frozen || AppManager.setAppFrozen(pkg, frozen) -> {
                HomeAdapter.notifyItemChanged(position)
                getString(
                    if (AppManager.isAppFrozen(pkg)) R.string.msg_freeze else R.string.msg_unfreeze,
                    HomeAdapter.currentList[position].name
                )
            }
            pkg == app.packageName -> getString(R.string.app_slogan)
            else -> getString(R.string.permission_denied)
        }
    )

    private fun setAllFrozen(frozen: Boolean, justSelectedTab: Boolean = false) {
        var i = 0
        var denied = false
        (if (justSelectedTab) HomeAdapter.currentList else HailData.checkedList).forEachIndexed { index, info ->
            when {
                AppManager.isAppFrozen(info.packageName) == frozen -> return@forEachIndexed
                AppManager.setAppFrozen(info.packageName, frozen) ->
                    if (justSelectedTab)
                        HomeAdapter.notifyItemChanged(index)
                    else i++
                info.packageName != app.packageName && info.applicationInfo != null -> denied = true
            }
        }
        when {
            denied && i == 0 -> HUI.showToast(getString(R.string.permission_denied))
            justSelectedTab.not() && i > 0 -> {
                updateCurrentList()
                HUI.showToast(
                    getString(
                        if (frozen) R.string.msg_freeze else R.string.msg_unfreeze, i.toString()
                    )
                )
            }
        }
    }

    private fun showTagDialog(info: AppInfo? = null) {
        val isAdd = info != null
        val input = DialogInputBinding.inflate(layoutInflater, FrameLayout(activity), true)
        input.inputLayout.setHint(if (isAdd) R.string.action_tag_add else R.string.action_tag_set)
        if (isAdd.not()) input.editText.append(HailData.tags[binding.tabs.selectedTabPosition].first)
        MaterialAlertDialogBuilder(activity)
            .setView(input.root.parent as View)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val tagName = input.editText.text.toString()
                val tagId = tagName.hashCode()
                if (tagName in HailData.tags.map { it.first } || tagId in HailData.tags.map { it.second }) return@setPositiveButton
                binding.tabs.run {
                    if (isAdd) {
                        HailData.tags.add(tagName to tagId)
                        info!!.setTag(tagId)
                        addTab(newTab().setText(tagName), false)
                        if (tabCount == 2) visibility = View.VISIBLE
                        updateCurrentList()
                    } else {
                        HailData.tags.run {
                            removeAt(selectedTabPosition)
                            add(selectedTabPosition, tagName to tagId)
                        }
                        HomeAdapter.currentList.forEach {
                            it.setTag(tagId)
                        }
                        getTabAt(selectedTabPosition)!!.text = tagName
                    }
                }
                HailData.saveTags()
            }
            .apply {
                with(binding.tabs) {
                    if (isAdd.not()) setNeutralButton(R.string.action_tag_remove) { _, _ ->
                        HailData.checkedList.forEach {
                            if (it in HomeAdapter.currentList) it.setTag(0)
                        }
                        HailData.tags.removeAt(selectedTabPosition)
                        HailData.saveTags()
                        removeTabAt(selectedTabPosition)
                        if (tabCount == 1) visibility = View.GONE
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create().show()
    }

    private fun createShortcut(icon: Drawable, id: String, label: CharSequence, intent: Intent) {
        if (ShortcutManagerCompat.isRequestPinShortcutSupported(app)) {
            ShortcutManagerCompat.requestPinShortcut(
                app, ShortcutInfoCompat.Builder(app, id)
                    .setIcon(getAppIcon(icon))
                    .setShortLabel(label)
                    .setIntent(intent)
                    .build(), null
            )
        } else HUI.showToast(
            getString(
                R.string.operation_failed,
                getString(R.string.action_create_shortcut)
            )
        )
    }

    private fun getAppIcon(drawable: Drawable): IconCompat =
        IconCompat.createWithBitmap(Bitmap.createBitmap(
            drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
        ).also {
            with(Canvas(it)) {
                drawable.setBounds(0, 0, width, height)
                drawable.draw(this)
            }
        })

    private fun removeCheckedApp(packageName: String) {
        HailData.removeCheckedApp(packageName)
        updateCurrentList()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_create_shortcut -> {
                MaterialAlertDialogBuilder(activity).setTitle(R.string.action_create_shortcut)
                    .setItems(R.array.create_shortcut_entries) { _, which ->
                        when (which) {
                            0 -> createShortcut(
                                app.applicationInfo.loadIcon(app.packageManager),
                                HailApi.ACTION_FREEZE_ALL,
                                getString(R.string.action_freeze_all),
                                Intent(HailApi.ACTION_FREEZE_ALL)
                            )
                            1 -> createShortcut(
                                app.applicationInfo.loadIcon(app.packageManager),
                                HailApi.ACTION_UNFREEZE_ALL,
                                getString(R.string.action_unfreeze_all),
                                Intent(HailApi.ACTION_UNFREEZE_ALL)
                            )
                            2 -> createShortcut(
                                app.applicationInfo.loadIcon(app.packageManager),
                                HailApi.ACTION_LOCK,
                                getString(R.string.action_lock),
                                Intent(HailApi.ACTION_LOCK)
                            )
                            3 -> createShortcut(
                                app.applicationInfo.loadIcon(app.packageManager),
                                HailApi.ACTION_LOCK_FREEZE,
                                getString(R.string.action_lock_freeze),
                                Intent(HailApi.ACTION_LOCK_FREEZE)
                            )
                        }
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .create().show()
            }
            R.id.action_freeze_all -> setAllFrozen(true)
            R.id.action_unfreeze_all -> setAllFrozen(false)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_home, menu)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}