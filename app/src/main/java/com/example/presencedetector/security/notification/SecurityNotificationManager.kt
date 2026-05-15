package com.example.presencedetector.security.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.presencedetector.MainActivity
import com.example.presencedetector.R
import com.example.presencedetector.security.model.CameraChannel
import com.example.presencedetector.security.ui.CameraStreamActivity
import com.example.presencedetector.utils.NotificationUtil
import com.example.presencedetector.receivers.NotificationActionReceiver

/**
 * Gerencia notificações de alertas de segurança (Residenciais e do Celular).
 */
class SecurityNotificationManager(private val context: Context) {

  companion object {
    private const val NOTIFICATION_ID_BASE = 10000
    private const val PANIC_NOTIFICATION_ID = 1000
    private const val BATTERY_NOTIFICATION_ID = 2001
    private const val INTRUDER_NOTIFICATION_ID = 3001
  }

  init {
    NotificationUtil.createNotificationChannels(context)
  }

  fun showHomeSecurityAlert(
    title: String,
    message: String,
    notificationId: Int,
    fullScreenIntent: PendingIntent? = null,
    actions: List<NotificationCompat.Action> = emptyList(),
  ) {
    val builder =
      NotificationUtil.buildBaseNotification(
          context,
          NotificationUtil.SECURITY_CHANNEL_ID,
          title,
          message,
          NotificationCompat.PRIORITY_MAX,
        )
        .setSmallIcon(R.drawable.ic_notification_alert)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setAutoCancel(false)
        .setOngoing(true)
        .setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000))

    if (fullScreenIntent != null) {
      builder.setFullScreenIntent(fullScreenIntent, true)
    }

    for (action in actions) { builder.addAction(action) }

    val safeIntent =
      Intent(context, NotificationActionReceiver::class.java).apply {
        action = NotificationActionReceiver.ACTION_MARK_SAFE
        putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
      }
    val pendingSafeIntent =
      PendingIntent.getBroadcast(
        context,
        notificationId + 99,
        safeIntent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
      )
    builder.addAction(android.R.drawable.ic_menu_save, "Marcar Seguro", pendingSafeIntent)

    notify(notificationId, builder.build())
  }

  fun showIntruderAlert(bitmap: Bitmap) {
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

    val builder =
      NotificationUtil.buildBaseNotification(
          context,
          NotificationUtil.SECURITY_CHANNEL_ID,
          "🚨 INTRUSO DETECTADO!",
          "Uma foto foi capturada durante o alerta de segurança.",
          NotificationCompat.PRIORITY_MAX,
        )
        .setSmallIcon(R.drawable.ic_notification_alert)
        .setLargeIcon(bitmap)
        .setStyle(
          NotificationCompat.BigPictureStyle().bigPicture(bitmap).bigLargeIcon(null as Bitmap?)
        )
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setContentIntent(pendingIntent)
        .setAutoCancel(false)
        .setOngoing(true)

    val safeIntent =
      Intent(context, NotificationActionReceiver::class.java).apply {
        action = NotificationActionReceiver.ACTION_MARK_SAFE
        putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, INTRUDER_NOTIFICATION_ID)
      }
    val pendingSafeIntent =
      PendingIntent.getBroadcast(
        context,
        INTRUDER_NOTIFICATION_ID + 99,
        safeIntent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
      )
    builder.addAction(android.R.drawable.ic_menu_save, "Marcar Seguro", pendingSafeIntent)

    val emergencyIntent =
      Intent(Intent.ACTION_DIAL).apply { data = android.net.Uri.parse("tel:190") }
    val pendingEmergencyIntent =
      PendingIntent.getActivity(context, 4005, emergencyIntent, PendingIntent.FLAG_IMMUTABLE)
    builder.addAction(android.R.drawable.ic_menu_call, "LIGAR 190", pendingEmergencyIntent)

    notify(INTRUDER_NOTIFICATION_ID, builder.build())
  }

  fun showPanicAlert() {
    val emergencyIntent =
      Intent(Intent.ACTION_DIAL).apply { data = android.net.Uri.parse("tel:190") }
    val pendingEmergencyIntent =
      PendingIntent.getActivity(context, 4001, emergencyIntent, PendingIntent.FLAG_IMMUTABLE)

    val stopIntent =
      Intent(context, NotificationActionReceiver::class.java).apply {
        action = NotificationActionReceiver.ACTION_STOP_ALARM
      }
    val pendingStopIntent =
      PendingIntent.getBroadcast(
        context,
        4002,
        stopIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val safeIntent =
      Intent(context, NotificationActionReceiver::class.java).apply {
        action = NotificationActionReceiver.ACTION_MARK_SAFE
        putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, PANIC_NOTIFICATION_ID)
      }
    val pendingSafeIntent =
      PendingIntent.getBroadcast(
        context,
        PANIC_NOTIFICATION_ID + 99,
        safeIntent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
      )

    val builder =
      NotificationUtil.buildBaseNotification(
          context,
          NotificationUtil.SECURITY_CHANNEL_ID,
          "🆘 ALARME DE PÂNICO ATIVO",
          "Sua segurança está em risco? A ajuda está a um toque.",
          NotificationCompat.PRIORITY_MAX,
        )
        .setSmallIcon(android.R.drawable.ic_lock_power_off)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setVibrate(longArrayOf(0, 1000, 1000, 1000, 1000))
        .addAction(android.R.drawable.ic_menu_call, "LIGAR 190", pendingEmergencyIntent)
        .addAction(android.R.drawable.ic_media_pause, "PARAR ALARME", pendingStopIntent)
        .addAction(android.R.drawable.ic_menu_save, "Estou Seguro", pendingSafeIntent)
        .setOngoing(true)
        .setAutoCancel(false)

    notify(PANIC_NOTIFICATION_ID, builder.build())
  }

  fun showMobileSecurityAlert(level: Int) {
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

    val builder =
      NotificationUtil.buildBaseNotification(
          context,
          NotificationUtil.BATTERY_CHANNEL_ID,
          context.getString(R.string.notif_battery_warning),
          context.getString(R.string.notif_battery_desc, level),
          NotificationCompat.PRIORITY_HIGH,
        )
        .setSmallIcon(android.R.drawable.ic_lock_idle_low_battery)
        .setContentIntent(pendingIntent)
        .setCategory(NotificationCompat.CATEGORY_SYSTEM)

    notify(BATTERY_NOTIFICATION_ID, builder.build())
  }

  fun showDetectionNotification(channel: CameraChannel, snapshot: Bitmap? = null) {
    val intent =
      Intent(context, CameraStreamActivity::class.java).apply {
        putExtra(CameraStreamActivity.EXTRA_CAMERA_URL, channel.rtspUrl)
        putExtra(CameraStreamActivity.EXTRA_CAMERA_NAME, channel.name)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }

    val pendingIntent =
      PendingIntent.getActivity(
        context,
        channel.id,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val builder =
      NotificationCompat.Builder(context, NotificationUtil.SECURITY_ALERTS_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("🚨 Movimento detectado")
        .setContentText(channel.name)
        .setStyle(
          NotificationCompat.BigTextStyle()
            .bigText("Pessoa detectada na ${channel.name}. Toque para ver ao vivo.")
        )
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .setVibrate(longArrayOf(0, 500, 200, 500))

    if (snapshot != null) {
      builder.setLargeIcon(snapshot)
      builder.setStyle(
        NotificationCompat.BigPictureStyle()
          .bigPicture(snapshot)
          .bigLargeIcon(null as Bitmap?)
          .setSummaryText("Pessoa detectada na ${channel.name}")
      )
    }

    val safeIntent =
      Intent(context, NotificationActionReceiver::class.java).apply {
        action = NotificationActionReceiver.ACTION_MARK_SAFE
        putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, NOTIFICATION_ID_BASE + channel.id)
      }
    val pendingSafeIntent =
      PendingIntent.getBroadcast(
        context,
        channel.id + 99,
        safeIntent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
      )
    builder.addAction(android.R.drawable.ic_menu_save, "Marcar Seguro", pendingSafeIntent)

    val stopIntent =
      Intent(context, NotificationActionReceiver::class.java).apply {
        action = NotificationActionReceiver.ACTION_STOP_ALARM
        putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, NOTIFICATION_ID_BASE + channel.id)
      }
    val pendingStopIntent =
      PendingIntent.getBroadcast(
        context,
        channel.id + 100,
        stopIntent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
      )
    builder.addAction(android.R.drawable.ic_media_pause, "Desativar", pendingStopIntent)

    notify(NOTIFICATION_ID_BASE + channel.id, builder.build())
  }

  fun cancelNotification(channelId: Int) {
    NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_BASE + channelId)
  }

  fun cancelAllNotifications() {
    NotificationManagerCompat.from(context).cancelAll()
  }

  private fun notify(id: Int, notification: android.app.Notification) {
    if (NotificationUtil.checkPermission(context)) {
      try {
        NotificationManagerCompat.from(context).notify(id, notification)
      } catch (e: SecurityException) {
        android.util.Log.e("SecurityNotificationManager", "Sem permissão para notificações: ${e.message}")
      }
    }
  }
}
