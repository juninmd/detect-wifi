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
import androidx.annotation.RequiresApi
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

    // Channel IDs
    const val CHANNEL_ID = "presence_detection_channel"
    const val INFO_CHANNEL_ID = "presence_info_channel"
    const val SILENT_CHANNEL_ID = "presence_silent_channel"
    const val SECURITY_CHANNEL_ID = "security_critical_channel_v1"
    const val BATTERY_CHANNEL_ID = "battery_alert_channel"
    const val HOME_SECURITY_CHANNEL_ID = "home_security_channel"
    const val MOBILE_SECURITY_CHANNEL_ID = "mobile_security_channel"

    private const val GROUP_KEY_PRESENCE = "com.example.presencedetector.PRESENCE_UPDATES"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return

            val channels = mutableListOf<NotificationChannel>()

            channels.add(createChannel(CHANNEL_ID, context.getString(R.string.channel_service_name), NotificationManager.IMPORTANCE_LOW, context.getString(R.string.channel_service_desc)) {
                setShowBadge(false)
            })

            channels.add(createChannel(INFO_CHANNEL_ID, context.getString(R.string.channel_info_name), NotificationManager.IMPORTANCE_DEFAULT, context.getString(R.string.channel_info_desc)) {
                enableLights(true)
                lightColor = android.graphics.Color.BLUE
            })

            channels.add(createChannel(SILENT_CHANNEL_ID, "Eventos Silenciosos", NotificationManager.IMPORTANCE_LOW, "Notificações de rotina sem som") {
                setShowBadge(false)
            })

            channels.add(createChannel(SECURITY_CHANNEL_ID, "Alerta de Segurança Crítico", NotificationManager.IMPORTANCE_HIGH, "Alertas de intrusão e roubo. Toca mesmo em modo não perturbe.") {
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
                enableLights(true)
                lightColor = android.graphics.Color.RED
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            })

            channels.add(createChannel(BATTERY_CHANNEL_ID, context.getString(R.string.channel_battery_name), NotificationManager.IMPORTANCE_HIGH, context.getString(R.string.channel_battery_desc)) {
                enableVibration(true)
                enableLights(true)
                lightColor = android.graphics.Color.YELLOW
            })

            channels.add(createChannel(HOME_SECURITY_CHANNEL_ID, "Segurança Residencial", NotificationManager.IMPORTANCE_LOW, "Notificações de monitoramento WiFi e presença em casa") {
                setShowBadge(false)
            })

            channels.add(createChannel(MOBILE_SECURITY_CHANNEL_ID, "Segurança do Celular", NotificationManager.IMPORTANCE_LOW, "Notificações de monitoramento anti-furto (bolso, movimento)") {
                setShowBadge(true)
            })

            notificationManager.createNotificationChannels(channels)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel(
        id: String,
        name: String,
        importance: Int,
        desc: String? = null,
        configure: (NotificationChannel.() -> Unit)? = null
    ): NotificationChannel {
        return NotificationChannel(id, name, importance).apply {
            description = desc
            configure?.invoke(this)
        }
    }

    private fun buildBaseNotification(
        context: Context,
        channelId: String,
        title: String,
        message: String,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(priority)
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
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

        val builder = buildBaseNotification(context, channelId, title, message, if (isImportantEvent) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setGroup(GROUP_KEY_PRESENCE)

        if (iconResId != null) {
            builder.setSmallIcon(iconResId)
        }

        if (actionTitle != null && actionIntent != null) {
            builder.addAction(R.drawable.ic_status_inactive, actionTitle, actionIntent)
        }
        if (secondActionTitle != null && secondActionIntent != null) {
            builder.addAction(R.drawable.ic_status_active, secondActionTitle, secondActionIntent)
        }

        notify(context, notificationId ?: System.currentTimeMillis().toInt(), builder.build())
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

        val builder = buildBaseNotification(context, SECURITY_CHANNEL_ID, title, message, NotificationCompat.PRIORITY_MAX)
            .setSmallIcon(R.drawable.ic_notification_alert)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setOngoing(true)
            .setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000))

        if (fullScreenIntent != null) {
            builder.setFullScreenIntent(fullScreenIntent, true)
        }

        actions.forEach { builder.addAction(it) }

        notify(context, notificationId, builder.build())
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

        val builder = buildBaseNotification(context, BATTERY_CHANNEL_ID, context.getString(R.string.notif_battery_warning), context.getString(R.string.notif_battery_desc, level), NotificationCompat.PRIORITY_HIGH)
            .setSmallIcon(android.R.drawable.ic_lock_idle_low_battery)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SYSTEM)

        notify(context, 2001, builder.build())
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

        val builder = buildBaseNotification(context, SECURITY_CHANNEL_ID, "🚨 INTRUSO DETECTADO!", "Uma foto foi capturada durante o alerta de segurança.", NotificationCompat.PRIORITY_MAX)
            .setSmallIcon(R.drawable.ic_notification_alert)
            .setLargeIcon(bitmap)
            .setStyle(NotificationCompat.BigPictureStyle()
                .bigPicture(bitmap)
                .bigLargeIcon(null as Bitmap?))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)

        notify(context, 3001, builder.build())
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

        val builder = buildBaseNotification(context, SECURITY_CHANNEL_ID, "🆘 ALARME DE PÂNICO ATIVO", "Sua segurança está em risco? A ajuda está a um toque.", NotificationCompat.PRIORITY_MAX)
            .setSmallIcon(android.R.drawable.ic_lock_power_off)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setVibrate(longArrayOf(0, 1000, 1000, 1000, 1000))
            .addAction(android.R.drawable.ic_menu_call, "LIGAR 190", pendingEmergencyIntent)
            .addAction(android.R.drawable.ic_media_pause, "PARAR ALARME", pendingStopIntent)
            .setOngoing(true)
            .setAutoCancel(false)

        notify(context, 1000, builder.build())
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

        return buildBaseNotification(context, channelId, title, subtitle, NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
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

    private fun notify(context: Context, id: Int, notification: Notification) {
        if (checkPermission(context)) {
            NotificationManagerCompat.from(context).notify(id, notification)
        }
    }
}
