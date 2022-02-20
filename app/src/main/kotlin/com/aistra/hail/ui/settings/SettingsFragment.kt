package com.aistra.hail.ui.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.aistra.hail.R
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import rikka.shizuku.Shizuku

class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setHasOptionsMenu(true)
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        findPreference<Preference>(HailData.WORKING_MODE)?.onPreferenceChangeListener = this

        findPreference<Preference>(HailData.SKIP_FOREGROUND_APP)?.setOnPreferenceChangeListener { _, value ->
            if (value == true && !HSystem.checkOpUsageStats(requireContext())) {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                false
            } else true
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        when (newValue) {
            HailData.MODE_DO_HIDE -> if (!HPolicy.isDeviceOwnerActive) {
                MaterialAlertDialogBuilder(requireActivity()).setTitle(R.string.title_set_do)
                    .setMessage(getString(R.string.msg_set_do, HPolicy.ADB_SET_DO))
                    .setPositiveButton(android.R.string.ok, null)
                    .setNeutralButton(R.string.action_help) { _, _ -> HUI.openLink(HailData.URL_README) }
                    .create().show()
                return false
            }
            HailData.MODE_SU_DISABLE -> if (!HShell.checkSU) {
                HUI.showToast(R.string.permission_denied)
                return false
            }
            HailData.MODE_SHIZUKU_DISABLE -> return try {
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_help -> HUI.openLink(HailData.URL_README)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_settings, menu)
    }
}