package com.aistra.hail.extensions

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePaddingRelative

val View.isRtl get() = layoutDirection == View.LAYOUT_DIRECTION_RTL

/**
 * Very easy to apply insets to a view.
 * */
fun View.applyInsetsPadding(
    start: Boolean = false,
    end: Boolean = false,
    top: Boolean = false,
    bottom: Boolean = false
) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, windowInsets ->
        val insets =
            windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        v.updatePaddingRelative(
            start = if (start) insets.getStart(context) else 0,
            end = if (end) insets.getEnd(context) else 0,
            bottom = if (bottom) insets.bottom else 0,
            top = if (top) insets.top else 0,
        )
        windowInsets
    }
}