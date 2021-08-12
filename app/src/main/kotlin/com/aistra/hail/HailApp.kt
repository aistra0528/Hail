package com.aistra.hail

import android.app.Application

class HailApp : Application() {
    override fun onCreate() {
        super.onCreate()
        app = this
    }

    companion object {
        lateinit var app: HailApp
    }
}