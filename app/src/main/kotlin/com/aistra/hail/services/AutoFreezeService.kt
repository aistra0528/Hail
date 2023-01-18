package com.aistra.hail.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.getSystemService
import com.aistra.hail.R
import com.aistra.hail.app.HailApi
import com.aistra.hail.app.HailData
import com.aistra.hail.receiver.ScreenOffReceiver
import com.aistra.hail.utils.HTarget

class AutoFreezeService : NotificationListenerService() {
    private val channelID = javaClass.simpleName
    private val lockReceiver by lazy { ScreenOffReceiver() }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val freezeAll = PendingIntent.getActivity(
            applicationContext, 0, Intent(HailApi.ACTION_FREEZE_ALL), PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, channelID)
            .setContentTitle(getString(R.string.auto_freeze_notification_title))
            .setSmallIcon(R.drawable.ic_round_frozen)
            .addAction(R.drawable.ic_round_frozen, getString(R.string.action_freeze_all), freezeAll)
        if (HailData.checkedList.any { it.whitelisted }) {
            val freezeNonWhitelisted = PendingIntent.getActivity(
                applicationContext,
                0,
                Intent(HailApi.ACTION_FREEZE_NON_WHITELISTED),
                PendingIntent.FLAG_IMMUTABLE
            )
            notification.addAction(
                R.drawable.ic_round_frozen,
                getString(R.string.action_freeze_non_whitelisted),
                freezeNonWhitelisted
            )
        }
        startForeground(100, notification.build())
        return START_STICKY
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (HTarget.O) {
            val name = getString(R.string.auto_freeze)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(channelID, name, importance)
            // Register the channel with the system
            val notificationManager = getSystemService<NotificationManager>()
            notificationManager?.createNotificationChannel(channel)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        registerScreenReceiver()
    }

    private fun registerScreenReceiver() {
        registerReceiver(lockReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(lockReceiver)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    companion object {
        lateinit var instance: AutoFreezeService private set
    }
}