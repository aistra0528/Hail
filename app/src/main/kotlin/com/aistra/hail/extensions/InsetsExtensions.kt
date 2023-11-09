package com.aistra.hail.extensions

import android.view.View
import androidx.core.graphics.Insets

fun Insets.getStart(view: View): Int {
    return if (view.isRtl) right else left
}

fun Insets.getEnd(view: View): Int {
    return if (view.isRtl) left else right
}