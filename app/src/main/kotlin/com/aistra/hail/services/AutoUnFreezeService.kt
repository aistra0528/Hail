package com.aistra.hail.services

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.service.notification.NotificationListenerService
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.aistra.hail.R
import com.aistra.hail.app.HailApi
import com.aistra.hail.receiver.ScreenOnReceiver

class AutoUnFreezeService : NotificationListenerService() {
    private val channelID = javaClass.simpleName
    private val lockReceiver by lazy { ScreenOnReceiver() }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val freezeAuto = PendingIntent.getActivity(
            applicationContext, 0, Intent(HailApi.ACTION_UNFREEZE_ALL), PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, channelID)
            .setContentTitle(getString(R.string.auto_unfreeze_notification_title))
            .setSmallIcon(R.drawable.ic_round_unfrozen)
            .addAction(R.drawable.ic_round_unfrozen, getString(R.string.auto_unfreeze), freezeAuto)

        startForeground(101, notification.build())
        return START_STICKY
    }

    private fun createNotificationChannel() {
        val name = getString(R.string.auto_unfreeze)
        val importance = NotificationManagerCompat.IMPORTANCE_HIGH
        val channel = NotificationChannelCompat.Builder(channelID, importance).setName(name).build()
        // Register the channel with the system
        NotificationManagerCompat.from(this).createNotificationChannel(channel)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        registerScreenReceiver()
    }

    private fun registerScreenReceiver() {
        registerReceiver(lockReceiver, IntentFilter(Intent.ACTION_USER_PRESENT))
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(lockReceiver)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    companion object {
        lateinit var instance: AutoUnFreezeService private set
    }
}