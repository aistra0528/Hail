package com.aistra.hail.utils

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.services.DhizukuService
import com.aistra.hail.services.IDhizukuService
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.dhizuku.api.DhizukuUserServiceArgs

object HDhizuku {
    private var mService: IDhizukuService? = null

    fun init() {
        Dhizuku.init()
        if (Dhizuku.isPermissionGranted()) bindService()
    }

    fun bindService() = Dhizuku.bindUserService(
        DhizukuUserServiceArgs(ComponentName(app, DhizukuService::class.java)),
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                mService = IDhizukuService.Stub.asInterface(service)
            }

            override fun onServiceDisconnected(name: ComponentName) {
            }
        })

    val lockScreen: Boolean get() = mService?.lockScreen() ?: false

    fun isAppHidden(packageName: String): Boolean = mService?.isAppHidden(packageName) ?: false

    fun setAppHidden(packageName: String, hidden: Boolean): Boolean =
        mService?.setAppHidden(packageName, hidden) ?: false

    fun setAppSuspended(packageName: String, suspended: Boolean): Boolean =
        mService?.setAppSuspended(packageName, suspended) ?: false

    fun uninstallApp(packageName: String): Boolean = mService?.uninstallApp(packageName) ?: false
}