package com.aistra.hail.ui.apps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.widget.doAfterTextChanged
import com.aistra.hail.R
import com.aistra.hail.app.HailData
import com.aistra.hail.app.HailData.LaunchAssociation
import com.aistra.hail.ui.main.MainFragment
import com.aistra.hail.ui.theme.AppTheme
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HUI
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class LaunchAssociationsFragment : MainFragment() {
    private var refreshKey by mutableIntStateOf(0)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            AppTheme {
                LaunchAssociationsScreen(
                    refreshKey = refreshKey,
                    onAdd = { showAssociationDialog() },
                    onEdit = ::showAssociationDialog,
                    onDelete = ::deleteAssociation,
                )
            }
        }
    }

    private fun showAssociationDialog(existing: LaunchAssociation? = null) {
        val managedApps = HailData.checkedList.toList()
        if (managedApps.size < 2) {
            HUI.showToast(R.string.launch_association_not_enough_apps)
            return
        }
        var primaryPackageName = existing?.primaryPackageName ?: managedApps.first().packageName
        val selector = searchableSelector(
            apps = managedApps,
            selectedPackages = setOf(primaryPackageName),
            multiple = false,
        ) { selected -> primaryPackageName = selected.firstOrNull() ?: primaryPackageName }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.launch_association_primary)
            .setView(selector)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                showSecondaryDialog(primaryPackageName, existing)
            }
            .show()
    }

    private fun showSecondaryDialog(primaryPackageName: String, existing: LaunchAssociation?) {
        val candidates = HailData.checkedList.filter { it.packageName != primaryPackageName }
        val selectedPackages = existing?.secondaryPackageNames?.toMutableSet() ?: mutableSetOf()
        val selector = searchableSelector(candidates, selectedPackages, multiple = true) { selected ->
            selectedPackages.clear()
            selectedPackages.addAll(selected)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.launch_association_secondary)
            .setView(selector)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                if (selectedPackages.isEmpty()) {
                    HUI.showToast(R.string.launch_association_select_secondary)
                    return@setPositiveButton
                }
                if (existing != null && existing.primaryPackageName != primaryPackageName) {
                    HailData.removeLaunchAssociation(existing.primaryPackageName)
                }
                HailData.setLaunchAssociation(primaryPackageName, selectedPackages.toList())
                refreshKey++
            }
            .show()
    }

    private fun searchableSelector(
        apps: List<com.aistra.hail.app.AppInfo>,
        selectedPackages: Set<String>,
        multiple: Boolean,
        onSelectionChanged: (Set<String>) -> Unit,
    ): View {
        val context = requireContext()
        val selection = selectedPackages.toMutableSet()
        val search = EditText(context).apply {
            hint = getString(R.string.search)
            setSingleLine(true)
        }
        val list = ListView(context).apply {
            choiceMode = if (multiple) ListView.CHOICE_MODE_MULTIPLE else ListView.CHOICE_MODE_SINGLE
        }
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 0, 48, 0)
            addView(search, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 720))
        }
        var visibleApps = apps
        fun refresh(query: String) {
            visibleApps = apps.filter { appInfo ->
                val name = appName(appInfo.packageName)
                name.contains(query, ignoreCase = true) || appInfo.packageName.contains(query, ignoreCase = true)
            }
            list.adapter = ArrayAdapter(
                context,
                if (multiple) android.R.layout.simple_list_item_multiple_choice else android.R.layout.simple_list_item_single_choice,
                visibleApps.map { "${appName(it.packageName)}\n${it.packageName}" },
            )
            visibleApps.forEachIndexed { index, appInfo ->
                list.setItemChecked(index, appInfo.packageName in selection)
            }
        }
        list.setOnItemClickListener { _, _, position, _ ->
            val packageName = visibleApps[position].packageName
            if (multiple) {
                if (packageName in selection) selection.remove(packageName) else selection.add(packageName)
            } else {
                selection.clear()
                selection.add(packageName)
            }
            onSelectionChanged(selection)
        }
        search.doAfterTextChanged { refresh(it?.toString().orEmpty()) }
        refresh("")
        return container
    }

    private fun deleteAssociation(association: LaunchAssociation) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(getString(R.string.launch_association_delete_message, appName(association.primaryPackageName)))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                HailData.removeLaunchAssociation(association.primaryPackageName)
                refreshKey++
            }
            .show()
    }

    private fun appName(packageName: String): String = HPackages.getApplicationInfoOrNull(packageName)
        ?.loadLabel(requireContext().packageManager)?.toString() ?: packageName
}

@Composable
private fun LaunchAssociationsScreen(
    refreshKey: Int,
    onAdd: () -> Unit,
    onEdit: (LaunchAssociation) -> Unit,
    onDelete: (LaunchAssociation) -> Unit,
) {
    val context = LocalContext.current
    val associations = remember(refreshKey) { HailData.launchAssociations.toList() }
    val appNames = remember(refreshKey) {
        HailData.checkedList.associate { appInfo ->
            appInfo.packageName to (HPackages.getApplicationInfoOrNull(appInfo.packageName)
                ?.loadLabel(context.packageManager)?.toString() ?: appInfo.packageName)
        }
    }
    if (associations.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Outlined.Link, contentDescription = null)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.launch_association_empty), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.launch_association_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = onAdd) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_add_launch_association))
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.launch_association_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                Button(onClick = onAdd, modifier = Modifier.padding(top = 12.dp, start = 8.dp)) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_add_launch_association))
                }
            }
            items(associations, key = { it.primaryPackageName }) { association ->
                Card(onClick = { onEdit(association) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                            Text(appNames[association.primaryPackageName] ?: association.primaryPackageName)
                            Text(
                                association.secondaryPackageNames.joinToString { appNames[it] ?: it },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { onDelete(association) }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                }
            }
        }
    }
}
