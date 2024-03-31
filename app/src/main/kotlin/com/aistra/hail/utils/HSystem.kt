package com.aistra.hail.utils

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.PowerManager
import androidx.core.content.getSystemService
import com.aistra.hail.app.HailData
import org.lsposed.hiddenapibypass.HiddenApiBypass

object HSystem {
    fun isInteractive(context: Context): Boolean {
        val powerManger = context.getSystemService<PowerManager>()!!
        return powerManger.isInteractive
    }

    fun isCharging(context: Context): Boolean {
        val batteryStatus = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    @Suppress("SameParameterValue")
    private fun checkOp(context: Context, op: String): Boolean {
        val opsManager = context.getSystemService<AppOpsManager>()!!
        val result = if (HTarget.Q) {
            opsManager.unsafeCheckOp(op, android.os.Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            opsManager.checkOp(op, android.os.Process.myUid(), context.packageName)
        }
        return result == AppOpsManager.MODE_ALLOWED
    }

    fun checkOpUsageStats(context: Context): Boolean =
        checkOp(context, AppOpsManager.OPSTR_GET_USAGE_STATS)

    fun isForegroundApp(context: Context, packageName: String): Boolean {
        val usageStatsManager = context.getSystemService<UsageStatsManager>()!!
        val now = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            now - 1000 * 60 * (HailData.autoFreezeDelay + 1), now  // to ensure that we can get the last app used
        )?.sortedBy { it.lastTimeUsed }
        val foregroundPackageName = stats?.let {
            if (it.isEmpty()) return@let null
            it.last()?.packageName
        }
        return foregroundPackageName == packageName
    }

    private fun forceStopApp(packageName: String, ctx: Context) = runCatching {
        ctx.getSystemService<ActivityManager>()!!.let {
            if (HTarget.P) HiddenApiBypass.invoke(
                it::class.java, it, "forceStopPackage", packageName
            ) else it::class.java.getMethod(
                "forceStopPackage", String::class.java
            ).invoke(
                it, packageName
            )
        }
        true
    }.getOrElse {
        HLog.e(it)
        false
    }

    fun setAppDisabled(ctx: Context, packageName: String, disabled: Boolean): Boolean {
        HPackages.getApplicationInfoOrNull(packageName) ?: return false
        if (disabled) forceStopApp(packageName, ctx)
        runCatching {
            val newState = when {
                !disabled -> PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                else -> PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            ctx.packageManager.setApplicationEnabledSetting(packageName, newState, 0)
        }.onFailure {
            HLog.e(it)
        }
        return HPackages.isAppDisabled(packageName) == disabled
    }
}