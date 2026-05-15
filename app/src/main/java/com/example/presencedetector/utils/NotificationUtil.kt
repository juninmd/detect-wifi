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
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.presencedetector.MainActivity
import com.example.presencedetector.R
import com.example.presencedetector.WifiRadarActivity
import com.example.presencedetector.receivers.NotificationActionReceiver

/** Utility for managing notifications. */
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
  const val SECURITY_ALERTS_CHANNEL_ID = "security_alerts"

  private const val GROUP_KEY_PRESENCE = "com.example.presencedetector.PRESENCE_UPDATES"

  private data class ChannelDefinition(
    val id: String,
    val name: String,
    val importance: Int,
    val description: String? = null,
    val configure: (NotificationChannel.() -> Unit)? = null,
  )

  fun createNotificationChannels(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return

      val channels =
        getChannelDefinitions(context).map { def ->
          NotificationChannel(def.id, def.name, def.importance).apply {
            description = def.description
            def.configure?.invoke(this)
          }
        }
      notificationManager.createNotificationChannels(channels)
    }
  }

  private fun getChannelDefinitions(context: Context): List<ChannelDefinition> {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return emptyList()

    return listOf(
      ChannelDefinition(
        CHANNEL_ID,
        context.getString(R.string.channel_service_name),
        NotificationManager.IMPORTANCE_LOW,
        context.getString(R.string.channel_service_desc),
      ) {
        setShowBadge(false)
      },
      ChannelDefinition(
        INFO_CHANNEL_ID,
        context.getString(R.string.channel_info_name),
        NotificationManager.IMPORTANCE_DEFAULT,
        context.getString(R.string.channel_info_desc),
      ) {
        enableLights(true)
        lightColor = android.graphics.Color.BLUE
      },
      ChannelDefinition(
        SILENT_CHANNEL_ID,
        "Eventos Silenciosos",
        NotificationManager.IMPORTANCE_LOW,
        "Notificações de rotina sem som",
      ) {
        setShowBadge(false)
      },
      createSecurityChannelDefinition(),
      ChannelDefinition(
        BATTERY_CHANNEL_ID,
        context.getString(R.string.channel_battery_name),
        NotificationManager.IMPORTANCE_HIGH,
        context.getString(R.string.channel_battery_desc),
      ) {
        enableVibration(true)
        enableLights(true)
        lightColor = android.graphics.Color.YELLOW
      },
      ChannelDefinition(
        HOME_SECURITY_CHANNEL_ID,
        "Segurança Residencial",
        NotificationManager.IMPORTANCE_LOW,
        "Notificações de monitoramento WiFi e presença em casa",
      ) {
        setShowBadge(false)
      },
      ChannelDefinition(
        MOBILE_SECURITY_CHANNEL_ID,
        "Segurança do Celular",
        NotificationManager.IMPORTANCE_LOW,
        "Notificações de monitoramento anti-furto (bolso, movimento)",
      ) {
        setShowBadge(true)
      },
      ChannelDefinition(
        SECURITY_ALERTS_CHANNEL_ID,
        "Alertas de Segurança",
        NotificationManager.IMPORTANCE_HIGH,
        "Notificações quando uma pessoa é detectada nas câmeras",
      ) {
        enableVibration(true)
        enableLights(true)
      },
    )
  }

  private fun createSecurityChannelDefinition(): ChannelDefinition {
    return ChannelDefinition(
      SECURITY_CHANNEL_ID,
      "Alerta de Segurança Crítico",
      NotificationManager.IMPORTANCE_HIGH,
      "Alertas de intrusão e roubo. Toca mesmo em modo não perturbe.",
    ) {
      enableVibration(true)
      vibrationPattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
      enableLights(true)
      lightColor = android.graphics.Color.RED
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        setBypassDnd(true)
        setSound(
          RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build(),
        )
      }
    }
  }

  fun buildBaseNotification(
    context: Context,
    channelId: String,
    title: String,
    message: String,
    priority: Int = NotificationCompat.PRIORITY_DEFAULT,
  ): NotificationCompat.Builder {
    return NotificationCompat.Builder(context, channelId)
      .setContentTitle(title)
      .setContentText(message)
      .setStyle(NotificationCompat.BigTextStyle().bigText(message))
      .setPriority(priority)
      .setSmallIcon(R.drawable.ic_notification)
      .setAutoCancel(true)
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
    secondActionIntent: PendingIntent? = null,
    iconResId: Int? = null,
    channelType: String = INFO_CHANNEL_ID,
  ) {
    createNotificationChannels(context)

    val channelId = if (channelType != INFO_CHANNEL_ID || isImportantEvent) channelType else SILENT_CHANNEL_ID

    val intent =
      Intent(context, WifiRadarActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        putExtra("from_notification", true)
      }

    val pendingIntent =
      PendingIntent.getActivity(
        context,
        System.currentTimeMillis().toInt(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val builder =
      buildBaseNotification(
          context,
          channelId,
          title,
          message,
          if (isImportantEvent) NotificationCompat.PRIORITY_DEFAULT
          else NotificationCompat.PRIORITY_LOW,
        )
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





  fun createForegroundNotification(
    context: Context,
    title: String,
    message: String,
    channelId: String,
  ): Notification {
    createNotificationChannels(context)

    val intent =
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
      }
    val pendingIntent =
      PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    return buildBaseNotification(
        context,
        channelId,
        title,
        message,
        NotificationCompat.PRIORITY_LOW,
      )
      .setContentIntent(pendingIntent)
      .setOngoing(true)
      .build()
  }

  fun checkPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
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
