package com.aistra.hail.ui

import androidx.appcompat.app.AppCompatActivity
import com.aistra.hail.HailApp

abstract class HailActivity : AppCompatActivity() {
    protected val activity: HailActivity get() = this
    protected val app: HailApp get() = HailApp.app
}