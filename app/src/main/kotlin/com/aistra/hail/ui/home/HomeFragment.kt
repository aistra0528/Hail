package com.aistra.hail.ui.home

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.SearchView
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
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HUI
import com.aistra.hail.utils.NameComparator
import com.aistra.hail.work.HWork
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import org.json.JSONArray

class HomeFragment : MainFragment(),
    HomeAdapter.OnItemClickListener, HomeAdapter.OnItemLongClickListener {

    private var query: String = String()
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var multiselect: Boolean = false

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
                if (multiselect) updateCurrentList() else deselect()
                isRefreshing = false
            }
        }
        binding.tabs.run {
            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) = updateCurrentList()

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
        if (activity.fab.hasOnClickListeners()) updateCurrentList()
        activity.fab.setOnClickListener { setListFrozen(true, HomeAdapter.currentList) }
    }

    private fun updateCurrentList() {
        HomeAdapter.submitList(HailData.checkedList.filter {
            (query.isEmpty() && it.tagId == HailData.tags[binding.tabs.selectedTabPosition].second)
                    || (query.isNotEmpty() &&
                    (it.packageName.contains(query, true) || it.name.contains(query, true)))
        })
        activity.setAutoFreezeService()
    }

    private fun updateBarTitle() {
        activity.supportActionBar?.title = HomeAdapter.selectedList.size.let {
            if (it == 0) getString(R.string.app_name)
            else getString(R.string.msg_selected, it.toString())
        }
    }

    override fun onItemClick(info: AppInfo) {
        if (info.applicationInfo == null) {
            Snackbar.make(activity.fab, R.string.app_not_installed, Snackbar.LENGTH_LONG)
                .setAction(R.string.action_remove_home) { removeCheckedApp(info.packageName) }
                .show()
            return
        }
        if (multiselect) {
            HomeAdapter.run {
                if (info in selectedList) selectedList.remove(info)
                else selectedList.add(info)
            }
            updateCurrentList()
            updateBarTitle()
            return
        }
        launchApp(info)
    }

    override fun onItemLongClick(info: AppInfo): Boolean {
        info.applicationInfo ?: return false
        val actions = resources.getStringArray(R.array.home_action_entries)
        if (onMultiSelect(info, actions)) return true
        val pkg = info.packageName
        val frozen = AppManager.isAppFrozen(pkg)
        val action = getString(if (frozen) R.string.action_unfreeze else R.string.action_freeze)
        MaterialAlertDialogBuilder(activity).setTitle(info.name).setItems(
            actions.toMutableList().apply { removeAt(if (frozen) 1 else 2) }.toTypedArray()
        ) { _, which ->
            when (which) {
                0 -> launchApp(info)
                1 -> setListFrozen(!frozen, listOf(info))
                2 -> {
                    val values = resources.getIntArray(R.array.deferred_task_values)
                    val entries = arrayOfNulls<String>(values.size)
                    values.forEachIndexed { i, it ->
                        entries[i] =
                            resources.getQuantityString(R.plurals.deferred_task_entry, it, it)
                    }
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
                3 -> {
                    var checked = -1
                    for (i in HailData.tags.indices) {
                        if (info.tagId == HailData.tags[i].second) {
                            checked = i
                            break
                        }
                    }
                    MaterialAlertDialogBuilder(activity).setTitle(R.string.action_tag_set)
                        .setSingleChoiceItems(
                            HailData.tags.map { it.first }.toTypedArray(),
                            checked
                        ) { dialog, index ->
                            if (info.tagId != HailData.tags[index].second) {
                                info.tagId = HailData.tags[index].second
                                HailData.saveApps()
                                updateCurrentList()
                            }
                            dialog.cancel()
                        }
                        .setNeutralButton(R.string.action_tag_add) { _, _ ->
                            showTagDialog(listOf(info))
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .create().show()
                }
                4 -> addPinShortcut(
                    info.icon, pkg, info.name,
                    HailApi.getIntentForPackage(HailApi.ACTION_LAUNCH, pkg)
                )
                5 -> exportToClipboard(listOf(info))
                6 -> removeCheckedApp(pkg)
            }
        }.create().show()
        return true
    }

    private fun deselect() {
        HomeAdapter.selectedList.clear()
        updateCurrentList()
        updateBarTitle()
    }

    private fun onMultiSelect(info: AppInfo, actions: Array<String>): Boolean = HomeAdapter.run {
        if (info in selectedList) {
            MaterialAlertDialogBuilder(activity)
                .setTitle(getString(R.string.msg_selected, selectedList.size.toString()))
                .setItems(actions.filter {
                    it != getString(R.string.action_launch)
                            && it != getString(R.string.action_deferred_task)
                            && it != getString(R.string.action_add_pin_shortcut)
                }.toTypedArray()) { _, which ->
                    when (which) {
                        0 -> {
                            setListFrozen(true, selectedList, false)
                            deselect()
                        }
                        1 -> {
                            setListFrozen(false, selectedList, false)
                            deselect()
                        }
                        2 -> {
                            var checked = -1
                            for (i in HailData.tags.indices) {
                                if (selectedList.all { it.tagId == HailData.tags[i].second }) {
                                    checked = i
                                    break
                                }
                            }
                            MaterialAlertDialogBuilder(activity).setTitle(R.string.action_tag_set)
                                .setSingleChoiceItems(
                                    HailData.tags.map { it.first }.toTypedArray(),
                                    checked
                                ) { dialog, index ->
                                    selectedList.forEach { it.tagId = HailData.tags[index].second }
                                    HailData.saveApps()
                                    deselect()
                                    dialog.cancel()
                                }
                                .setNeutralButton(R.string.action_tag_add) { _, _ ->
                                    showTagDialog(selectedList)
                                }
                                .setNegativeButton(android.R.string.cancel, null)
                                .create().show()
                        }
                        3 -> {
                            exportToClipboard(selectedList)
                            deselect()
                        }
                        4 -> {
                            selectedList.forEach { removeCheckedApp(it.packageName, false) }
                            HailData.saveApps()
                            deselect()
                        }
                    }
                }
                .setNegativeButton(R.string.action_deselect) { _, _ -> deselect() }
                .create().show()
            true
        } else false
    }

    private fun launchApp(info: AppInfo) {
        if (AppManager.isAppFrozen(info.packageName)
            && AppManager.setAppFrozen(info.packageName, false)
        ) {
            updateCurrentList()
        }
        app.packageManager.getLaunchIntentForPackage(info.packageName)?.let {
            addDynamicShortcut(info)
            startActivity(it)
        } ?: HUI.showToast(R.string.activity_not_found)
    }

    private fun setListFrozen(
        frozen: Boolean, list: List<AppInfo> = HailData.checkedList, updateList: Boolean = true
    ) {
        var i = 0
        var denied = false
        var name = String()
        list.forEach {
            when {
                AppManager.isAppFrozen(it.packageName) == frozen -> return@forEach
                AppManager.setAppFrozen(it.packageName, frozen) -> {
                    i++
                    name = it.name.toString()
                }
                it.packageName != app.packageName && it.applicationInfo != null -> denied = true
            }
        }
        when {
            denied && i == 0 -> HUI.showToast(R.string.permission_denied)
            i > 0 -> {
                if (updateList) updateCurrentList()
                HUI.showToast(
                    if (frozen) R.string.msg_freeze else R.string.msg_unfreeze,
                    if (i > 1) i.toString() else name
                )
            }
        }
    }

    private fun showTagDialog(list: List<AppInfo>? = null) {
        val isMultiSelect = list?.size ?: 0 > 1
        val input = DialogInputBinding.inflate(layoutInflater, FrameLayout(activity), true)
        input.inputLayout.setHint(if (list != null) R.string.action_tag_add else R.string.action_tag_set)
        list ?: input.editText.setText(HailData.tags[binding.tabs.selectedTabPosition].first)
        MaterialAlertDialogBuilder(activity)
            .setView(input.root.parent as View)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val tagName = input.editText.text.toString()
                val tagId = tagName.hashCode()
                if (HailData.tags.any { it.first == tagName || it.second == tagId }) return@setPositiveButton
                binding.tabs.run {
                    if (list != null) {
                        HailData.tags.add(tagName to tagId)
                        list.forEach { it.tagId = tagId }
                        addTab(newTab().setText(tagName), false)
                        if (query.isEmpty() && tabCount == 2) visibility = View.VISIBLE
                        if (isMultiSelect) deselect() else updateCurrentList()
                    } else {
                        HailData.tags.run {
                            removeAt(selectedTabPosition)
                            add(selectedTabPosition, tagName to tagId)
                        }
                        HomeAdapter.currentList.forEach { it.tagId = tagId }
                        getTabAt(selectedTabPosition)!!.text = tagName
                    }
                }
                HailData.saveApps()
                HailData.saveTags()
            }
            .apply {
                list ?: setNeutralButton(R.string.action_tag_remove) { _, _ ->
                    HomeAdapter.currentList.forEach { it.tagId = 0 }
                    binding.tabs.run {
                        HailData.tags.removeAt(selectedTabPosition)
                        removeTabAt(selectedTabPosition)
                        if (tabCount == 1) visibility = View.GONE
                    }
                    HailData.saveApps()
                    HailData.saveTags()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create().show()
    }

    private fun addPinShortcut(icon: Drawable, id: String, label: CharSequence, intent: Intent) {
        if (ShortcutManagerCompat.isRequestPinShortcutSupported(app)) {
            val shortcut = ShortcutInfoCompat.Builder(app, id)
                .setIcon(getDrawableIcon(icon))
                .setShortLabel(label)
                .setIntent(intent)
                .build()
            ShortcutManagerCompat.requestPinShortcut(app, shortcut, null)
        } else HUI.showToast(R.string.operation_failed, getString(R.string.action_add_pin_shortcut))
    }

    private fun addDynamicShortcut(info: AppInfo) {
        if (isMaxDynamicShortcutCount()) removeAllDynamicShortcuts()
        val shortcut = ShortcutInfoCompat.Builder(app, info.packageName)
            .setIcon(getDrawableIcon(info.icon))
            .setShortLabel(info.name)
            .setIntent(HailApi.getIntentForPackage(HailApi.ACTION_LAUNCH, info.packageName))
            .build()
        ShortcutManagerCompat.pushDynamicShortcut(app, shortcut)
        val freezeAll = ShortcutInfoCompat.Builder(app, HailApi.ACTION_FREEZE_ALL)
            .setIcon(
                getDrawableIcon(
                    AppCompatResources.getDrawable(
                        app,
                        R.drawable.ic_round_frozen_shortcut
                    )!!
                )
            )
            .setShortLabel(getString(R.string.action_freeze_all))
            .setIntent(Intent(HailApi.ACTION_FREEZE_ALL))
            .build()
        ShortcutManagerCompat.pushDynamicShortcut(app, freezeAll)
    }

    private fun isMaxDynamicShortcutCount(): Boolean =
        ShortcutManagerCompat.getDynamicShortcuts(app).size >=
                ShortcutManagerCompat.getMaxShortcutCountPerActivity(app)

    private fun removeAllDynamicShortcuts() {
        ShortcutManagerCompat.removeAllDynamicShortcuts(app)
    }

    private fun getDrawableIcon(drawable: Drawable): IconCompat =
        IconCompat.createWithBitmap(
            Bitmap.createBitmap(
                drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
            ).also {
                with(Canvas(it)) {
                    drawable.setBounds(0, 0, width, height)
                    drawable.draw(this)
                }
            })

    private fun exportToClipboard(list: List<AppInfo>) {
        HUI.copyText(JSONArray().run {
            list.forEach { put(it.packageName) }
            toString()
        })
        HUI.showToast(
            R.string.msg_exported,
            if (list.size > 1) list.size.toString() else list[0].name
        )
    }

    private fun importFromClipboard() = try {
        val str = HUI.pasteText() ?: throw IllegalArgumentException()
        val json = JSONArray(str.substring(str.indexOf('[')..str.indexOf(']', str.indexOf('['))))
        var i = 0
        for (index in 0 until json.length()) {
            val pkg = json.getString(index)
            if (HPackages.getPackageInfoOrNull(pkg) != null && HailData.checkedList.all { it.packageName != pkg }) {
                HailData.addCheckedApp(pkg, false)
                i++
            }
        }
        if (i > 0) {
            HailData.checkedList.sortWith(NameComparator)
            HailData.saveApps()
            updateCurrentList()
        }
        HUI.showToast(getString(R.string.msg_imported, i.toString()))
    } catch (t: Throwable) {
    }

    private fun removeCheckedApp(packageName: String, saveApps: Boolean = true) {
        HailData.removeCheckedApp(packageName, saveApps)
        if (saveApps) updateCurrentList()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_multiselect -> {
                multiselect = !multiselect
                item.icon.setTint(
                    MaterialColors.getColor(
                        activity.findViewById(R.id.toolbar),
                        if (multiselect) R.attr.colorPrimary else R.attr.colorOnSurface
                    )
                )
            }
            R.id.action_freeze_current -> setListFrozen(true, HomeAdapter.currentList)
            R.id.action_unfreeze_current -> setListFrozen(false, HomeAdapter.currentList)
            R.id.action_freeze_all -> setListFrozen(true)
            R.id.action_unfreeze_all -> setListFrozen(false)
            R.id.action_import_clipboard -> importFromClipboard()
            R.id.pin_freeze_all -> addPinShortcut(
                AppCompatResources.getDrawable(
                    app,
                    R.drawable.ic_round_frozen_shortcut
                )!!,
                HailApi.ACTION_FREEZE_ALL,
                getString(R.string.action_freeze_all),
                Intent(HailApi.ACTION_FREEZE_ALL)
            )
            R.id.pin_unfreeze_all -> addPinShortcut(
                AppCompatResources.getDrawable(
                    app,
                    R.drawable.ic_round_frozen_shortcut
                )!!,
                HailApi.ACTION_UNFREEZE_ALL,
                getString(R.string.action_unfreeze_all),
                Intent(HailApi.ACTION_UNFREEZE_ALL)
            )
            R.id.pin_lock -> addPinShortcut(
                AppCompatResources.getDrawable(
                    app,
                    R.drawable.ic_outline_lock_shortcut
                )!!,
                HailApi.ACTION_LOCK,
                getString(R.string.action_lock),
                Intent(HailApi.ACTION_LOCK)
            )
            R.id.pin_lock_freeze -> addPinShortcut(
                AppCompatResources.getDrawable(
                    app,
                    R.drawable.ic_outline_lock_shortcut
                )!!,
                HailApi.ACTION_LOCK_FREEZE,
                getString(R.string.action_lock_freeze),
                Intent(HailApi.ACTION_LOCK_FREEZE)
            )
            R.id.action_clear_dynamic_shortcut -> removeAllDynamicShortcuts()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_home, menu)
        (menu.findItem(R.id.action_search).actionView as SearchView).setOnQueryTextListener(object :
            SearchView.OnQueryTextListener {
            private var once = false
            override fun onQueryTextChange(newText: String): Boolean {
                if (once) {
                    query = newText
                    binding.tabs.visibility =
                        if (query.isEmpty() && binding.tabs.tabCount > 1) View.VISIBLE else View.GONE
                    updateCurrentList()
                } else once = true
                return true
            }

            override fun onQueryTextSubmit(query: String): Boolean = true
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}