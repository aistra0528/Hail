package com.aistra.hail.extensions

import android.view.View
import com.aistra.hail.utils.HUI.INSETS_TYPE_DEFAULT
import dev.chrisbanes.insetter.InsetterApplyTypeDsl
import dev.chrisbanes.insetter.InsetterDsl
import dev.chrisbanes.insetter.applyInsetter

fun InsetterApplyTypeDsl.paddingRelative(
    isRtl: Boolean,
    start: Boolean = false,
    top: Boolean = false,
    end: Boolean = false,
    bottom: Boolean = false,
    horizontal: Boolean = false,
    vertical: Boolean = false,
    animated: Boolean = false,
) {
    padding(
        left = if (!isRtl) start else end,
        top = top,
        right = if (!isRtl) end else start,
        bottom = bottom,
        horizontal = horizontal,
        vertical = vertical,
        animated = animated
    )
}

fun InsetterApplyTypeDsl.marginRelative(
    isRtl: Boolean,
    start: Boolean = false,
    top: Boolean = false,
    end: Boolean = false,
    bottom: Boolean = false,
    horizontal: Boolean = false,
    vertical: Boolean = false,
    animated: Boolean = false,
) {
    margin(
        left = if (!isRtl) start else end,
        top = top,
        right = if (!isRtl) end else start,
        bottom = bottom,
        horizontal = horizontal,
        vertical = vertical,
        animated = animated
    )
}

fun InsetterDsl.typeDefault(f: InsetterApplyTypeDsl.() -> Unit) = type(INSETS_TYPE_DEFAULT, f = f)

fun View.applyDefaultInsetter(f: InsetterApplyTypeDsl.() -> Unit) = applyInsetter {
    typeDefault(f)
}