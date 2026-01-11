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
import com.example.presencedetector.R

/**
 * Utility for managing notifications.
 */
object NotificationUtil {
    private const val TAG = "NotificationUtil"
    private const val CHANNEL_ID = "presence_detection_channel"
    private const val CHANNEL_NAME = "Presence Detection"
    private const val ALERT_CHANNEL_ID = "presence_alerts_channel"
    private const val ALERT_CHANNEL_NAME = "Presence Alerts"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)

            // Canal para notificações de foreground (contínuo)
            val foregroundChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Detection status updates"
                setShowBadge(false)
            }

            // Canal para alertas (presença detectada)
            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                ALERT_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Presence detection alerts"
                enableVibration(true)
                enableLights(true)
            }

            notificationManager?.createNotificationChannels(
                listOf(foregroundChannel, alertChannel)
            )
            Log.d(TAG, "Notification channels created")
        }
    }

    fun createForegroundNotification(
        context: Context,
        title: String = "Detectando Presença",
        subtitle: String = "Scanning WiFi and Bluetooth..."
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
            .setSmallIcon(android.R.drawable.ic_dialog_info)
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
        presenceDetected: Boolean
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("presence_detected", presenceDetected)
            putExtra("notification_message", message)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val icon = if (presenceDetected) {
            android.R.drawable.ic_dialog_info
        } else {
            android.R.drawable.ic_dialog_alert
        }

        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(icon)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0, 500, 250, 500))
            .setLights(0xFF00FF00.toInt(), 1000, 1000)
            .build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager?.notify(
            System.currentTimeMillis().toInt(),
            notification
        )

        Log.d(TAG, "Presence notification sent: $title")
    }
}
