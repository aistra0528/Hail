package com.aistra.hail.app

import android.content.Intent
import com.aistra.hail.BuildConfig
import com.aistra.hail.utils.*

object AppManager {
    val lockScreen: Boolean
        get() = when {
            HailData.workingMode.startsWith(HailData.OWNER) -> HPolicy.lockScreen
            HailData.workingMode.startsWith(HailData.SU) -> HShell.lockScreen
            HailData.workingMode.startsWith(HailData.SHIZUKU) -> HShizuku.lockScreen
            else -> false
        }

    fun isAppFrozen(packageName: String): Boolean = when {
        HailData.workingMode == HailData.MODE_OWNER_HIDE -> HPolicy.isAppHidden(packageName)
        HailData.workingMode == HailData.MODE_SHIZUKU_HIDE -> HShizuku.isAppHidden(packageName)
        HailData.workingMode.endsWith(HailData.SUSPEND) -> HPackages.isAppSuspended(packageName)
        else -> HPackages.isAppDisabled(packageName)
    }

    fun setAppFrozen(packageName: String, frozen: Boolean): Boolean =
        packageName != BuildConfig.APPLICATION_ID && when (HailData.workingMode) {
            HailData.MODE_OWNER_HIDE -> HPolicy.setAppHidden(packageName, frozen)
            HailData.MODE_OWNER_SUSPEND -> HPolicy.setAppSuspended(packageName, frozen)
            HailData.MODE_SU_DISABLE -> HShell.setAppDisabled(packageName, frozen)
            HailData.MODE_SU_SUSPEND -> HShell.setAppSuspended(packageName, frozen)
            HailData.MODE_SHIZUKU_DISABLE -> HShizuku.setAppDisabled(packageName, frozen)
            HailData.MODE_SHIZUKU_HIDE -> HShizuku.setAppHidden(packageName, frozen)
            HailData.MODE_SHIZUKU_SUSPEND -> HShizuku.setAppSuspended(packageName, frozen)
            else -> false
        }

    fun uninstallApp(packageName: String): Boolean {
        when {
            HailData.workingMode.startsWith(HailData.OWNER) -> if (HPolicy.uninstallApp(
                    packageName
                )
            ) return true
            HailData.workingMode.startsWith(HailData.SU) -> if (HShell.uninstallApp(
                    packageName
                )
            ) return true
            HailData.workingMode.startsWith(HailData.SHIZUKU) -> if (HShizuku.uninstallApp(
                    packageName
                )
            ) return true
        }
        HUI.startActivity(Intent.ACTION_DELETE, HPackages.packageUri(packageName))
        return false
    }

    fun execute(command: String): Boolean = when {
        HailData.workingMode.startsWith(HailData.SU) -> HShell.execute(command, true)
        HailData.workingMode.startsWith(HailData.SHIZUKU) -> HShizuku.execute(command)
        else -> false
    }
}