package com.aistra.hail.utils

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

object HTarget {
    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.N)
    val N = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O)
    val O = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.P)
    val P = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
    val Q = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
    val S = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
    val T = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    val U = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
}