package com.aistra.hail.extensions

import android.view.View

val View.isRtl get() = layoutDirection == View.LAYOUT_DIRECTION_RTL
