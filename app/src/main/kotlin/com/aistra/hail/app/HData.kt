package com.aistra.hail.app

import androidx.preference.PreferenceManager
import com.aistra.hail.HailApp
import com.aistra.hail.ui.home.HomeFragment
import com.aistra.hail.util.FilesCompat
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object HData {
    private const val KEY_PACKAGE = "package"
    private const val KET_CHECKED = "checked"
    const val RUNNING_MODE = "running_mode"
    private const val MODE_DEFAULT = "default"
    const val MODE_DPM_HIDE = "dpm_hide"
    private const val SHOW_SYSTEM_APPS = "show_system_apps"

    private val sp = PreferenceManager.getDefaultSharedPreferences(HailApp.app)

    val showSystemApps get() = sp.getBoolean(SHOW_SYSTEM_APPS, false)
    val runningMode get() = sp.getString(RUNNING_MODE, MODE_DEFAULT)
    private val arrayPath get() = "${HailApp.app.filesDir.path}/v1/apps.json"
    private val checkedArray: JSONArray by lazy {
        File(arrayPath).run {
            if (!exists()) {
                parentFile?.mkdirs()
                FilesCompat.writeK(JSONArray().toString(), path)
            }
            FilesCompat.readK(path)?.let {
                return@lazy JSONArray(it)
            }
            JSONArray()
        }
    }

    val checkedList: MutableList<String> by lazy {
        mutableListOf<String>().apply {
            for (i in 0 until checkedArray.length()) {
                if (checkedArray.getJSONObject(i).getBoolean(KET_CHECKED))
                    add(checkedArray.getJSONObject(i).getString(KEY_PACKAGE))
            }
        }
    }

    fun addCheckedApp(packageName: String) {
        for (i in 0 until checkedArray.length()) {
            if (checkedArray.getJSONObject(i).getString(KEY_PACKAGE) == packageName) {
                checkedArray.getJSONObject(i).put(KET_CHECKED, true)
                FilesCompat.writeK(checkedArray.toString(), arrayPath)
                checkedList.add(packageName)
                HomeFragment.adapter?.notifyDataSetChanged()
                HLog.i(checkedArray.toString())
                return
            }
        }
        checkedArray.put(JSONObject().put(KEY_PACKAGE, packageName).put(KET_CHECKED, true))
        FilesCompat.writeK(checkedArray.toString(), arrayPath)
        checkedList.add(packageName)
        HomeFragment.adapter?.notifyDataSetChanged()
        HLog.i(checkedArray.toString())
    }

    fun removeCheckedApp(packageName: String) {
        for (i in 0 until checkedArray.length()) {
            if (checkedArray.getJSONObject(i).getString(KEY_PACKAGE) == packageName) {
                checkedArray.getJSONObject(i).put(KET_CHECKED, false)
                FilesCompat.writeK(checkedArray.toString(), arrayPath)
                checkedList.remove(packageName)
                HomeFragment.adapter?.notifyDataSetChanged()
                HLog.i(checkedArray.toString())
                return
            }
        }
    }

    fun isChecked(packageName: String): Boolean {
        for (i in checkedList) {
            if (i == packageName)
                return true
        }
        return false
    }
}