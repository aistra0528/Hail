package com.aistra.hail.xposed

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.service.quicksettings.TileService
import com.aistra.hail.BuildConfig
import com.aistra.hail.app.HailApi.ACTION_UNFREEZE
import com.aistra.hail.app.HailData
import com.aistra.hail.ui.api.ApiActivity
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import java.lang.reflect.Method

class XposedInterface : IXposedHookLoadPackage {
    @Throws(Throwable::class)
    override fun handleLoadPackage(loadPackageParam: LoadPackageParam) {
        if (!loadPackageParam.isFirstApplication) {
            return
        }

        if (loadPackageParam.packageName == BuildConfig.APPLICATION_ID) {
            return
        }

        LaunchAppHook(loadPackageParam.classLoader).startHook()
    }

    abstract class BaseHook(protected val classLoader: ClassLoader) {
        abstract fun startHook()
    }
}
