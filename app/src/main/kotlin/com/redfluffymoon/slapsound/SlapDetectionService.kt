package com.redfluffymoon.slapsound

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Optional foreground service that keeps the app alive in the background
 * and shows a persistent notification with the current slap count.
 *
 * The service receives broadcast updates from [SlapViewModel] whenever a
 * slap is detected so it can refresh the notification count.
 */
class SlapDetectionService : Service() {

    private var slapCount = 0

    private val slapReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_SLAP_DETECTED) {
                slapCount = intent.getIntExtra(EXTRA_SLAP_COUNT, slapCount)
                updateNotification()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val filter = IntentFilter(ACTION_SLAP_DETECTED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(slapReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(slapReceiver, filter)
        }

        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(slapReceiver)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Notification helpers ─────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Slap Detection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when slap detection is running in the background"
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("👋 Slap Sound – Running")
            .setContentText("Slaps detected: $slapCount")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification() {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification())
    }

    companion object {
        const val ACTION_SLAP_DETECTED = "com.redfluffymoon.slapsound.SLAP_DETECTED"
        const val EXTRA_SLAP_COUNT = "slap_count"
        private const val CHANNEL_ID = "slap_detection_channel"
        private const val NOTIFICATION_ID = 1
    }
}
