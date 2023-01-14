package com.aistra.hail.app

import android.content.Intent
import com.aistra.hail.BuildConfig
import com.aistra.hail.utils.*

object AppManager {
    val lockScreen: Boolean
        get() = when (HailData.workingMode) {
            HailData.MODE_DO_HIDE, HailData.MODE_DO_SUSPEND -> HPolicy.lockScreen
            HailData.MODE_SU_DISABLE, HailData.MODE_SU_SUSPEND -> HShell.lockScreen
            HailData.MODE_SHIZUKU_DISABLE, HailData.MODE_SHIZUKU_HIDE, HailData.MODE_SHIZUKU_SUSPEND -> HShizuku.lockScreen
            else -> false
        }

    fun isAppFrozen(packageName: String): Boolean = when (HailData.workingMode) {
        HailData.MODE_DO_HIDE -> HPolicy.isAppHidden(packageName)
        HailData.MODE_SHIZUKU_HIDE -> HShizuku.isAppHidden(packageName)
        HailData.MODE_DO_SUSPEND, HailData.MODE_SU_SUSPEND, HailData.MODE_SHIZUKU_SUSPEND -> HPackages.isAppSuspended(
            packageName
        )
        else -> HPackages.isAppDisabled(packageName)
    }

    fun setAppFrozen(packageName: String, frozen: Boolean): Boolean =
        packageName != BuildConfig.APPLICATION_ID && when (HailData.workingMode) {
            HailData.MODE_DO_HIDE -> HPolicy.setAppHidden(packageName, frozen)
            HailData.MODE_DO_SUSPEND -> HPolicy.setAppSuspended(packageName, frozen)
            HailData.MODE_SU_DISABLE -> HShell.setAppDisabled(packageName, frozen)
            HailData.MODE_SU_SUSPEND -> HShell.setAppSuspended(packageName, frozen)
            HailData.MODE_SHIZUKU_DISABLE -> HShizuku.setAppDisabled(packageName, frozen)
            HailData.MODE_SHIZUKU_HIDE -> HShizuku.setAppHidden(packageName, frozen)
            HailData.MODE_SHIZUKU_SUSPEND -> HShizuku.setAppSuspended(packageName, frozen)
            else -> false
        }

    fun uninstallApp(packageName: String) {
        when (HailData.workingMode) {
            HailData.MODE_DO_HIDE, HailData.MODE_DO_SUSPEND -> if (HPolicy.uninstallApp(packageName)) return
            HailData.MODE_SU_DISABLE, HailData.MODE_SU_SUSPEND -> if (HShell.uninstallApp(
                    packageName
                )
            ) return
            /*
            HailData.MODE_SHIZUKU_DISABLE, HailData.MODE_SHIZUKU_HIDE, HailData.MODE_SHIZUKU_SUSPEND -> if (HShizuku.uninstallApp(
                    packageName
                )
            ) return
            */
        }
        HUI.startActivity(Intent.ACTION_DELETE, HPackages.packageUri(packageName))
    }
}