package com.aistra.hail.extensions

import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat.Type.displayCutout
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
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
        val insets = windowInsets.getInsets(systemBars() or displayCutout())
        v.updatePaddingRelative(
            start = origin.getStart(v) + if (start) insets.getStart(v) else 0,
            end = origin.getEnd(v) + if (end) insets.getEnd(v) else 0,
            bottom = origin.bottom + if (bottom) insets.bottom else 0,
            top = origin.top + if (top) insets.top else 0,
        )
        windowInsets
    }
}

fun View.applyInsetsMargin(
    start: Boolean = false,
    end: Boolean = false,
    top: Boolean = false,
    bottom: Boolean = false
) {
    val origin = Insets.of(marginLeft, marginTop, marginRight, marginBottom)
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, windowInsets ->
        val insets = windowInsets.getInsets(systemBars() or displayCutout())
        (v.layoutParams as MarginLayoutParams).apply {
            val marginStart = origin.getStart(v) + if (start) insets.getStart(v) else 0
            val marginEnd = origin.getEnd(v) + if (end) insets.getEnd(v) else 0
            setMargins(
                if (isRtl) marginEnd else marginStart,
                origin.top + if (top) insets.top else 0,
                if (isRtl) marginStart else marginEnd,
                origin.bottom + if (bottom) insets.bottom else 0
            )
        }
        windowInsets
    }
}