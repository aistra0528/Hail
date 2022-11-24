package com.aistra.hail.ui.settings

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.view.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.getSystemService
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.aistra.hail.R
import com.aistra.hail.app.HailApi
import com.aistra.hail.app.HailData
import com.aistra.hail.services.AutoFreezeService
import com.aistra.hail.utils.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import rikka.shizuku.Shizuku

class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener,
    MenuProvider {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val menuHost = requireActivity() as MenuHost
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
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
            val name = ComponentName(requireContext(), AutoFreezeService::class.java.name)
            val isGranted = if (HTarget.O_MR1) {
                requireContext().getSystemService<NotificationManager>()!!
                    .isNotificationListenerAccessGranted(name)
            } else {
                Settings.Secure.getString(
                    requireContext().contentResolver,
                    "enabled_notification_listeners"
                ).split(':').map { ComponentName.unflattenFromString(it) }.contains(name)
            }
            if (value == true && !isGranted) {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                false
            } else true
        }
        findPreference<Preference>(HailData.AUTO_FREEZE_AFTER_LOCK)?.setOnPreferenceChangeListener { _, autoFreezeAfterLock ->
            if ((autoFreezeAfterLock as Boolean).not()) {
                requireContext().stopService(Intent(requireContext(), AutoFreezeService::class.java))
            }
            true
        }
        findPreference<Preference>("add_shortcut")?.setOnPreferenceClickListener {
            addShortcutsToHomeScreen()
            true
        }
        findPreference<Preference>("remove_shortcuts")?.setOnPreferenceClickListener {
            removeShortcutsFromHomeScreen()
            true
        }
    }

    private fun addShortcutsToHomeScreen() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.title_add_shortcut_to_home_screen)
            .setItems(R.array.shortcut_entries) { _, which ->
                when (which) {
                    0 -> HShortcuts.addPinShortcut(
                        AppCompatResources.getDrawable(
                            requireContext(),
                            R.drawable.ic_round_frozen_shortcut
                        )!!,
                        HailApi.ACTION_FREEZE_ALL,
                        getString(R.string.action_freeze_all),
                        Intent(HailApi.ACTION_FREEZE_ALL)
                    )
                    1 -> HShortcuts.addPinShortcut(
                        AppCompatResources.getDrawable(
                            requireContext(),
                            R.drawable.ic_round_frozen_shortcut
                        )!!,
                        HailApi.ACTION_UNFREEZE_ALL,
                        getString(R.string.action_unfreeze_all),
                        Intent(HailApi.ACTION_UNFREEZE_ALL)
                    )
                    2 -> HShortcuts.addPinShortcut(
                        AppCompatResources.getDrawable(
                            requireContext(),
                            R.drawable.ic_outline_lock_shortcut
                        )!!,
                        HailApi.ACTION_LOCK,
                        getString(R.string.action_lock),
                        Intent(HailApi.ACTION_LOCK)
                    )
                    3 -> HShortcuts.addPinShortcut(
                        AppCompatResources.getDrawable(
                            requireContext(),
                            R.drawable.ic_outline_lock_shortcut
                        )!!,
                        HailApi.ACTION_LOCK_FREEZE,
                        getString(R.string.action_lock_freeze),
                        Intent(HailApi.ACTION_LOCK_FREEZE)
                    )
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .show()
    }

    private fun removeShortcutsFromHomeScreen() {
        HShortcuts.removeAllDynamicShortcuts()
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        when (newValue) {
            HailData.MODE_DO_HIDE, HailData.MODE_DO_SUSPEND -> if (!HPolicy.isDeviceOwnerActive) {
                MaterialAlertDialogBuilder(requireActivity()).setTitle(R.string.title_set_do)
                    .setMessage(getString(R.string.msg_set_do, HPolicy.ADB_SET_DO))
                    .setPositiveButton(android.R.string.ok, null)
                    .setNeutralButton(R.string.action_help) { _, _ -> HUI.openLink(HailData.URL_README) }
                    .create().show()
                return false
            }
            HailData.MODE_SU_DISABLE, HailData.MODE_SU_SUSPEND -> if (!HShell.checkSU) {
                HUI.showToast(R.string.permission_denied)
                return false
            }
            HailData.MODE_SHIZUKU_DISABLE, HailData.MODE_SHIZUKU_SUSPEND -> return try {
                when {
                    Shizuku.isPreV11() -> throw IllegalStateException("unsupported shizuku version")
                    Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> true
                    Shizuku.shouldShowRequestPermissionRationale() -> {
                        HUI.showToast(R.string.permission_denied)
                        false
                    }
                    else -> {
                        Shizuku.requestPermission(0)
                        while (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                            SystemClock.sleep(1000)
                        }
                        true
                    }
                }
            } catch (t: Throwable) {
                HLog.e(t)
                HUI.showToast(R.string.shizuku_missing)
                false
            }
        }
        return true
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_help -> HUI.openLink(HailData.URL_README)
        }
        return false
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_settings, menu)
    }
}