package com.aistra.hail.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import com.aistra.hail.R
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HData

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        findPreference<ListPreference>(HData.RUNNING_MODE)?.setOnPreferenceChangeListener { _, newValue ->
            when (newValue) {
                HData.MODE_DPM_HIDE -> {
                    if (!AppManager.isDeviceOwnerApp) {
                        AlertDialog.Builder(requireActivity()).run {
                            setTitle(R.string.title_set_dpm)
                            setMessage(R.string.msg_set_dpm)
                            setPositiveButton(android.R.string.ok, null)
                            create().show()
                        }
                    }
                }
            }
            true
        }
    }
}