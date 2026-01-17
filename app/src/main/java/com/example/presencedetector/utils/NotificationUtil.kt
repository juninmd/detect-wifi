package com.example.presencedetector.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.presencedetector.MainActivity
import com.example.presencedetector.WifiRadarActivity
import com.example.presencedetector.R

/**
 * Utility for managing notifications.
 */
object NotificationUtil {
    private const val TAG = "NotificationUtil"
    const val CHANNEL_ID = "presence_detection_channel"
    private const val CHANNEL_NAME = "Presence Detection"
    const val ALERT_CHANNEL_ID = "presence_alerts_channel"
    private const val ALERT_CHANNEL_NAME = "Presence Alerts"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)

            val foregroundChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Detection status updates"
                setShowBadge(false)
            }

            // Critical Alert Channel - bypasses DND for security alerts
            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                ALERT_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Presence detection alerts - Critical"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500, 250, 500)
                enableLights(true)
                lightColor = android.graphics.Color.RED
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setBypassDnd(true) // Bypass silent/DND mode for security alerts
                setSound(
                    android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM),
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }

            notificationManager?.createNotificationChannels(
                listOf(foregroundChannel, alertChannel)
            )
        }
    }

    fun createForegroundNotification(
        context: Context,
        title: String = "Detectando Presença",
        subtitle: String = "Scanning WiFi..."
    ): android.app.Notification {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun sendPresenceNotification(
        context: Context,
        title: String,
        message: String,
        isImportantEvent: Boolean,
        actionTitle: String? = null,
        actionIntent: PendingIntent? = null,
        notificationId: Int? = null
    ) {
        createNotificationChannels(context)

        // Ao tocar na notificação, abre a tela de Radar para ver os detalhes
        val intent = Intent(context, WifiRadarActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("from_notification", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val icon = if (isImportantEvent) {
            R.drawable.ic_notification
        } else {
            R.drawable.ic_notification_alert
        }

        val builder = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(icon)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0, 500, 250, 500))
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        if (actionTitle != null && actionIntent != null) {
            builder.addAction(R.drawable.ic_status_inactive, actionTitle, actionIntent)
            // If it has an action (like Stop Alarm), make it sticky/alerting
            builder.setFullScreenIntent(pendingIntent, true)
        }

        val notification = builder.build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val finalId = notificationId ?: System.currentTimeMillis().toInt()
        notificationManager?.notify(finalId, notification)
    }
}
