package com.aistra.hail.app

import android.content.Intent
import com.aistra.hail.BuildConfig
import com.aistra.hail.utils.*

object AppManager {
    fun isAppFrozen(packageName: String): Boolean = when (HailData.workingMode) {
        HailData.MODE_DO_HIDE -> HPolicy.isAppHidden(packageName)
        else -> HPackages.isAppDisabled(packageName)
    }

    fun setAppFrozen(packageName: String, frozen: Boolean): Boolean =
        packageName != BuildConfig.APPLICATION_ID && when (HailData.workingMode) {
            HailData.MODE_DO_HIDE -> HPolicy.setAppHidden(packageName, frozen)
            HailData.MODE_SU_DISABLE -> HShell.setAppDisabledAsUser(packageName, frozen)
            HailData.MODE_SHIZUKU_DISABLE -> HShizuku.setAppDisabledAsUser(packageName, frozen)
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