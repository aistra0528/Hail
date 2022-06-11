package com.aistra.hail.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.getSystemService
import com.aistra.hail.R
import com.aistra.hail.app.HailApi
import com.aistra.hail.receiver.ScreenOffReceiver

class AutoFreezeService : Service() {
    private val channelID = javaClass.simpleName
    private lateinit var lockReceiver: ScreenOffReceiver

    override fun onBind(intent: Intent?): IBinder? {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val freezeAll = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(HailApi.ACTION_FREEZE_ALL),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, channelID)
            .setContentTitle(getString(R.string.auto_freeze_notification_title))
            .setSmallIcon(R.drawable.ic_round_frozen)
            .addAction(R.drawable.ic_round_frozen, getString(R.string.action_freeze_all), freezeAll)
            .build()
        startForeground(100, notification)
        return START_STICKY
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
        registerScreenReceiver()
    }

    private fun registerScreenReceiver() {
        lockReceiver = ScreenOffReceiver()
        registerReceiver(lockReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(lockReceiver)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }
}