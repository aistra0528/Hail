package com.aistra.hail.app

import androidx.preference.PreferenceManager
import com.aistra.hail.BuildConfig
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.R
import com.aistra.hail.utils.HFiles
import org.json.JSONArray
import org.json.JSONObject

object HailData {
    const val URL_WHY_FREE_SOFTWARE = "https://www.gnu.org/philosophy/free-software-even-more-important.html"
    const val URL_GITHUB = "https://github.com/aistra0528/Hail"
    const val URL_README = "$URL_GITHUB#readme"
    const val URL_RELEASES = "$URL_GITHUB/releases"
    const val URL_TELEGRAM = "https://t.me/+yvRXYTounDIxODFl"
    const val URL_QQ = "http://qm.qq.com/cgi-bin/qm/qr?k=I2g_Ymanc6bQMo4cVKTG0knARE0twtSG"
    const val URL_FDROID = "https://f-droid.org/packages/${BuildConfig.APPLICATION_ID}"
    const val URL_ALIPAY = "https://qr.alipay.com/tsx02922ajwj6xekqyd1rbf"
    const val URL_ALIPAY_API = "alipays://platformapi/startapp?saId=10000007&qrcode=$URL_ALIPAY"
    const val URL_BILIBILI = "https://space.bilibili.com/9261272"
    const val URL_LIBERAPAY = "https://liberapay.com/aistra0528"
    const val URL_PAYPAL = "https://www.paypal.me/aistra0528"
    const val URL_TRANSLATE = "https://hosted.weblate.org/engage/hail/"
    const val VERSION = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    const val KEY_PACKAGE = "package"
    const val KEY_FROZEN = "frozen"
    const val WORKING_MODE = "working_mode"
    const val MODE_DEFAULT = "default"

    const val OWNER = "owner_"
    const val DHIZUKU = "dhizuku_"
    const val SU = "su_"
    const val SHIZUKU = "shizuku_"
    const val ISLAND = "island_"
    const val PRIVAPP = "privapp_"

    const val STOP = "stop"
    const val DISABLE = "disable"
    const val HIDE = "hide"
    const val SUSPEND = "suspend"

    const val MODE_OWNER_HIDE = OWNER + HIDE
    const val MODE_OWNER_SUSPEND = OWNER + SUSPEND
    const val MODE_DHIZUKU_HIDE = DHIZUKU + HIDE
    const val MODE_DHIZUKU_SUSPEND = DHIZUKU + SUSPEND
    const val MODE_SU_STOP = SU + STOP
    const val MODE_SU_DISABLE = SU + DISABLE
    const val MODE_SU_HIDE = SU + HIDE
    const val MODE_SU_SUSPEND = SU + SUSPEND
    const val MODE_SHIZUKU_STOP = SHIZUKU + STOP
    const val MODE_SHIZUKU_DISABLE = SHIZUKU + DISABLE
    const val MODE_SHIZUKU_HIDE = SHIZUKU + HIDE
    const val MODE_SHIZUKU_SUSPEND = SHIZUKU + SUSPEND
    const val MODE_ISLAND_HIDE = ISLAND + HIDE
    const val MODE_ISLAND_SUSPEND = ISLAND + SUSPEND
    const val MODE_PRIVAPP_STOP = PRIVAPP + STOP
    const val MODE_PRIVAPP_DISABLE = PRIVAPP + DISABLE

    private const val TILE_ACTION = "tile_action"
    private const val HOME_FONT_SIZE = "home_font_size"
    const val DYNAMIC_SHORTCUT_ACTION = "dynamic_shortcut_action"
    const val ACTION_NONE = "none"
    const val ACTION_FREEZE_ALL = "freeze_all"
    const val ACTION_FREEZE_NON_WHITELISTED = "freeze_non_whitelisted"
    const val ACTION_LOCK = "lock"
    const val ACTION_LOCK_FREEZE = "lock_freeze"
    private const val SORT_BY = "sort_by"
    const val SORT_NAME = "name"
    const val SORT_INSTALL = "install"
    const val SORT_UPDATE = "update"
    private const val KEY_ID = "id"
    const val KEY_TAG = "tag"
    private const val KEY_PINNED = "pinned"
    private const val KEY_WHITELISTED = "whitelisted"
    const val FILTER_USER_APPS = "filter_user_apps"
    const val FILTER_SYSTEM_APPS = "filter_system_apps"
    const val FILTER_FROZEN_APPS = "filter_frozen_apps"
    const val FILTER_UNFROZEN_APPS = "filter_unfrozen_apps"
    private const val BIOMETRIC_LOGIN = "biometric_login"
    private const val FUZZY_SEARCH = "fuzzy_search"
    private const val NINE_KEY_SEARCH = "nine_key"
    const val ICON_PACK = "icon_pack"
    private const val GRAYSCALE_ICON = "grayscale_icon"
    private const val COMPACT_ICON = "compact_icon"
    private const val SYNTHESIZE_ADAPTIVE_ICONS = "synthesize_adaptive_icons"
    const val AUTO_FREEZE_AFTER_LOCK = "auto_freeze_after_lock"
    private const val SKIP_WHILE_CHARGING = "skip_while_charging"
    const val SKIP_FOREGROUND_APP = "skip_foreground_app"
    const val SKIP_NOTIFYING_APP = "skip_notifying_app"
    private const val AUTO_FREEZE_DELAY = "auto_freeze_delay"

