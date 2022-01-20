package com.aistra.hail.app

import android.provider.Settings
import androidx.preference.PreferenceManager
import com.aistra.hail.BuildConfig
import com.aistra.hail.HailApp
import com.aistra.hail.R
import com.aistra.hail.utils.HFiles
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
    const val URL_REDEEM_CODE = "https://aistra0528.github.io/hail/code"
    const val VERSION = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    const val KEY_PACKAGE = "package"
    const val KEY_FROZEN = "frozen"
    const val WORKING_MODE = "working_mode"
    const val MODE_DEFAULT = "default"
    const val MODE_DO_HIDE = "do_hide"
    const val MODE_SU_DISABLE = "su_disable"
    const val MODE_SHIZUKU_DISABLE = "shizuku_disable"
    private const val SORT_BY = "sort_by"
    private const val SORT_NAME = "name"
    const val SORT_INSTALL = "install"
    const val SORT_UPDATE = "update"
    private const val KEY_ID = "id"
    private const val KEY_TAG = "tag"
    private const val KEY_AID = "aid"
    private const val KEY_TAP_TO_SELECT = "tap_to_select"
    private const val SHOW_SYSTEM_APPS = "show_system_apps"
    private const val SHOW_UNFROZEN_APPS = "show_unfrozen_apps"
    private const val AUTO_FREEZE_AFTER_LOCK = "auto_freeze_after_lock"

    private val sp = PreferenceManager.getDefaultSharedPreferences(HailApp.app)
    val workingMode get() = sp.getString(WORKING_MODE, MODE_DEFAULT)
    val sortBy get() = sp.getString(SORT_BY, SORT_NAME)
    val tapToSelect get() = sp.getBoolean(KEY_TAP_TO_SELECT, false)
    val showSystemApps get() = sp.getBoolean(SHOW_SYSTEM_APPS, false)
    val showUnfrozenApps get() = sp.getBoolean(SHOW_UNFROZEN_APPS, true)
    val autoFreezeAfterLock get() = sp.getBoolean(AUTO_FREEZE_AFTER_LOCK, false)

    val isDeviceAid: Boolean get() = sp.getString(KEY_AID, null) == androidId

    fun setAid() = sp.edit().putString(KEY_AID, androidId).apply()

    private val androidId: String
        get() = Settings.System.getString(HailApp.app.contentResolver, Settings.Secure.ANDROID_ID)

    private val dir = "${HailApp.app.filesDir.path}/v1"
    private val appsPath = "$dir/apps.json"
    private val tagsPath = "$dir/tags.json"

    val checkedList: MutableList<AppInfo> by lazy {
        mutableListOf<AppInfo>().apply {
            try {
                val json = JSONArray(HFiles.read(appsPath))
                for (i in 0 until json.length()) {
                    add(with(json.getJSONObject(i)) {
                        AppInfo(getString(KEY_PACKAGE), optInt(KEY_TAG))
                    })
                }
                sortWith(NameComparator)
            } catch (t: Throwable) {
            }
        }
    }

    private fun getCheckedPosition(packageName: String): Int {
        checkedList.forEachIndexed { position, info ->
            if (info.packageName == packageName)
                return position
        }
        return -1
    }

    fun isChecked(packageName: String): Boolean = getCheckedPosition(packageName) != -1

    fun addCheckedApp(packageName: String, sortAndSave: Boolean = true) {
        checkedList.add(AppInfo(packageName, 0))
        if (sortAndSave) {
            checkedList.sortWith(NameComparator)
            saveApps()
        }
    }

    fun removeCheckedApp(packageName: String, saveApps: Boolean = true) {
        checkedList.removeAt(getCheckedPosition(packageName))
        if (saveApps) saveApps()
    }

    fun saveApps() {
        if (!HFiles.exists(dir))
            HFiles.createDirectories(dir)
        HFiles.write(appsPath, JSONArray().run {
            checkedList.forEach {
                put(JSONObject().put(KEY_PACKAGE, it.packageName).put(KEY_TAG, it.tagId))
            }
            toString()
        })
    }

    val tags: MutableList<Pair<String, Int>> by lazy {
        mutableListOf<Pair<String, Int>>().apply {
            try {
                val json = JSONArray(HFiles.read(tagsPath))
                for (i in 0 until json.length()) {
                    add(with(json.getJSONObject(i)) { getString(KEY_TAG) to getInt(KEY_ID) })
                }
            } catch (t: Throwable) {
                add(HailApp.app.getString(R.string.label_default) to 0)
            }
        }
    }

    fun saveTags() {
        if (!HFiles.exists(dir))
            HFiles.createDirectories(dir)
        HFiles.write(tagsPath, JSONArray().run {
            tags.forEach {
                put(JSONObject().put(KEY_TAG, it.first).put(KEY_ID, it.second))
            }
            toString()
        })
    }
}