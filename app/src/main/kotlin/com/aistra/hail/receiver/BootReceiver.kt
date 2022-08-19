package com.aistra.hail.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aistra.hail.work.HWork

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        /*关机*/
        if (Intent.ACTION_SHUTDOWN.equals(intent.action)) {
            /*设备要关机*/
            println("设备关机！")
            HWork.setUnFreezeAll();
        }
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.action)) {
            /*设备要关机*/
            println("设备开机！")
        }
    }

}