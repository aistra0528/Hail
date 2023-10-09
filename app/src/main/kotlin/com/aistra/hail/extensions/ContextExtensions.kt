package com.aistra.hail.extensions

import android.content.Context
import android.content.res.Configuration
import android.view.View

val Context.isLandscape get() = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
val Context.isRtl get() = resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL