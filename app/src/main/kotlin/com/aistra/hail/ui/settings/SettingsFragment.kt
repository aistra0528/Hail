package com.aistra.hail.ui.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.R
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailApi
import com.aistra.hail.app.HailData
import com.aistra.hail.databinding.DialogInputBinding
import com.aistra.hail.ui.main.MainActivity
import com.aistra.hail.utils.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku


class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener,
    MenuProvider {
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val menuHost = requireActivity() as MenuHost
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
        (requireActivity() as MainActivity).appbar.liftOnScrollTargetViewId = R.id.recycler_view

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        findPreference<Preference>(HailData.WORKING_MODE)?.onPreferenceChangeListener = this
        findPreference<Preference>(HailData.SKIP_FOREGROUND_APP)?.setOnPreferenceChangeListener { _, value ->
            if (value == true && !HSystem.checkOpUsageStats(requireContext())) {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                false
            } else true
        }
        findPreference<Preference>(HailData.SKIP_NOTIFYING_APP)?.setOnPreferenceChangeListener { _, value ->
            val isGranted = NotificationManagerCompat.getEnabledListenerPackages(requireContext())
                .contains(requireContext().packageName)
            if (value == true && !isGranted) {
                app.setAutoFreezeServiceEnabled(true)
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                false
            } else true
        }
        findPreference<Preference>(HailData.AUTO_FREEZE_AFTER_LOCK)?.setOnPreferenceChangeListener { _, autoFreezeAfterLock ->
            if (autoFreezeAfterLock == false) {
                app.setAutoFreezeService(false)
            }
            true
        }
        findPreference<Preference>(HailData.ICON_PACK)?.setOnPreferenceClickListener {
            iconPackDialog()
            true
        }
        findPreference<Preference>("add_pin_shortcut")?.setOnPreferenceClickListener {
            addPinShortcut()
            true
        }
        findPreference<Preference>(HailData.DYNAMIC_SHORTCUT_ACTION)?.setOnPreferenceChangeListener { _, action ->
            HShortcuts.removeAllDynamicShortcuts()
            HShortcuts.addDynamicShortcutAction(action as String)
            true
        }
        findPreference<Preference>("clear_dynamic_shortcuts")?.setOnPreferenceClickListener {
            HShortcuts.removeAllDynamicShortcuts()
            HShortcuts.addDynamicShortcutAction(HailData.dynamicShortcutAction)
            true
        }
    }

    @Suppress("DEPRECATION")
    private fun iconPackDialog() {
        val list = Intent(Intent.ACTION_MAIN).addCategory("com.anddoes.launcher.THEME").let {
            if (HTarget.T) app.packageManager.queryIntentActivities(
                it, PackageManager.ResolveInfoFlags.of(0)
            ) else app.packageManager.queryIntentActivities(it, 0)
        }.map { it.activityInfo }
        if (list.isEmpty()) {
            HUI.showToast(R.string.app_not_installed)
            return
        }
        MaterialAlertDialogBuilder(requireActivity()).setTitle(R.string.icon_pack)
            .setItems(list.map { it.loadLabel(app.packageManager) }.toTypedArray()) { _, which ->
                if (HailData.iconPack == list[which].packageName) return@setItems
                HailData.setIconPack(list[which].packageName)
                AppIconCache.clear()
            }.setNeutralButton(R.string.label_default) { _, _ ->
                if (HailData.iconPack == HailData.ACTION_NONE) return@setNeutralButton
                HailData.setIconPack(HailData.ACTION_NONE)
                AppIconCache.clear()
            }.setNegativeButton(android.R.string.cancel, null).show()
    }

    private fun addPinShortcut() {
        MaterialAlertDialogBuilder(requireActivity()).setTitle(R.string.action_add_pin_shortcut)
            .setItems(R.array.pin_shortcut_entries) { _, which ->
                when (which) {
                    0 -> MaterialAlertDialogBuilder(requireActivity()).setTitle(R.string.action_freeze_tag)
                        .setItems(HailData.tags.map { it.first }.toTypedArray()) { _, index ->
                            val tag = HailData.tags[index].first
                            HShortcuts.addPinShortcut(
                                AppCompatResources.getDrawable(
                                    requireContext(), R.drawable.ic_round_frozen_shortcut
                                )!!,
                                HailApi.ACTION_FREEZE_TAG + tag,
                                tag,
                                HailApi.getIntentForTag(HailApi.ACTION_FREEZE_TAG, tag)
                            )
                        }.setNegativeButton(android.R.string.cancel, null).show()

                    1 -> MaterialAlertDialogBuilder(requireActivity()).setTitle(R.string.action_unfreeze_tag)
                        .setItems(HailData.tags.map { it.first }.toTypedArray()) { _, index ->
                            val tag = HailData.tags[index].first
                            HShortcuts.addPinShortcut(
                                AppCompatResources.getDrawable(
                                    requireContext(), R.drawable.ic_round_unfrozen_shortcut
                                )!!,
                                HailApi.ACTION_UNFREEZE_TAG + tag,
                                tag,
                                HailApi.getIntentForTag(HailApi.ACTION_UNFREEZE_TAG, tag)
                            )
                        }.setNegativeButton(android.R.string.cancel, null).show()

                    2 -> HShortcuts.addPinShortcut(
                        AppCompatResources.getDrawable(
                            requireContext(), R.drawable.ic_round_frozen_shortcut
                        )!!,
                        HailApi.ACTION_FREEZE_ALL,
                        getString(R.string.action_freeze_all),
                        Intent(HailApi.ACTION_FREEZE_ALL)
                    )

                    3 -> HShortcuts.addPinShortcut(
                        AppCompatResources.getDrawable(
                            requireContext(), R.drawable.ic_round_unfrozen_shortcut
                        )!!,
                        HailApi.ACTION_UNFREEZE_ALL,
                        getString(R.string.action_unfreeze_all),
                        Intent(HailApi.ACTION_UNFREEZE_ALL)
                    )

                    4 -> HShortcuts.addPinShortcut(
                        AppCompatResources.getDrawable(
                            requireContext(), R.drawable.ic_round_frozen_shortcut
                        )!!,
                        HailApi.ACTION_FREEZE_NON_WHITELISTED,
                        getString(R.string.action_freeze_non_whitelisted),
                        Intent(HailApi.ACTION_FREEZE_NON_WHITELISTED)
                    )

                    5 -> HShortcuts.addPinShortcut(
                        AppCompatResources.getDrawable(
                            requireContext(), R.drawable.ic_outline_lock_shortcut
                        )!!,
                        HailApi.ACTION_LOCK,
                        getString(R.string.action_lock),
                        Intent(HailApi.ACTION_LOCK)
                    )

                    6 -> HShortcuts.addPinShortcut(
                        AppCompatResources.getDrawable(
                            requireContext(), R.drawable.ic_outline_lock_shortcut
                        )!!,
                        HailApi.ACTION_LOCK_FREEZE,
                        getString(R.string.action_lock_freeze),
                        Intent(HailApi.ACTION_LOCK_FREEZE)
                    )
                }
            }.setNegativeButton(android.R.string.cancel, null).show()
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        // Show/hide terminal menu.
        activity?.invalidateOptionsMenu()
        val mode = newValue as String
        when {
            mode.startsWith(HailData.OWNER) -> if (!HPolicy.isDeviceOwnerActive) {
                MaterialAlertDialogBuilder(requireActivity()).setTitle(R.string.title_set_owner)
                    .setMessage(getString(R.string.msg_set_owner, HPolicy.ADB_COMMAND))
                    .setPositiveButton(android.R.string.ok, null)
                    .setNeutralButton(android.R.string.copy) { _, _ -> HUI.copyText(HPolicy.ADB_COMMAND) }
                    .show()
                    .findViewById<MaterialTextView>(android.R.id.message)?.setTextIsSelectable(true)
                return false
            }

            mode.startsWith(HailData.DHIZUKU) -> return runCatching {
                Dhizuku.init(app)
                when {
                    Dhizuku.isPermissionGranted() -> true
                    else -> {
                        lifecycleScope.launch {
                            val result = callbackFlow {
                                Dhizuku.requestPermission(object :
                                    DhizukuRequestPermissionListener() {
                                    override fun onRequestPermission(grantResult: Int) {
                                        trySendBlocking(grantResult == PackageManager.PERMISSION_GRANTED)
                                    }
                                })
                                awaitClose()
                            }.first()
                            if (result && preference is ListPreference) {
                                preference.value = mode
                                if (HTarget.O) HDhizuku.setDelegatedScopes()
                            }
                        }
                        false
                    }
                }
            }.getOrElse {
                HLog.e(it)
                HUI.showToast(R.string.permission_denied)
                false
            }

            mode.startsWith(HailData.SU) -> if (!HShell.checkSU) {
                HUI.showToast(R.string.permission_denied)
                return false
            }

            mode.startsWith(HailData.SHIZUKU) -> return runCatching {
                when {
                    Shizuku.isPreV11() -> throw IllegalStateException("unsupported shizuku version")
                    Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> true
                    Shizuku.shouldShowRequestPermissionRationale() -> {
                        HUI.showToast(R.string.permission_denied)
                        false
                    }

                    else -> {
                        lifecycleScope.launch {
                            val result = callbackFlow {
                                val shizukuRequestCode = 0
                                val listener =
                                    Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
                                        if (requestCode != shizukuRequestCode) return@OnRequestPermissionResultListener
                                        trySendBlocking(grantResult == PackageManager.PERMISSION_GRANTED)
                                    }
                                Shizuku.addRequestPermissionResultListener(listener)
                                Shizuku.requestPermission(shizukuRequestCode)
                                awaitClose {
                                    Shizuku.removeRequestPermissionResultListener(listener)
                                }
                            }.first()
                            if (result && preference is ListPreference) preference.value = mode
                        }
                        false
                    }
                }
            }.getOrElse {
                HLog.e(it)
                HUI.showToast(R.string.shizuku_missing)
                false
            }

            mode.startsWith(HailData.ISLAND) -> return runCatching{
                when{
                    mode == HailData.MODE_ISLAND_HIDE && HIsland.freezePermissionGranted() -> true
                    mode == HailData.MODE_ISLAND_SUSPEND && HIsland.suspendPermissionGranted() -> true
                    else -> {
                        lifecycleScope.launch {
                            requestPermissionLauncher.launch(if(mode == HailData.MODE_ISLAND_HIDE)
                                HIsland.PERMISSION_FREEZE_PACKAGE
                            else
                                HIsland.PERMISSION_SUSPEND_PACKAGE
                            )
                        }
                        false
                    }
                }
            }.getOrElse {
                HLog.e(it)
                HUI.showToast(R.string.permission_denied)
                false
            }
        }

        return true
    }

    private suspend fun onTerminalResult(exitValue: Int, msg: String?) =
        withContext(Dispatchers.Main) {
            if (exitValue == 0 && msg.isNullOrBlank()) return@withContext
            MaterialAlertDialogBuilder(requireActivity()).apply {
                if (!msg.isNullOrBlank()) {
                    if (exitValue != 0) {
                        setTitle(getString(R.string.operation_failed, exitValue.toString()))
                    }
                    setMessage(msg)
                    setNeutralButton(android.R.string.copy) { _, _ -> HUI.copyText(msg) }
                } else if (exitValue != 0) {
                    setMessage(getString(R.string.operation_failed, exitValue.toString()))
                }
            }.setPositiveButton(android.R.string.ok, null).show()
                .findViewById<MaterialTextView>(android.R.id.message)?.setTextIsSelectable(true)
        }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_terminal -> {
                val input =
                    DialogInputBinding.inflate(layoutInflater, FrameLayout(requireActivity()), true)
                input.inputLayout.setHint(R.string.action_terminal)
                input.editText.run {
                    setSingleLine()
                    filters = arrayOf()
                }
                MaterialAlertDialogBuilder(requireActivity()).setView(input.root.parent as View)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        lifecycleScope.launch {
                            val result = AppManager.execute(input.editText.text.toString())
                            onTerminalResult(result.first, result.second)
                        }
                    }.setNegativeButton(android.R.string.cancel, null).show()
            }

            R.id.action_remove_owner -> (requireActivity() as MainActivity).ownerRemoveDialog()
            R.id.action_help -> HUI.openLink(HailData.URL_README)
        }
        return false
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_settings, menu)
    }

    override fun onPrepareMenu(menu: Menu) {
        super.onPrepareMenu(menu)
        if (HailData.workingMode.startsWith(HailData.SU) || HailData.workingMode.startsWith(HailData.SHIZUKU))
            menu.findItem(R.id.action_terminal).isVisible = true
        else if (HPolicy.isDeviceOwnerActive)
            menu.findItem(R.id.action_remove_owner).isVisible = true
    }
}