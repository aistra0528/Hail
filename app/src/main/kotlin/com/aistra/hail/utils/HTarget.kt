package com.aistra.hail.utils

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

object HTarget {
    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.N)
    val N get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O)
    val O get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.P)
    val P get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
    val Q get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
    val T get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}