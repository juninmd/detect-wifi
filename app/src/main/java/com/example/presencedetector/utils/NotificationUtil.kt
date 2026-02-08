package com.example.presencedetector.utils

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.presencedetector.MainActivity
import com.example.presencedetector.WifiRadarActivity
import com.example.presencedetector.R
import com.example.presencedetector.receivers.NotificationActionReceiver

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

    // 6. New Specific Channels
    const val HOME_SECURITY_CHANNEL_ID = "home_security_channel"
    const val MOBILE_SECURITY_CHANNEL_ID = "mobile_security_channel"

    private const val GROUP_KEY_PRESENCE = "com.example.presencedetector.PRESENCE_UPDATES"

    // Deprecated constant compatibility if needed by other classes not yet updated
    const val ALERT_CHANNEL_ID = SECURITY_CHANNEL_ID

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

            // 6. Home Security Channel
            val homeChannel = NotificationChannel(
                HOME_SECURITY_CHANNEL_ID,
                "Segurança Residencial",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificações de monitoramento WiFi e presença em casa"
                setShowBadge(false)
            }

            // 7. Mobile Security Channel
            val mobileChannel = NotificationChannel(
                MOBILE_SECURITY_CHANNEL_ID,
                "Segurança do Celular",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificações de monitoramento anti-furto (bolso, movimento)"
                setShowBadge(true)
            }

            notificationManager?.createNotificationChannels(
                listOf(serviceChannel, infoChannel, silentChannel, securityChannel, batteryChannel, homeChannel, mobileChannel)
            )
        }
    }

    /**
     * Send a standard notification for presence events.
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

        if (checkPermission(context)) {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(notificationId ?: System.currentTimeMillis().toInt(), builder.build())
        }
    }

    /**
     * Send a Critical Security Alert.
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
            .setSmallIcon(R.drawable.ic_notification_alert)
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

        if (checkPermission(context)) {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(notificationId, builder.build())
        }
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

        if (checkPermission(context)) {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(2001, builder.build())
        }
    }

    fun sendIntruderAlert(context: Context, bitmap: Bitmap) {
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

        val builder = NotificationCompat.Builder(context, SECURITY_CHANNEL_ID)
            .setContentTitle("🚨 INTRUSO DETECTADO!")
            .setContentText("Uma foto foi capturada durante o alerta de segurança.")
            .setSmallIcon(R.drawable.ic_notification_alert)
            .setLargeIcon(bitmap)
            .setStyle(NotificationCompat.BigPictureStyle()
                .bigPicture(bitmap)
                .bigLargeIcon(null as Bitmap?))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)

        if (checkPermission(context)) {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(3001, builder.build())
        }
    }

    fun sendPanicAlert(context: Context) {
        createNotificationChannels(context)

        val emergencyIntent = Intent(Intent.ACTION_DIAL).apply {
            data = android.net.Uri.parse("tel:190")
        }
        val pendingEmergencyIntent = PendingIntent.getActivity(
            context,
            4001,
            emergencyIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_STOP_ALARM
        }
        val pendingStopIntent = PendingIntent.getBroadcast(
            context,
            4002,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, SECURITY_CHANNEL_ID)
            .setContentTitle("🆘 ALARME DE PÂNICO ATIVO")
            .setContentText("Sua segurança está em risco? A ajuda está a um toque.")
            .setSmallIcon(android.R.drawable.ic_lock_power_off)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setVibrate(longArrayOf(0, 1000, 1000, 1000, 1000))
            .addAction(android.R.drawable.ic_menu_call, "LIGAR 190", pendingEmergencyIntent)
            .addAction(android.R.drawable.ic_media_pause, "PARAR ALARME", pendingStopIntent)
            .setOngoing(true)
            .setAutoCancel(false)

        if (checkPermission(context)) {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(1000, builder.build())
        }
    }

    fun createForegroundNotification(
        context: Context,
        title: String = "Monitoramento Ativo",
        subtitle: String = "Verificando sensores...",
        channelId: String = CHANNEL_ID
    ): Notification {
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

        return NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun checkPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
