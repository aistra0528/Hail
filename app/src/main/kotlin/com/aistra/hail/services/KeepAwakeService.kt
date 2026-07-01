package com.aistra.hail.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.aistra.hail.R
import com.aistra.hail.utils.HTarget
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Native port of the "keep screen awake" feature (ported from the sibling Coffee app), branded
 * for Hail. Global/device-wide wakelock, triggered per app from the home-screen long-press menu.
 *
 * Stops itself on [Intent.ACTION_SCREEN_OFF] (own receiver, local to this service) whether the
 * screen turned off because the wakelock's own timeout expired or because the user manually
 * locked it early. Hail's always-registered ScreenOffReceiver (see HailApp) picks up from that
 * same real screen-off moment to drive the separate freeze-after-lock delay, so the two timers
 * compose without this service needing to know anything about that chain.
 */
class KeepAwakeService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null
    private var endTimeMillis: Long = 0L
    private var durationMinutes: Int = 0
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var notificationBuilder: NotificationCompat.Builder

    private val tickRunnable = object : Runnable {
        override fun run() {
            val remaining = endTimeMillis - System.currentTimeMillis()
            if (remaining <= 0) {
                stopSelf()
                return
            }
            notify(remainingText(remaining))
            handler.postDelayed(this, 1000)
        }
    }

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        if (HTarget.T) registerReceiver(screenOffReceiver, filter, RECEIVER_NOT_EXPORTED)
        else registerReceiver(screenOffReceiver, filter)
        @Suppress("DEPRECATION") val flags =
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP
        wakeLock = getSystemService<PowerManager>()!!.newWakeLock(flags, "Hail::KeepAwakeLock")
            .apply { setReferenceCounted(false) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopSelf()
            ACTION_EXTEND -> handleExtend()
            else -> {
                triggerPackage = intent?.getStringExtra(EXTRA_PACKAGE)
                start(intent?.getIntExtra(EXTRA_MINUTES, 0) ?: 0)
            }
        }
        return START_NOT_STICKY
    }

    private fun start(minutes: Int) {
        durationMinutes = minutes
        endTimeMillis = if (minutes > 0) System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(minutes.toLong()) else 0L
        val notification = buildNotification(if (minutes > 0) remainingText(endTimeMillis - System.currentTimeMillis()) else infiniteText())
        if (HTarget.U) startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(NOTIFICATION_ID, notification)
        acquireWakeLock()
        handler.removeCallbacks(tickRunnable)
        if (minutes > 0) handler.post(tickRunnable)
    }

    private fun handleExtend() {
        if (endTimeMillis == 0L) return // already unlimited, nothing to extend
        endTimeMillis = endTimeMillis.coerceAtLeast(System.currentTimeMillis()) +
                TimeUnit.MINUTES.toMillis(durationMinutes.toLong())
        acquireWakeLock()
        notify(remainingText(endTimeMillis - System.currentTimeMillis()))
        handler.removeCallbacks(tickRunnable)
        handler.post(tickRunnable)
    }

    private fun acquireWakeLock() {
        wakeLock?.let {
            if (it.isHeld) runCatching { it.release() }
            val timeout = if (endTimeMillis > 0) {
                (endTimeMillis - System.currentTimeMillis()).coerceAtLeast(0L) + 2000L
            } else {
                TimeUnit.HOURS.toMillis(12) // safety cap for "until stopped" sessions
            }
            it.acquire(timeout)
        }
    }

    private fun remainingText(remainingMillis: Long): String {
        val totalSeconds = remainingMillis / 1000
        return getString(
            R.string.keep_awake_notification_text,
            String.format(Locale.getDefault(), "%02d:%02d", totalSeconds / 60, totalSeconds % 60)
        )
    }

    private fun infiniteText(): String = getString(R.string.keep_awake_notification_text_infinite)

    private fun buildNotification(contentText: String): Notification {
        val stopIntent = PendingIntent.getService(
            this, REQ_STOP, Intent(this, KeepAwakeService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        notificationBuilder.clearActions().addAction(0, getString(R.string.action_stop), stopIntent)
        if (endTimeMillis > 0) {
            val extendIntent = PendingIntent.getService(
                this, REQ_EXTEND, Intent(this, KeepAwakeService::class.java).setAction(ACTION_EXTEND),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            notificationBuilder.addAction(0, getString(R.string.action_extend), extendIntent)
        }
        return notificationBuilder.setContentText(contentText).setOngoing(true).build()
    }

    private fun notify(contentText: String) {
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, buildNotification(contentText))
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
            .setName(getString(R.string.keep_awake)).build()
        NotificationManagerCompat.from(this).createNotificationChannel(channel)
        notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.keep_awake_notification_title))
            .setSmallIcon(R.drawable.ic_round_frozen)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        wakeLock?.let { if (it.isHeld) runCatching { it.release() } }
        runCatching { unregisterReceiver(screenOffReceiver) }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        if (activePackage == triggerPackage) activePackage = null
        super.onDestroy()
    }

    // Which app's launch started the current session, so a freeze targeting that same app (see
    // AppManager.setAppFrozen) can cancel it. Persisted only in memory: the service isn't alive
    // across process death, and Keep Awake never survives a process restart anyway.
    private var triggerPackage: String? = null

    companion object {
        private const val CHANNEL_ID = "keep_awake"
        private const val NOTIFICATION_ID = 101
        private const val REQ_STOP = 1
        private const val REQ_EXTEND = 2
        const val ACTION_STOP = "com.aistra.hail.action.KEEP_AWAKE_STOP"
        const val ACTION_EXTEND = "com.aistra.hail.action.KEEP_AWAKE_EXTEND"
        const val EXTRA_MINUTES = "minutes"
        const val EXTRA_PACKAGE = "package"

        private var activePackage: String? = null

        fun start(context: Context, minutes: Int, packageName: String) {
            activePackage = packageName
            val intent = Intent(context, KeepAwakeService::class.java)
                .putExtra(EXTRA_MINUTES, minutes)
                .putExtra(EXTRA_PACKAGE, packageName)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopIfTriggeredBy(context: Context, packageName: String) {
            if (activePackage == packageName) {
                context.stopService(Intent(context, KeepAwakeService::class.java))
            }
        }
    }
}
