package com.aistra.hail.app

import android.annotation.SuppressLint
import androidx.preference.PreferenceManager
import com.aistra.hail.HailApp
import com.aistra.hail.ui.home.HomeAdapter
import com.aistra.hail.utils.Files
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object HData {
    private const val KEY_PACKAGE = "package"
    private const val KET_CHECKED = "checked"
    const val RUNNING_MODE = "running_mode"
    const val MODE_DEFAULT = "default"
    const val MODE_DO_HIDE = "do_hide"
    const val MODE_SU_DISABLE = "su_disable"
    const val SHOW_SYSTEM_APPS = "show_system_apps"

    private val sp = PreferenceManager.getDefaultSharedPreferences(HailApp.app)
    val showSystemApps get() = sp.getBoolean(SHOW_SYSTEM_APPS, false)
    val runningMode get() = sp.getString(RUNNING_MODE, MODE_DEFAULT)

    private val arrayPath = "${HailApp.app.filesDir.path}/v1/apps.json"

    private val checkedArray: JSONArray by lazy {
        File(arrayPath).run {
            if (!exists()) {
                parentFile?.mkdirs()
                Files.write(path, JSONArray().toString())
            }
            Files.read(path)?.let {
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

    private fun matchCheckedArray(packageName: String, checked: Boolean): Boolean {
        for (i in 0 until checkedArray.length()) {
            if (checkedArray.getJSONObject(i).getString(KEY_PACKAGE) == packageName) {
                checkedArray.getJSONObject(i).put(KET_CHECKED, checked)
                return true
            }
        }
        return false
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateCheckedList(packageName: String, checked: Boolean) {
        Files.write(arrayPath, checkedArray.toString())
        if (checked) checkedList.add(packageName)
        else checkedList.remove(packageName)
        HomeAdapter.notifyDataSetChanged()
        HLog.i(checkedArray.toString())
    }

    fun addCheckedApp(packageName: String) {
        if (!matchCheckedArray(packageName, true))
            checkedArray.put(JSONObject().put(KEY_PACKAGE, packageName).put(KET_CHECKED, true))
        updateCheckedList(packageName, true)
    }

    fun removeCheckedApp(packageName: String) {
        matchCheckedArray(packageName, false)
        updateCheckedList(packageName, false)
    }

    fun isChecked(packageName: String): Boolean {
        checkedList.forEach {
            if (it == packageName)
                return true
        }
        return false
    }
}