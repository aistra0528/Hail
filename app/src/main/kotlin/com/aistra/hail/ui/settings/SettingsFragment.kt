package com.aistra.hail.ui.settings

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.aistra.hail.R
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HData
import com.aistra.hail.ui.apps.AppsAdapter
import com.aistra.hail.utils.Shell

class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        findPreference<Preference>(HData.RUNNING_MODE)?.onPreferenceChangeListener = this
        findPreference<Preference>(HData.SHOW_SYSTEM_APPS)?.onPreferenceChangeListener = this
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        when (preference.key) {
            HData.RUNNING_MODE -> {
                when (newValue) {
                    HData.MODE_DO_HIDE -> {
                        if (!AppManager.isDeviceOwnerApp) {
                            AlertDialog.Builder(requireActivity()).run {
                                setTitle(R.string.title_set_do)
                                setMessage(R.string.msg_set_do)
                                setPositiveButton(android.R.string.ok, null)
                                create().show()
                            }
                            return false
                        }
                    }
                    HData.MODE_SU_DISABLE -> {
                        if (!Shell.checkSU) {
                            Toast.makeText(
                                requireActivity(),
                                R.string.permission_denied,
                                Toast.LENGTH_SHORT
                            ).show()
                            return false
                        }
                    }
                }
            }
            HData.SHOW_SYSTEM_APPS -> {
                AppsAdapter.run {
                    updateList(newValue as Boolean, null)
                    notifyDataSetChanged()
                }
            }
        }
        return true
    }
}