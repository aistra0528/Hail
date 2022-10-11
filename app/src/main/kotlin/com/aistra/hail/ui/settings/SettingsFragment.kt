package com.aistra.hail.ui.settings

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.view.*
import androidx.core.content.getSystemService
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.aistra.hail.R
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
            val notificationManager = requireContext().getSystemService<NotificationManager>()!!
            if (value == true && !notificationManager.isNotificationListenerAccessGranted(
                    ComponentName(requireContext(), AutoFreezeService::class.java.name)
                )
            ) {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                false
            } else true
        }
        findPreference<MultiSelectListPreference>(HailData.AUTO_FREEZE_TAGS)?.apply {
            val tagFirsts = mutableListOf<CharSequence>()
            val tagSeconds = mutableListOf<CharSequence>()
            HailData.tags.forEach { tag ->
                tagFirsts.add(tag.first)
                tagSeconds.add(tag.second.toString())
            }
            entries = tagFirsts.toTypedArray()
            entryValues = tagSeconds.toTypedArray()
            values = HailData.autoFreezeTags // This sets the "checked" boxes and is actually only necessary the first time it runs. If not, the "default" tag wouldn't be selected. I can't think of a nice way of just running this the first time.
            setSummaryFromValues(HailData.autoFreezeTags)
            setOnPreferenceChangeListener { _, value ->
                setSummaryFromValues(value as Set<String>)
                true
            }
        }
    }

    private fun MultiSelectListPreference.setSummaryFromValues(values: Set<String>) {
        summary = values.joinToString(", ") { entries[findIndexOfValue(it)] }
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