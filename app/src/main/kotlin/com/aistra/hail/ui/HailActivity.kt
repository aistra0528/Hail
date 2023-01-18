package com.aistra.hail.ui

import androidx.appcompat.app.AppCompatActivity
import com.aistra.hail.HailApp

open class HailActivity : AppCompatActivity() {
    fun setAutoFreezeService() = HailApp.app.setAutoFreezeService()
}