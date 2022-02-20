package com.aistra.hail.utils

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager

object HSystem {
    fun isInteractive(context: Context): Boolean {
        val powerManger = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManger.isInteractive
    }

    fun isCharging(context: Context): Boolean {
        val batteryStatus = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

        return (status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL)
    }
}