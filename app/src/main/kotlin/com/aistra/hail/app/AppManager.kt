package com.aistra.hail.app

import android.content.Intent
import com.aistra.hail.BuildConfig
import com.aistra.hail.utils.*

object AppManager {
    val lockScreen: Boolean
        get() = when (HailData.workingMode) {
            HailData.MODE_DO_HIDE -> HPolicy.lockScreen
            HailData.MODE_SU_DISABLE -> HShell.lockScreen
            HailData.MODE_SHIZUKU_DISABLE -> HShizuku.lockScreen
            else -> false
        }

    fun isAppFrozen(packageName: String): Boolean = when (HailData.workingMode) {
        HailData.MODE_DO_HIDE -> HPolicy.isAppHidden(packageName)
        HailData.MODE_DO_SUSPEND, HailData.MODE_SU_SUSPEND, HailData.MODE_SHIZUKU_SUSPEND ->
            HPackages.isAppSuspended(packageName)
        else -> HPackages.isAppDisabled(packageName)
    }

    fun setAppFrozen(packageName: String, frozen: Boolean): Boolean =
        packageName != BuildConfig.APPLICATION_ID && when (HailData.workingMode) {
            HailData.MODE_DO_HIDE -> HPolicy.setAppHidden(packageName, frozen)
            HailData.MODE_DO_SUSPEND -> HPolicy.setAppSuspended(packageName, frozen)
            HailData.MODE_SU_DISABLE -> HShell.setAppDisabled(packageName, frozen)
            HailData.MODE_SU_SUSPEND -> HShell.setAppSuspended(packageName, frozen)
            HailData.MODE_SHIZUKU_DISABLE -> HShizuku.setAppDisabledAsUser(packageName, frozen)
            HailData.MODE_SHIZUKU_SUSPEND -> HShizuku.setAppSuspendedAsUser(packageName, frozen)
            else -> false
        }

    fun uninstallApp(packageName: String) {
        when (HailData.workingMode) {
            HailData.MODE_DO_HIDE -> if (HPolicy.uninstallApp(packageName)) return
            HailData.MODE_SU_DISABLE -> if (HShell.uninstallApp(packageName)) return
        }
        HUI.startActivity(Intent.ACTION_DELETE, HPackages.packageUri(packageName))
    }
}