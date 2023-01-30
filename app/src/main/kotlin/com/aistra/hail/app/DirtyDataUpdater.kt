package com.aistra.hail.app

import android.content.Context
import androidx.preference.PreferenceManager

object DirtyDataUpdater {
    fun update(context: Context) = HailData.run {
        // This method will be removed in the next release.
        if (workingMode.startsWith("do_")) {
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(WORKING_MODE, workingMode.replace("do_", OWNER)).apply()
        }
    }
}