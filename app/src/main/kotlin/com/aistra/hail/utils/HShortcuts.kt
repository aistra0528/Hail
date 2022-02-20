package com.aistra.hail.utils

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.aistra.hail.HailApp
import com.aistra.hail.R
import com.aistra.hail.app.HailApi

object HShortcuts {
    fun addPinShortcut(icon: Drawable, id: String, label: CharSequence, intent: Intent) {
        if (ShortcutManagerCompat.isRequestPinShortcutSupported(HailApp.app)) {
            val shortcut = ShortcutInfoCompat.Builder(HailApp.app, id)
                .setIcon(getDrawableIcon(icon))
                .setShortLabel(label)
                .setIntent(intent)
                .build()
            ShortcutManagerCompat.requestPinShortcut(HailApp.app, shortcut, null)
        } else HUI.showToast(
            R.string.operation_failed,
            HailApp.app.getString(R.string.action_add_pin_shortcut)
        )
    }

    fun addDynamicShortcut(packageName: String) {
        if (isMaxDynamicShortcutCount()) removeAllDynamicShortcuts()
        val applicationInfo = HPackages.getApplicationInfoOrNull(packageName)
        val shortcut = ShortcutInfoCompat.Builder(HailApp.app, packageName)
            .setIcon(
                getDrawableIcon(
                    applicationInfo?.loadIcon(HailApp.app.packageManager)
                        ?: HailApp.app.packageManager.defaultActivityIcon
                )
            )
            .setShortLabel(applicationInfo?.loadLabel(HailApp.app.packageManager) ?: packageName)
            .setIntent(HailApi.getIntentForPackage(HailApi.ACTION_LAUNCH, packageName))
            .build()
        ShortcutManagerCompat.pushDynamicShortcut(HailApp.app, shortcut)
        val freezeAll = ShortcutInfoCompat.Builder(HailApp.app, HailApi.ACTION_FREEZE_ALL)
            .setIcon(
                getDrawableIcon(
                    AppCompatResources.getDrawable(
                        HailApp.app,
                        R.drawable.ic_round_frozen_shortcut
                    )!!
                )
            )
            .setShortLabel(HailApp.app.getString(R.string.action_freeze_all))
            .setIntent(Intent(HailApi.ACTION_FREEZE_ALL))
            .build()
        ShortcutManagerCompat.pushDynamicShortcut(HailApp.app, freezeAll)
    }

    fun removeAllDynamicShortcuts() {
        ShortcutManagerCompat.removeAllDynamicShortcuts(HailApp.app)
    }

    private fun isMaxDynamicShortcutCount(): Boolean =
        ShortcutManagerCompat.getDynamicShortcuts(HailApp.app).size >=
                ShortcutManagerCompat.getMaxShortcutCountPerActivity(HailApp.app)

    private fun getDrawableIcon(drawable: Drawable): IconCompat =
        IconCompat.createWithBitmap(
            Bitmap.createBitmap(
                drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
            ).also {
                with(Canvas(it)) {
                    drawable.setBounds(0, 0, width, height)
                    drawable.draw(this)
                }
            })
}