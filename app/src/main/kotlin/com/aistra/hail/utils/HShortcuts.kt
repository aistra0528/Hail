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
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.HailApi
import com.aistra.hail.app.HailData
import me.zhanghai.android.appiconloader.AppIconLoader

object HShortcuts {
    private val iconLoader by lazy {
        AppIconLoader(
            HailApp.app.resources.getDimensionPixelSize(R.dimen.app_icon_size),
            HailData.synthesizeAdaptiveIcons,
            HailApp.app
        )
    }

    fun addPinShortcut(icon: Drawable, id: String, label: CharSequence, intent: Intent) {
        addPinShortcut(getDrawableIcon(icon), id, label, intent)
    }

    fun addPinShortcut(appInfo: AppInfo, id: String, label: CharSequence, intent: Intent) {
        appInfo.applicationInfo?.let {
            val icon = IconPack.loadIcon(it.packageName) ?: iconLoader.loadIcon(it)
            addPinShortcut(IconCompat.createWithBitmap(icon), id, label, intent)
        } ?: run {
            addPinShortcut(HailApp.app.packageManager.defaultActivityIcon, id, label, intent)
        }
    }

    private fun addPinShortcut(icon: IconCompat, id: String, label: CharSequence, intent: Intent) {
        if (ShortcutManagerCompat.isRequestPinShortcutSupported(HailApp.app)) {
            val shortcut =
                ShortcutInfoCompat.Builder(HailApp.app, id).setIcon(icon).setShortLabel(label)
                    .setIntent(intent).build()
            ShortcutManagerCompat.requestPinShortcut(HailApp.app, shortcut, null)
        } else HUI.showToast(
            R.string.operation_failed, HailApp.app.getString(R.string.action_add_pin_shortcut)
        )
    }

    fun addDynamicShortcut(packageName: String) {
        if (isMaxDynamicShortcutCount()) removeAllDynamicShortcuts()
        if (HailData.biometricLogin) {
            removeAllDynamicShortcuts()
        } else {
            val applicationInfo = HPackages.getApplicationInfoOrNull(packageName)
            val shortcut = ShortcutInfoCompat.Builder(HailApp.app, packageName)
                .setIcon(IconCompat.createWithBitmap(applicationInfo?.let {
                    IconPack.loadIcon(it.packageName) ?: iconLoader.loadIcon(it)
                } ?: getBitmapFromDrawable(
                    HailApp.app.packageManager.defaultActivityIcon
                ))).setShortLabel(
                    applicationInfo?.loadLabel(HailApp.app.packageManager) ?: packageName
                ).setIntent(HailApi.getIntentForPackage(HailApi.ACTION_LAUNCH, packageName)).build()
            ShortcutManagerCompat.pushDynamicShortcut(HailApp.app, shortcut)
        }
        if (HailData.dynamicShortcutAction == HailData.ACTION_NONE) return
        val id = when (HailData.dynamicShortcutAction) {
            HailData.ACTION_FREEZE_NON_WHITELISTED -> HailApi.ACTION_FREEZE_NON_WHITELISTED
            HailData.ACTION_LOCK_FREEZE -> HailApi.ACTION_LOCK_FREEZE
            else -> HailApi.ACTION_FREEZE_ALL
        }
        val icon = when (HailData.dynamicShortcutAction) {
            HailData.ACTION_LOCK_FREEZE -> R.drawable.ic_outline_lock_shortcut
            else -> R.drawable.ic_round_frozen_shortcut
        }
        val label = when (HailData.dynamicShortcutAction) {
            HailData.ACTION_FREEZE_NON_WHITELISTED -> R.string.action_freeze_non_whitelisted
            HailData.ACTION_LOCK_FREEZE -> R.string.action_lock_freeze
            else -> R.string.action_freeze_all
        }
        val action = ShortcutInfoCompat.Builder(HailApp.app, id).setIcon(
            getDrawableIcon(
                AppCompatResources.getDrawable(
                    HailApp.app, icon
                )!!
            )
        ).setShortLabel(HailApp.app.getString(label)).setIntent(Intent(id)).build()
        ShortcutManagerCompat.pushDynamicShortcut(HailApp.app, action)
    }

    fun removeAllDynamicShortcuts() {
        ShortcutManagerCompat.removeAllDynamicShortcuts(HailApp.app)
    }

    private fun isMaxDynamicShortcutCount(): Boolean =
        ShortcutManagerCompat.getDynamicShortcuts(HailApp.app).size >= ShortcutManagerCompat.getMaxShortcutCountPerActivity(
            HailApp.app
        )

    private fun getDrawableIcon(drawable: Drawable): IconCompat =
        IconCompat.createWithBitmap(getBitmapFromDrawable(drawable))

    private fun getBitmapFromDrawable(drawable: Drawable): Bitmap = Bitmap.createBitmap(
        drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
    ).also {
        with(Canvas(it)) {
            drawable.setBounds(0, 0, width, height)
            drawable.draw(this)
        }
    }
}