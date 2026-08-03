package com.aistra.hail.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.core.content.edit
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.HPackages

object HailFolderWidgetStore {
    private const val PREFS = "hail_folder_widgets"
    private const val KEY_APPS_PREFIX = "apps_"
    private const val KEY_TITLE_PREFIX = "title_"
    private const val KEY_ICON_SIZE_PREFIX = "icon_size_"
    private const val KEY_BACKGROUND_ALPHA_PREFIX = "background_alpha_"
    private const val KEY_SHOW_NAMES_PREFIX = "show_names_"
    private const val DELIMITER = "|"
    private const val USER_SEPARATOR = "#"

    const val ICON_SIZE_MIN = 48
    const val ICON_SIZE_DEFAULT = 64
    const val ICON_SIZE_MAX = 96

    data class Config(
        val title: String,
        val iconSize: Int,
        val backgroundAlpha: Int,
        val showNames: Boolean,
        val apps: List<AppInfo>
    )

    fun checkedApps(): List<AppInfo> =
        HailData.checkedList.filter { it.applicationInfo != null }

    fun defaultApps(): List<AppInfo> = checkedApps().take(16)

    fun defaultConfig(context: Context): Config =
        Config(
            title = context.getString(R.string.widget_folder_title),
            iconSize = ICON_SIZE_DEFAULT,
            backgroundAlpha = 230,
            showNames = true,
            apps = defaultApps()
        )

    fun save(context: Context, appWidgetId: Int, config: Config) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putString(appsKey(appWidgetId), encodeApps(config.apps))
            putString(titleKey(appWidgetId), config.title)
            putInt(iconSizeKey(appWidgetId), config.iconSize.coerceIn(ICON_SIZE_MIN, ICON_SIZE_MAX))
            putInt(backgroundAlphaKey(appWidgetId), config.backgroundAlpha.coerceIn(0, 255))
            putBoolean(showNamesKey(appWidgetId), config.showNames)
        }
    }

    fun loadConfig(context: Context, appWidgetId: Int): Config {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val defaults = defaultConfig(context)
        return Config(
            title = prefs.getString(titleKey(appWidgetId), defaults.title) ?: defaults.title,
            iconSize = prefs.getInt(iconSizeKey(appWidgetId), defaults.iconSize).coerceIn(ICON_SIZE_MIN, ICON_SIZE_MAX),
            backgroundAlpha = prefs.getInt(backgroundAlphaKey(appWidgetId), defaults.backgroundAlpha).coerceIn(0, 255),
            showNames = prefs.getBoolean(showNamesKey(appWidgetId), defaults.showNames),
            apps = prefs.getString(appsKey(appWidgetId), null)?.let(::decodeApps) ?: defaults.apps
        )
    }

    fun load(context: Context, appWidgetId: Int): List<AppInfo> =
        loadConfig(context, appWidgetId).apps

    fun delete(context: Context, appWidgetIds: IntArray) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            appWidgetIds.forEach {
                remove(appsKey(it))
                remove(titleKey(it))
                remove(iconSizeKey(it))
                remove(backgroundAlphaKey(it))
                remove(showNamesKey(it))
            }
        }
    }

    fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, HailFolderWidgetProvider::class.java))
        HailFolderWidgetProvider.update(context, manager, ids)
    }

    private fun encodeApps(apps: List<AppInfo>): String =
        apps.joinToString(DELIMITER) { "${it.packageName}$USER_SEPARATOR${it.userId}" }

    private fun decodeApps(saved: String): List<AppInfo> =
        saved.split(DELIMITER).mapNotNull {
            val userSeparator = it.lastIndexOf(USER_SEPARATOR)
            if (userSeparator <= 0) return@mapNotNull null
            val packageName = it.substring(0, userSeparator)
            val userId = it.substring(userSeparator + 1).toIntOrNull() ?: HPackages.myUserId
            AppInfo(packageName, userId = userId).takeIf { appInfo -> appInfo.applicationInfo != null }
        }

    private fun appsKey(appWidgetId: Int) = "$KEY_APPS_PREFIX$appWidgetId"
    private fun titleKey(appWidgetId: Int) = "$KEY_TITLE_PREFIX$appWidgetId"
    private fun iconSizeKey(appWidgetId: Int) = "$KEY_ICON_SIZE_PREFIX$appWidgetId"
    private fun backgroundAlphaKey(appWidgetId: Int) = "$KEY_BACKGROUND_ALPHA_PREFIX$appWidgetId"
    private fun showNamesKey(appWidgetId: Int) = "$KEY_SHOW_NAMES_PREFIX$appWidgetId"
}
