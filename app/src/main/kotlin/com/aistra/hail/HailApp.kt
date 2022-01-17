package com.aistra.hail

import android.app.Application
import com.google.android.material.color.DynamicColors

class HailApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        app = this
    }

    companion object {
        lateinit var app: HailApp private set
    }
}