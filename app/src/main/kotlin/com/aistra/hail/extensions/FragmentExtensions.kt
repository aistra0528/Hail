package com.aistra.hail.extensions

import androidx.fragment.app.Fragment

val Fragment.isLandscape get() = requireContext().isLandscape
val Fragment.isRtl get() = requireContext().isRtl