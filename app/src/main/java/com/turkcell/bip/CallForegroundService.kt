package com.turkcell.bip

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder

class CallForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "bip_call_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "ACTION_START_CALL"
        const val ACTION_STOP = "ACTION_STOP_CALL"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startForeground(NOTIFICATION_ID, buildNotification())
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Aktif Çağrı",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Bip aktif çağrı bildirimi"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Bip — Aktif Çağrı")
            .setContentText("Çağrı devam ediyor...")
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .build()
    }
}
