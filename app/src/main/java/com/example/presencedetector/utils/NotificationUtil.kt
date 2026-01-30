package com.example.presencedetector.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
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

    // 1. Foreground Service Channel (Low noise, sticky)
    const val CHANNEL_ID = "presence_detection_channel"

    // 2. Info Channel (Standard beeps for non-critical events like arrivals)
    const val INFO_CHANNEL_ID = "presence_info_channel"

    // 3. Silent Channel (No sound, for routine logs)
    const val SILENT_CHANNEL_ID = "presence_silent_channel"

    // 4. Critical Security Channel (Max Priority, Bypasses DND, Loud)
    const val SECURITY_CHANNEL_ID = "security_critical_channel_v1"

    // 5. Battery Channel
    const val BATTERY_CHANNEL_ID = "battery_alert_channel"

    private const val GROUP_KEY_PRESENCE = "com.example.presencedetector.PRESENCE_UPDATES"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)

            // 1. Service Channel
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.channel_service_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.channel_service_desc)
                setShowBadge(false)
            }

            // 2. Info Channel
            val infoChannel = NotificationChannel(
                INFO_CHANNEL_ID,
                context.getString(R.string.channel_info_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.channel_info_desc)
                enableLights(true)
                lightColor = android.graphics.Color.BLUE
            }

            // 3. Silent Channel
            val silentChannel = NotificationChannel(
                SILENT_CHANNEL_ID,
                "Eventos Silenciosos",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificações de rotina sem som"
                setShowBadge(false)
            }

            // 4. Critical Security Channel
            val securityChannel = NotificationChannel(
                SECURITY_CHANNEL_ID,
                "Alerta de Segurança Crítico",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas de intrusão e roubo. Toca mesmo em modo não perturbe."
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
                enableLights(true)
                lightColor = android.graphics.Color.RED
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setBypassDnd(true) // Crucial for security
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }

            // 5. Battery Channel
            val batteryChannel = NotificationChannel(
                BATTERY_CHANNEL_ID,
                context.getString(R.string.channel_battery_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_battery_desc)
                enableVibration(true)
                enableLights(true)
                lightColor = android.graphics.Color.YELLOW
            }

            notificationManager?.createNotificationChannels(
                listOf(serviceChannel, infoChannel, silentChannel, securityChannel, batteryChannel)
            )
        }
    }

    /**
     * Send a standard notification for presence events.
     * @param isImportantEvent If true, uses INFO channel (Sound). If false, uses SILENT channel.
     */
    fun sendPresenceNotification(
        context: Context,
        title: String,
        message: String,
        isImportantEvent: Boolean,
        actionTitle: String? = null,
        actionIntent: PendingIntent? = null,
        notificationId: Int? = null,
        secondActionTitle: String? = null,
        secondActionIntent: PendingIntent? = null,
        iconResId: Int? = null
    ) {
        createNotificationChannels(context)

        val channelId = if (isImportantEvent) INFO_CHANNEL_ID else SILENT_CHANNEL_ID

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

        val icon = iconResId ?: R.drawable.ic_notification

        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(icon)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(GROUP_KEY_PRESENCE)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))

        if (isImportantEvent) {
            builder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
        } else {
            builder.setPriority(NotificationCompat.PRIORITY_LOW)
        }

        if (actionTitle != null && actionIntent != null) {
            builder.addAction(R.drawable.ic_status_inactive, actionTitle, actionIntent)
        }
        if (secondActionTitle != null && secondActionIntent != null) {
            builder.addAction(R.drawable.ic_status_active, secondActionTitle, secondActionIntent)
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager?.notify(notificationId ?: System.currentTimeMillis().toInt(), builder.build())
    }

    /**
     * Send a Critical Security Alert.
     * Always uses SECURITY_CHANNEL_ID (High Priority, Alarm Sound, Bypass DND).
     */
    fun sendCriticalAlert(
        context: Context,
        title: String,
        message: String,
        notificationId: Int,
        fullScreenIntent: PendingIntent? = null,
        actions: List<NotificationCompat.Action> = emptyList()
    ) {
        createNotificationChannels(context)

        val builder = NotificationCompat.Builder(context, SECURITY_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification_alert) // Ensure this resource exists or use generic
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setOngoing(true)
            .setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000))
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))

        if (fullScreenIntent != null) {
            builder.setFullScreenIntent(fullScreenIntent, true)
        }

        actions.forEach { builder.addAction(it) }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager?.notify(notificationId, builder.build())
    }

    fun sendBatteryAlert(context: Context, level: Int) {
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

        val builder = NotificationCompat.Builder(context, BATTERY_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.notif_battery_warning))
            .setContentText(context.getString(R.string.notif_battery_desc, level))
            .setSmallIcon(android.R.drawable.ic_lock_idle_low_battery)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SYSTEM)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager?.notify(2001, builder.build())
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

    // Deprecated constant compatibility if needed by other classes not yet updated
    const val ALERT_CHANNEL_ID = SECURITY_CHANNEL_ID
}
