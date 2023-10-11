package com.aistra.hail.extensions

import android.content.Context
import androidx.core.graphics.Insets

fun Insets.getStart(context: Context): Int {
    return if (context.isRtl) right else left
}

fun Insets.getEnd(context: Context): Int {
    return if (context.isRtl) left else right
}