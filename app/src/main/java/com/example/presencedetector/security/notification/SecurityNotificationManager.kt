package com.example.presencedetector.security.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.presencedetector.R
import com.example.presencedetector.security.model.CameraChannel
import com.example.presencedetector.security.ui.CameraStreamActivity
import com.example.presencedetector.utils.NotificationUtil

/**
 * Gerencia notificações de alertas de segurança.
 *
 * Responsabilidades:
 * - Criar canal de notificação para Android 8+
 * - Exibir notificações de detecção de pessoa
 * - Configurar deep-link para abrir câmera específica
 */
class SecurityNotificationManager(private val context: Context) {

  companion object {
    // ID base para notificações (somamos o ID do canal para ter IDs únicos)
    private const val NOTIFICATION_ID_BASE = 10000
  }

  init {
    NotificationUtil.createNotificationChannels(context)
  }

  /**
   * Exibe uma notificação de detecção de pessoa.
   *
   * @param channel Canal da câmera que detectou a pessoa
   * @param snapshot Opcional: Imagem do frame onde a pessoa foi detectada
   */
  fun showDetectionNotification(channel: CameraChannel, snapshot: Bitmap? = null) {
    // Intent para abrir a câmera específica quando clicar na notificação
    val intent =
      Intent(context, CameraStreamActivity::class.java).apply {
        putExtra(CameraStreamActivity.EXTRA_CAMERA_URL, channel.rtspUrl)
        putExtra(CameraStreamActivity.EXTRA_CAMERA_NAME, channel.name)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }

    val pendingIntent =
      PendingIntent.getActivity(
        context,
        channel.id, // Request code único por canal
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val builder =
      NotificationCompat.Builder(context, NotificationUtil.SECURITY_ALERTS_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground) // TODO: Criar ícone próprio
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
        .setVibrate(longArrayOf(0, 500, 200, 500)) // Padrão de vibração de alerta

    // Adiciona imagem se disponível
    if (snapshot != null) {
      builder.setLargeIcon(snapshot)
      builder.setStyle(
        NotificationCompat.BigPictureStyle()
          .bigPicture(snapshot)
          .bigLargeIcon(null as Bitmap?) // Remove ícone grande quando expandido
          .setSummaryText("Pessoa detectada na ${channel.name}")
      )
    }

    try {
      // Check permission is handled by caller or assumed granted if notification service is running
      val notificationManager = NotificationManagerCompat.from(context)
      notificationManager.notify(NOTIFICATION_ID_BASE + channel.id, builder.build())
    } catch (e: SecurityException) {
      // Permissão POST_NOTIFICATIONS não concedida em Android 13+
      android.util.Log.e(
        "SecurityNotificationManager",
        "Sem permissão para notificações: ${e.message}",
      )
    }
  }

  /** Cancela notificação de um canal específico. */
  fun cancelNotification(channelId: Int) {
    NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_BASE + channelId)
  }

  /** Cancela todas as notificações de segurança. */
  fun cancelAllNotifications() {
    NotificationManagerCompat.from(context).cancelAll()
  }
}
