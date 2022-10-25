package com.aistra.hail.utils

import android.os.Build

object HTarget {
    private fun target(api: Int): Boolean = Build.VERSION.SDK_INT >= api

    val O = target(Build.VERSION_CODES.O)
    val Q = target(Build.VERSION_CODES.Q)
    val T = target(Build.VERSION_CODES.TIRAMISU)
}