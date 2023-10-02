package com.aistra.hail.extensions

import android.content.Context
import android.content.res.Configuration

val Context.isLandscape get() = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
val Context.isRtl get() = resources.configuration.layoutDirection == Configuration.SCREENLAYOUT_LAYOUTDIR_RTL