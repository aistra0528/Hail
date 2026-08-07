package com.aistra.hail.xposed

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.aistra.hail.BuildConfig
import com.aistra.hail.app.HailApi
import com.aistra.hail.utils.HTarget
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface

class LaunchAppHook : XposedModule() {
    @Throws(Throwable::class)
    override fun onPackageLoaded(param: XposedModuleInterface.PackageLoadedParam) {
        if (!HTarget.O || !param.isFirstPackage || param.packageName == BuildConfig.APPLICATION_ID) {
            return
        }
        hookLauncherApp()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun hookLauncherApp() {
        hook(
            Activity::class.java.getMethod(
                "startActivityForResult", Intent::class.java, Int::class.java, Bundle::class.java
            )
        ).intercept { unsuspendBeforeLaunch(it) }
        hook(
            TileService::class.java.getMethod(
                "startActivityAndCollapse", Intent::class.java
            )
        ).intercept { unsuspendBeforeLaunch(it) }
        hook(
            ContextWrapper::class.java.getMethod(
                "startActivity", Intent::class.java
            )
        ).intercept { unsuspendBeforeLaunch(it) }
        hook(
            ContextWrapper::class.java.getMethod(
                "startActivity", Intent::class.java, Bundle::class.java
            )
        ).intercept { unsuspendBeforeLaunch(it) }
    }

    fun unsuspendBeforeLaunch(param: XposedInterface.Chain): Any {
        if (param.args.isNotEmpty() && param.args[0] != null) {
            val intent = param.args[0] as Intent
            var packageName = intent.getPackage()
            val component = intent.component
            if (packageName == null && component != null) {
                packageName = component.packageName
            }
            val context = param.thisObject as Context
            if (packageName != null && packageName != context.packageName && packageName != BuildConfig.APPLICATION_ID) {
                unsuspendApp(context, packageName)
            }
        }
        return param.proceed()
    }

    private fun unsuspendApp(context: Context, packageName: String) {
        val packageManager = context.packageManager
        val method = packageManager.javaClass.getMethod("isPackageSuspended", String::class.java)
        if (method.invoke(packageManager, packageName) as Boolean) {
            context.startActivity(HailApi.getIntentForPackage(HailApi.ACTION_UNFREEZE, packageName))

            /**
             * It took about 500 milliseconds from [Context.startActivity]
             * to start [com.aistra.hail.ui.api.ApiActivity] and successfully unfreeze.
             * We lack communication between applications, we can only wait.
             */
            Thread.sleep(300)
            repeat(6) {
                if (!(method.invoke(packageManager, packageName) as Boolean)) return@repeat
                Thread.sleep(75)
            }
        }
    }
}