package com.example.presencedetector.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.presencedetector.MainActivity
import com.example.presencedetector.WifiRadarActivity
import com.example.presencedetector.R

/**
 * Utility for managing notifications.
 */
object NotificationUtil {
    private const val TAG = "NotificationUtil"

    // Channel for Foreground Service (Scanning status)
    const val CHANNEL_ID = "presence_detection_channel"
    private const val CHANNEL_NAME = "Presence Detection Service"

    // Channel for Standard Events (Arrivals/Departures) - Medium Priority
    const val INFO_CHANNEL_ID = "presence_info_channel"
    private const val INFO_CHANNEL_NAME = "Presence Events"

    // Channel for Critical Alerts (Intruders, Anti-Theft) - Max Priority
    const val ALERT_CHANNEL_ID = "security_alerts_channel_v2"
    private const val ALERT_CHANNEL_NAME = "Security Alerts"

    private const val GROUP_KEY_PRESENCE = "com.example.presencedetector.PRESENCE_UPDATES"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)

            // 1. Service Channel (Low noise)
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Status of background monitoring services"
                setShowBadge(false)
            }

            // 2. Info Channel (Standard beeps)
            val infoChannel = NotificationChannel(
                INFO_CHANNEL_ID,
                INFO_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Arrivals, departures, and routine updates"
                enableLights(true)
                lightColor = android.graphics.Color.BLUE
            }

            // 3. Critical Alert Channel - Bypass DND
            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                ALERT_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical security alerts (Intruder, Theft, Motion)"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
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
                listOf(serviceChannel, infoChannel, alertChannel)
            )
        }
    }

    fun createForegroundNotification(
        context: Context,
        title: String = "Monitoring Active",
        subtitle: String = "Scanning for devices and motion..."
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
        notificationId: Int? = null,
        secondActionTitle: String? = null,
        secondActionIntent: PendingIntent? = null
    ) {
        // Determine channel based on importance
        val channelId = if (isImportantEvent) ALERT_CHANNEL_ID else INFO_CHANNEL_ID

        createNotificationChannels(context)

        // Default Tap Action -> WifiRadarActivity (Dashboard)
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

        // Logic: Alert Icon for Security, Normal Icon for Info
        val icon = if (isImportantEvent) R.drawable.ic_notification_alert else R.drawable.ic_notification

        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(icon)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(GROUP_KEY_PRESENCE) // Group notifications
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))

        // Configure Priority
        if (isImportantEvent) {
            builder.setPriority(NotificationCompat.PRIORITY_MAX)
            builder.setCategory(NotificationCompat.CATEGORY_ALARM)
            builder.setVibrate(longArrayOf(0, 1000, 500, 1000))
            builder.setDefaults(NotificationCompat.DEFAULT_ALL)
            // Sticky for alarms
            if (actionTitle != null) {
                builder.setFullScreenIntent(pendingIntent, true)
            }
        } else {
            builder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
            builder.setCategory(NotificationCompat.CATEGORY_STATUS)
        }

        // Actions
        if (actionTitle != null && actionIntent != null) {
            builder.addAction(R.drawable.ic_status_inactive, actionTitle, actionIntent)
        }

        if (secondActionTitle != null && secondActionIntent != null) {
            builder.addAction(R.drawable.ic_status_active, secondActionTitle, secondActionIntent)
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val finalId = notificationId ?: System.currentTimeMillis().toInt()
        notificationManager?.notify(finalId, builder.build())

        // Ensure summary exists if multiple notifications pile up (optional but good practice)
        // For now, grouping handles the folding UI automatically on Android 7+.
    }
}
