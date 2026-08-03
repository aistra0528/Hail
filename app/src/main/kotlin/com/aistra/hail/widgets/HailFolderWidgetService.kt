package com.aistra.hail.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailApi
import com.aistra.hail.utils.AppIconCache
import com.aistra.hail.utils.HPackages

class HailFolderWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return Factory(applicationContext, intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID))
    }

    private class Factory(
        private val context: Context,
        private val appWidgetId: Int
    ) : RemoteViewsFactory {
        private var apps: List<AppInfo> = emptyList()
        private var iconSize = HailFolderWidgetStore.ICON_SIZE_DEFAULT
        private var showNames = true

        override fun onCreate() = Unit

        override fun onDataSetChanged() {
            val config = HailFolderWidgetStore.loadConfig(context, appWidgetId)
            iconSize = config.iconSize
            showNames = config.showNames
            apps = config.apps
        }

        override fun onDestroy() {
            apps = emptyList()
        }

        override fun getCount(): Int = apps.size

        override fun getViewAt(position: Int): RemoteViews {
            val appInfo = apps[position]
            val applicationInfo = appInfo.applicationInfo
            val views = RemoteViews(context.packageName, R.layout.item_widget_app)
            val label = appInfo.name
            views.setTextViewText(R.id.widget_app_name, label)
            views.setViewVisibility(R.id.widget_app_name, if (showNames) View.VISIBLE else View.GONE)
            applicationInfo?.let {
                val iconPixelSize = iconSizeDpToPx(iconSize)
                val bitmap = AppIconCache.getOrLoadBitmap(context, it, appInfo.userId, iconPixelSize)
                    .centerIn(context.resources.getDimensionPixelSize(R.dimen.widget_app_icon_canvas_size), iconPixelSize)
                views.setImageViewBitmap(
                    R.id.widget_app_icon,
                    if (AppManager.isAppFrozen(appInfo.packageName, appInfo.userId)) bitmap.grayscale() else bitmap
                )
            }

            val fillInIntent = HailApi.getIntentForPackage(HailApi.ACTION_LAUNCH, appInfo.packageName, appInfo.userId)
                .setPackage(context.packageName)
            views.setOnClickFillInIntent(R.id.widget_app_root, fillInIntent)
            views.setContentDescription(
                R.id.widget_app_root,
                if (appInfo.userId == HPackages.myUserId) label else "$label (${appInfo.userId})"
            )
            return views
        }

        override fun getLoadingView(): RemoteViews? = null

        override fun getViewTypeCount(): Int = 1

        override fun getItemId(position: Int): Long {
            val appInfo = apps[position]
            return "${appInfo.packageName}#${appInfo.userId}".hashCode().toLong()
        }

        override fun hasStableIds(): Boolean = true

        private fun iconSizeDpToPx(dp: Int): Int =
            (dp * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)

        private fun Bitmap.centerIn(canvasSize: Int, iconSize: Int): Bitmap {
            val output = Bitmap.createBitmap(canvasSize, canvasSize, Bitmap.Config.ARGB_8888)
            val left = (canvasSize - iconSize) / 2f
            val top = (canvasSize - iconSize) / 2f
            Canvas(output).drawBitmap(
                this,
                null,
                android.graphics.RectF(left, top, left + iconSize, top + iconSize),
                Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            )
            return output
        }

        private fun Bitmap.grayscale(): Bitmap {
            val output = Bitmap.createBitmap(width, height, config ?: Bitmap.Config.ARGB_8888)
            Canvas(output).drawBitmap(this, 0f, 0f, Paint().apply {
                colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
            })
            return output
        }
    }
}
