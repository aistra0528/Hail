package com.aistra.hail.ui.apps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.aistra.hail.R
import com.aistra.hail.app.HailData
import com.aistra.hail.databinding.FragmentAppsBinding
import com.aistra.hail.extensions.*
import com.aistra.hail.ui.main.MainFragment
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HUI
import com.aistra.hail.utils.NameComparator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DisabledAppsFragment : MainFragment(), MenuProvider {
    private var _binding: FragmentAppsBinding? = null
    private val binding get() = _binding!!
    private val selectedPackages = linkedSetOf<String>()
    private lateinit var appsAdapter: DisabledAppsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        (requireActivity() as MenuHost).addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
        _binding = FragmentAppsBinding.inflate(inflater, container, false)
        appsAdapter = DisabledAppsAdapter(selectedPackages, ::onSelectionChanged)
        binding.refresh.apply {
            setOnRefreshListener(::refreshApps)
            applyDefaultInsetter { marginRelative(isRtl, start = !isLandscape, end = true) }
        }
        binding.recyclerView.apply {
            activity.appbar.setLiftOnScrollTargetView(this)
            layoutManager = GridLayoutManager(activity, resources.getInteger(R.integer.apps_span))
            adapter = appsAdapter
            applyDefaultInsetter { paddingRelative(isRtl, bottom = isLandscape) }
        }
        refreshApps()
        return binding.root
    }

    private fun refreshApps() {
        binding.refresh.isRefreshing = true
        lifecycleScope.launch {
            val apps = withContext(Dispatchers.Default) {
                HPackages.getInstalledApplications()
                    .filter {
                        HPackages.isAppExplicitlyDisabled(it.packageName) && !HailData.isChecked(it.packageName)
                    }
                    .sortedWith(NameComparator)
            }
            selectedPackages.retainAll(apps.mapTo(mutableSetOf()) { it.packageName })
            appsAdapter.submitList(apps)
            binding.refresh.isRefreshing = false
            requireActivity().invalidateMenu()
        }
    }

    private fun onSelectionChanged(packageName: String, selected: Boolean) {
        if (selected) selectedPackages.add(packageName) else selectedPackages.remove(packageName)
        requireActivity().invalidateMenu()
    }

    private fun selectAllOrClear() {
        val packages = appsAdapter.currentList.mapTo(mutableSetOf()) { it.packageName }
        if (selectedPackages.containsAll(packages)) selectedPackages.clear() else selectedPackages.addAll(packages)
        appsAdapter.notifyDataSetChanged()
        requireActivity().invalidateMenu()
    }

    private fun selectTagsAndImport() {
        if (selectedPackages.isEmpty()) return
        val tags = HailData.tags.toList()
        val checkedTags = BooleanArray(tags.size)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.action_tag_set)
            .setMultiChoiceItems(tags.map { it.first }.toTypedArray(), checkedTags) { _, index, checked ->
                checkedTags[index] = checked
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val tagIds = tags.indices.filter { checkedTags[it] }.map { tags[it].second }
                if (tagIds.isEmpty()) {
                    HUI.showToast(R.string.msg_select_tag)
                    return@setPositiveButton
                }
                importApps(tagIds)
            }
            .show()
    }

    private fun importApps(tagIds: List<Int>) {
        val packages = selectedPackages.toList()
        var added = 0
        packages.forEach { packageName ->
            if (HailData.isChecked(packageName)) return@forEach
            HailData.addCheckedApp(packageName, tagIds.first(), false)
            HailData.checkedList.first { it.packageName == packageName }.tagIdList.addAll(tagIds.drop(1))
            added++
        }
        if (added > 0) HailData.saveApps()
        HUI.showToast(getString(R.string.msg_imported, added.toString()))
        refreshApps()
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_disabled_apps, menu)
    }

    override fun onPrepareMenu(menu: Menu) {
        menu.findItem(R.id.action_import_disabled_apps).isEnabled = selectedPackages.isNotEmpty()
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_select_all -> {
            selectAllOrClear()
            true
        }

        R.id.action_import_disabled_apps -> {
            selectTagsAndImport()
            true
        }

        else -> false
    }

    override fun onDestroyView() {
        appsAdapter.onDestroy()
        _binding = null
        super.onDestroyView()
    }
}
