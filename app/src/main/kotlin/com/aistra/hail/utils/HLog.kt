package com.aistra.hail.utils

import android.util.Log

object HLog {
    private const val TAG = "Hail"
    fun i(string: String) = Log.i(TAG, string)
    fun e(t: Throwable) = Log.e(TAG, Log.getStackTraceString(t))
}