    private val sp = PreferenceManager.getDefaultSharedPreferences(app)
    val workingMode get() = sp.getString(WORKING_MODE, MODE_DEFAULT)!!
    val sortBy get() = sp.getString(SORT_BY, SORT_NAME)
    val filterUserApps get() = sp.getBoolean(FILTER_USER_APPS, true)
    val filterSystemApps get() = sp.getBoolean(FILTER_SYSTEM_APPS, false)
    val filterFrozenApps get() = sp.getBoolean(FILTER_FROZEN_APPS, true)
    val filterUnfrozenApps get() = sp.getBoolean(FILTER_UNFROZEN_APPS, true)
    val biometricLogin get() = sp.getBoolean(BIOMETRIC_LOGIN, false)
    val grayscaleIcon get() = sp.getBoolean(GRAYSCALE_ICON, true)
    val compactIcon get() = sp.getBoolean(COMPACT_ICON, false)
    val tileAction get() = sp.getString(TILE_ACTION, ACTION_FREEZE_ALL)
    val homeFontSize get() = sp.getInt(HOME_FONT_SIZE, 14)
    val dynamicShortcutAction get() = sp.getString(DYNAMIC_SHORTCUT_ACTION, ACTION_NONE)!!
    val synthesizeAdaptiveIcons get() = sp.getBoolean(SYNTHESIZE_ADAPTIVE_ICONS, false)
    val autoFreezeAfterLock get() = sp.getBoolean(AUTO_FREEZE_AFTER_LOCK, false)
    val skipWhileCharging get() = sp.getBoolean(SKIP_WHILE_CHARGING, false)
    val skipForegroundApp get() = sp.getBoolean(SKIP_FOREGROUND_APP, false)
    val skipNotifyingApp get() = sp.getBoolean(SKIP_NOTIFYING_APP, false)
    val autoFreezeDelay get() = sp.getInt(AUTO_FREEZE_DELAY, 0).toLong()

    val fuzzySearch get() = sp.getBoolean(FUZZY_SEARCH, false)
    val nineKeySearch get() = sp.getBoolean(NINE_KEY_SEARCH, false)

    val iconPack get() = sp.getString(ICON_PACK, ACTION_NONE)!!
    fun setIconPack(packageName: String) = sp.edit().putString(ICON_PACK, packageName).apply()

    private val dir = "${app.filesDir.path}/v1"
    private val appsPath = "$dir/apps.json"
    private val tagsPath = "$dir/tags.json"

    val checkedList: MutableList<AppInfo> by lazy {
        mutableListOf<AppInfo>().apply {
            runCatching {
                val json = JSONArray(HFiles.read(appsPath))
                for (i in 0 until json.length()) {
                    add(with(json.getJSONObject(i)) {
                        AppInfo(
                            getString(KEY_PACKAGE), optBoolean(KEY_PINNED), optInt(KEY_TAG), optBoolean(KEY_WHITELISTED)
                        )
                    })
                }
            }
        }
    }

    fun isChecked(packageName: String): Boolean = checkedList.any { it.packageName == packageName }

    fun addCheckedApp(packageName: String, saveApps: Boolean = true, tagId: Int = 0) {
        checkedList.add(AppInfo(packageName, false, tagId, false))
        if (saveApps) saveApps()
    }

    fun removeCheckedApp(packageName: String, saveApps: Boolean = true) {
        checkedList.removeAll { it.packageName == packageName }
        if (saveApps) saveApps()
    }

    fun saveApps() {
        if (!HFiles.exists(dir)) HFiles.createDirectories(dir)
        HFiles.write(appsPath, JSONArray().run {
            checkedList.forEach {
                put(
                    JSONObject().put(KEY_PACKAGE, it.packageName).put(KEY_PINNED, it.pinned).put(KEY_TAG, it.tagId)
                        .put(KEY_WHITELISTED, it.whitelisted)
                )
            }
            toString()
        })
    }

    val tags: MutableList<Pair<String, Int>> by lazy {
        mutableListOf<Pair<String, Int>>().apply {
            runCatching {
                val json = JSONArray(HFiles.read(tagsPath))
                for (i in 0 until json.length()) {
                    add(with(json.getJSONObject(i)) { getString(KEY_TAG) to getInt(KEY_ID) })
                }
            }.onFailure {
                add(app.getString(R.string.label_default) to 0)
            }
        }
    }

    fun saveTags() {
        if (!HFiles.exists(dir)) HFiles.createDirectories(dir)
        HFiles.write(tagsPath, JSONArray().run {
            tags.forEach {
                put(JSONObject().put(KEY_TAG, it.first).put(KEY_ID, it.second))
            }
            toString()
        })
    }

    fun changeAppsSort(sort: String) = sp.edit().putString(SORT_BY, sort).apply()

    fun changeAppsFilter(filter: String, enabled: Boolean) = sp.edit().putBoolean(filter, enabled).apply()
}