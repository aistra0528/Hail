package com.aistra.hail.app

import androidx.preference.PreferenceManager
import com.aistra.hail.BuildConfig
import com.aistra.hail.HailApp
import com.aistra.hail.utils.HFiles
import com.aistra.hail.utils.HLog
import com.aistra.hail.utils.NameComparator
import org.json.JSONArray
import org.json.JSONObject

object HailData {
    const val URL_WHY_FREE_SOFTWARE =
        "https://www.gnu.org/philosophy/free-software-even-more-important.html"
    const val URL_GITHUB = "https://github.com/aistra0528/Hail"
    const val URL_README = "$URL_GITHUB#readme"
    const val URL_TELEGRAM = "https://t.me/+yvRXYTounDIxODFl"
    const val URL_QQ = "http://qm.qq.com/cgi-bin/qm/qr?k=I2g_Ymanc6bQMo4cVKTG0knARE0twtSG"
    const val URL_COOLAPK = "https://www.coolapk.com/apk/${BuildConfig.APPLICATION_ID}"
    const val URL_ALIPAY = "https://qr.alipay.com/tsx02922ajwj6xekqyd1rbf"
    const val URL_ALIPAY_API = "alipays://platformapi/startapp?saId=10000007&qrcode=$URL_ALIPAY"
    const val URL_BILIBILI = "https://space.bilibili.com/9261272"
    const val URL_PAYPAL = "https://paypal.me/aistra0528"
    const val VERSION = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    const val KEY_PACKAGE = "package"
    const val KEY_FROZEN = "frozen"
    const val WORKING_MODE = "working_mode"
    const val MODE_DEFAULT = "default"
    const val MODE_DO_HIDE = "do_hide"
    const val MODE_SU_DISABLE = "su_disable"
    const val MODE_SHIZUKU_DISABLE = "shizuku_disable"
    private const val SHOW_SYSTEM_APPS = "show_system_apps"
    private const val SHOW_UNFROZEN_APPS = "show_unfrozen_apps"

    private val sp = PreferenceManager.getDefaultSharedPreferences(HailApp.app)
    val workingMode get() = sp.getString(WORKING_MODE, MODE_DEFAULT)
    val showSystemApps get() = sp.getBoolean(SHOW_SYSTEM_APPS, false)
    val showUnfrozenApps get() = sp.getBoolean(SHOW_UNFROZEN_APPS, true)

    private val dir = "${HailApp.app.filesDir.path}/v1"
    private val path = "$dir/apps.json"

    private val array by lazy {
        JSONArray(HFiles.read(path) ?: "[]").apply {
            if (!HFiles.exists(dir))
                HFiles.createDirectories(dir)
            if (!HFiles.exists(path))
                HFiles.write(path, toString())
        }
    }

    val checkedList: MutableList<AppInfo> by lazy {
        mutableListOf<AppInfo>().apply {
            for (i in 0 until array.length()) {
                with(array.getJSONObject(i)) {
                    add(AppInfo(getString(KEY_PACKAGE)))
                }
            }
            sortWith(NameComparator)
        }
    }

    private fun getArrayIndex(packageName: String): Int {
        for (i in 0 until array.length()) {
            if (array.getJSONObject(i).getString(KEY_PACKAGE) == packageName)
                return i
        }
        return -1
    }

    private fun getCheckedPosition(packageName: String): Int {
        checkedList.forEachIndexed { position, info ->
            if (info.packageName == packageName)
                return position
        }
        return -1
    }

    fun isChecked(packageName: String): Boolean = getCheckedPosition(packageName) != -1

    fun addCheckedApp(packageName: String) {
        array.put(JSONObject().put(KEY_PACKAGE, packageName))
        checkedList.add(AppInfo(packageName))
        checkedList.sortWith(NameComparator)
        HFiles.write(path, array.toString())
        HLog.i(array.toString())
    }

    fun removeCheckedApp(packageName: String): Int = getCheckedPosition(packageName).also {
        array.remove(getArrayIndex(packageName))
        checkedList.removeAt(it)
        HFiles.write(path, array.toString())
        HLog.i(array.toString())
    }
}