package com.aistra.hail.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.aistra.hail.R
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailApi

class HailFolderWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_FREEZE_WIDGET) return
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        val apps = HailFolderWidgetStore.load(context, appWidgetId)
        AppManager.setListFrozen(true, *apps.toTypedArray())
        HailFolderWidgetStore.updateAll(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        update(context, appWidgetManager, appWidgetIds)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        HailFolderWidgetStore.delete(context, appWidgetIds)
    }

    companion object {
        private const val ACTION_FREEZE_WIDGET = "com.aistra.hail.action.FREEZE_WIDGET"

        fun update(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
            appWidgetIds.forEach { appWidgetId ->
                val config = HailFolderWidgetStore.loadConfig(context, appWidgetId)
                val views = RemoteViews(context.packageName, R.layout.widget_hail_folder)
                val serviceIntent = Intent(context, HailFolderWidgetService::class.java)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    .setData(android.net.Uri.parse("hail://folder-widget/$appWidgetId/${config.iconSize}"))

                views.setRemoteAdapter(R.id.widget_app_grid, serviceIntent)
                views.setEmptyView(R.id.widget_app_grid, R.id.widget_empty)
                views.setTextViewText(R.id.widget_title, config.title)
                views.setViewVisibility(R.id.widget_title, if (config.title.isBlank()) View.GONE else View.VISIBLE)
                views.setInt(R.id.widget_background, "setImageAlpha", config.backgroundAlpha)

                val launchTemplate = PendingIntent.getActivity(
                    context,
                    appWidgetId,
                    Intent(HailApi.ACTION_LAUNCH).setPackage(context.packageName),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
                views.setPendingIntentTemplate(R.id.widget_app_grid, launchTemplate)

                val configureIntent = Intent(context, HailFolderWidgetConfigureActivity::class.java)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                val configurePendingIntent = PendingIntent.getActivity(
                    context,
                    appWidgetId,
                    configureIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_title, configurePendingIntent)
                views.setOnClickPendingIntent(R.id.widget_root, configurePendingIntent)

                val freezeIntent = Intent(context, HailFolderWidgetProvider::class.java)
                    .setAction(ACTION_FREEZE_WIDGET)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                val freezePendingIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId,
                    freezeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_freeze, freezePendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_app_grid)
            }
        }
    }
}
