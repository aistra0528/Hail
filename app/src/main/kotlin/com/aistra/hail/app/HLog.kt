package com.aistra.hail.app

import android.util.Log

object HLog {
    private const val TAG = "Hail"
    fun i(string: String) = Log.i(TAG, string)
}