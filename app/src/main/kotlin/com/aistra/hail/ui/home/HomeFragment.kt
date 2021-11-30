package com.aistra.hail.ui.home

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.*
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.aistra.hail.R
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailApi
import com.aistra.hail.app.HailData
import com.aistra.hail.ui.main.MainFragment
import com.aistra.hail.utils.HUI
import com.aistra.hail.work.HWork
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class HomeFragment : MainFragment(), HomeAdapter.OnItemClickListener,
    HomeAdapter.OnItemLongClickListener {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        return SwipeRefreshLayout(activity).apply {
            addView(RecyclerView(activity).apply {
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
            })
            setOnRefreshListener {
                HomeAdapter.notifyItemRangeChanged(0, HomeAdapter.itemCount)
                isRefreshing = false
            }
        }
    }

    override fun onStart() {
        super.onStart()
        activity.fab.run {
            if (hasOnClickListeners()) HomeAdapter.notifyItemRangeChanged(0, HomeAdapter.itemCount)
            else setOnClickListener { setAllFrozen(true) }
        }
    }

    override fun onItemClick(position: Int) {
        val info = HailData.checkedList[position]
        if (info.applicationInfo == null) {
            Snackbar.make(activity.fab, R.string.app_not_installed, Snackbar.LENGTH_LONG)
                .setAction(R.string.action_remove_home) { removeCheckedApp(position) }.show()
            return
        }
        val pkg = info.packageName
        if (AppManager.isAppFrozen(pkg) && AppManager.setAppFrozen(pkg, false))
            HomeAdapter.notifyItemChanged(position)
        app.packageManager.getLaunchIntentForPackage(pkg)?.let {
            startActivity(it)
        }
    }

    override fun onItemLongClick(position: Int): Boolean {
        val info = HailData.checkedList[position]
        info.applicationInfo ?: return false
        val pkg = info.packageName
        val frozen = AppManager.isAppFrozen(pkg)
        val action = getString(if (frozen) R.string.action_unfreeze else R.string.action_freeze)
        MaterialAlertDialogBuilder(activity).setTitle(info.name).setItems(
            arrayOf(
                action,
                getString(R.string.action_deferred_task),
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
                2 -> createShortcut(
                    info.icon, pkg, info.name,
                    HailApi.getIntentForPackage(HailApi.ACTION_LAUNCH, pkg)
                )
                3 -> removeCheckedApp(position)
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
                    HailData.checkedList[position].name
                )
            }
            pkg == app.packageName -> getString(R.string.app_slogan)
            else -> getString(R.string.permission_denied)
        }
    )

    private fun setAllFrozen(frozen: Boolean) =
        HailData.checkedList.forEachIndexed { position, info ->
            if (AppManager.isAppFrozen(info.packageName) != frozen) {
                if (AppManager.setAppFrozen(info.packageName, frozen))
                    HomeAdapter.notifyItemChanged(position)
                else if (info.packageName != app.packageName && info.applicationInfo != null) {
                    HUI.showToast(R.string.permission_denied)
                    return
                }
            }
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

    private fun removeCheckedApp(position: Int) = HomeAdapter.notifyItemRemoved(
        HailData.removeCheckedApp(HailData.checkedList[position].packageName)
    )

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
                        }
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .create().show()
            }
            R.id.action_unfreeze_all -> setAllFrozen(false)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_home, menu)
    }
}