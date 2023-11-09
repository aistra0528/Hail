package com.aistra.hail.extensions

import android.view.View
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePaddingRelative

val View.isRtl get() = layoutDirection == View.LAYOUT_DIRECTION_RTL

/**
 * Very easy to apply insets to a view.
 */
fun View.applyInsetsPadding(
    start: Boolean = false,
    end: Boolean = false,
    top: Boolean = false,
    bottom: Boolean = false
) {
    val origin = Insets.of(paddingLeft, paddingTop, paddingRight, paddingBottom)
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, windowInsets ->
        val insets =
            windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        v.updatePaddingRelative(
            start = origin.getStart(v) + if (start) insets.getStart(v) else 0,
            end = origin.getEnd(v) + if (end) insets.getEnd(v) else 0,
            bottom = origin.bottom + if (bottom) insets.bottom else 0,
            top = origin.top + if (top) insets.top else 0,
        )
        windowInsets
    }
}