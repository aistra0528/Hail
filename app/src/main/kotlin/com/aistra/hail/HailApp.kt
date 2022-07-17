package com.aistra.hail

import android.app.Application
import android.content.res.Resources
import com.aistra.hail.app.HailData
import com.google.android.material.color.DynamicColors
import me.zhanghai.android.appiconloader.AppIconLoader

class HailApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        app = this
        iconLoader =
            AppIconLoader((64 * Resources.getSystem().displayMetrics.density).toInt(), HailData.synthesizeAdaptiveIcons, this)
    }

    companion object {
        lateinit var app: HailApp private set
        lateinit var iconLoader: AppIconLoader
    }
}