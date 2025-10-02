package com.aistra.hail.xposed

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import android.service.quicksettings.TileService
import com.aistra.hail.app.HailApi
import com.aistra.hail.ui.api.ApiActivity
import com.aistra.hail.utils.HTarget
import com.aistra.hail.xposed.XposedInterface.BaseHook
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import java.lang.reflect.Method

class LaunchAppHook(classLoader: ClassLoader) : BaseHook(classLoader) {
    override fun startHook() {
        appFreezeInject(classLoader)
    }

    private fun appFreezeInject(classLoader: ClassLoader) {
        if (HTarget.P) {
            val hook = object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (param.args.isNotEmpty() && param.args[0] != null) {
                        val intent = param.args[0] as Intent
                        var packageName = intent.getPackage()
                        val component = intent.component
                        if (packageName == null && component != null) {
                            packageName = component.packageName
                        }
                        val context = param.thisObject as Context
                        if (packageName != null && packageName != context.packageName) {
                            unfreezeApp(context, packageName)
                        }
                    }
                }
            }

            XposedHelpers.findAndHookMethod(
                Activity::class.java.name,
                classLoader,
                "startActivityForResult",
                Intent::class.java, Int::class.javaPrimitiveType,
                Bundle::class.java,
                hook
            )
            XposedHelpers.findAndHookMethod(
                TileService::class.java.name,
                classLoader,
                "startActivityAndCollapse",
                Intent::class.java, hook
            )
            val contextClass = XposedHelpers.findClass(
                ContextWrapper::class.java.name,
                classLoader
            )
            XposedHelpers.findAndHookMethod(
                contextClass,
                "startActivity",
                Intent::class.java,
                hook
            )
            XposedHelpers.findAndHookMethod(
                contextClass,
                "startActivity",
                Intent::class.java, Bundle::class.java,
                hook
            )
        }
    }

    private fun unfreezeApp(context: Context, packageName: String) {
        val packageManager = context.applicationContext.packageManager
        val method: Method = packageManager.javaClass.getMethod(
            "isPackageSuspended",
            String::class.java
        )
        if (method.invoke(packageManager, packageName) as Boolean) {
            context.startActivity(HailApi.getIntentForPackage(HailApi.ACTION_UNFREEZE, packageName))

            /**
             * It took about 500 milliseconds from [Context.startActivity]
             * to start [ApiActivity] and successfully unfreeze.
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