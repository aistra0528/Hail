package com.aistra.hail.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.aistra.hail.R
import com.aistra.hail.receiver.ScreenOffReceiver

class AutoFreezeService : Service() {
    private val channelID = "foregroundService"
    private lateinit var mReceiver : ScreenOffReceiver
    private lateinit var mIntentFilter : IntentFilter

    override fun onBind(intent: Intent?): IBinder? {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, channelID)
            .setContentTitle(getString(R.string.foreground_service_notification_title))
            .setContentText(getString(R.string.foreground_service_notification_message))
            .setSmallIcon(R.drawable.ic_round_frozen)
            .build()
        startForeground(100, notification)
        return START_STICKY
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.foreground_service_channel_name)
            val descriptionText = getString(R.string.foreground_service_channel_description)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(channelID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onCreate() {
        super.onCreate()
        registerScreenReceiver()
    }

    private fun registerScreenReceiver() {
        mReceiver = ScreenOffReceiver()
        mIntentFilter = IntentFilter()
        mIntentFilter.addAction(Intent.ACTION_SCREEN_OFF)
        registerReceiver(mReceiver, mIntentFilter)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mReceiver)
        stopForeground(true)
    }
}