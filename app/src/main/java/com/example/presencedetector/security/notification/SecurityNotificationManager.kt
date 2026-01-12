package com.example.presencedetector.security.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.presencedetector.R
import com.example.presencedetector.security.model.CameraChannel
import com.example.presencedetector.security.ui.CameraStreamActivity

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
        private const val CHANNEL_ID = "security_alerts"
        private const val CHANNEL_NAME = "Alertas de Segurança"
        private const val CHANNEL_DESCRIPTION = "Notificações quando uma pessoa é detectada nas câmeras"
        
        // ID base para notificações (somamos o ID do canal para ter IDs únicos)
        private const val NOTIFICATION_ID_BASE = 10000
    }

    init {
        createNotificationChannel()
    }

    /**
     * Cria o canal de notificação (obrigatório para Android 8.0+).
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Exibe uma notificação de detecção de pessoa.
     * 
     * @param channel Canal da câmera que detectou a pessoa
     * @param snapshot Opcional: Imagem do frame onde a pessoa foi detectada
     */
    fun showDetectionNotification(channel: CameraChannel, snapshot: Bitmap? = null) {
        // TODO: Corrigir a implementação da notificação para ser compatível com a nova CameraStreamActivity
        // O código abaixo foi comentado para permitir o build do projeto, pois as constantes
        // EXTRA_CHANNEL_ID, EXTRA_CHANNEL_NAME e EXTRA_RTSP_URL não existem mais na CameraStreamActivity.

        /*
        // Intent para abrir a câmera específica quando clicar na notificação
        val intent = Intent(context, CameraStreamActivity::class.java).apply {
            putExtra(CameraStreamActivity.EXTRA_CHANNEL_ID, channel.id)
            putExtra(CameraStreamActivity.EXTRA_CHANNEL_NAME, channel.name)
            putExtra(CameraStreamActivity.EXTRA_RTSP_URL, channel.rtspUrl)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            channel.id, // Request code único por canal
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // TODO: Criar ícone próprio
            .setContentTitle("🚨 Movimento detectado")
            .setContentText(channel.name)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Pessoa detectada na ${channel.name}. Toque para ver ao vivo."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500)) // Padrão de vibração de alerta

        // Adiciona imagem se disponível
        if (snapshot != null) {
            builder.setLargeIcon(snapshot)
            builder.setStyle(NotificationCompat.BigPictureStyle()
                .bigPicture(snapshot)
                .bigLargeIcon(null as Bitmap?) // Remove ícone grande quando expandido
                .setSummaryText("Pessoa detectada na ${channel.name}"))
        }

        try {
            NotificationManagerCompat.from(context).notify(
                NOTIFICATION_ID_BASE + channel.id,
                builder.build()
            )
        } catch (e: SecurityException) {
            // Permissão POST_NOTIFICATIONS não concedida em Android 13+
            android.util.Log.e("SecurityNotificationManager", 
                "Sem permissão para notificações: ${e.message}")
        }
        */
    }

    /**
     * Cancela notificação de um canal específico.
     */
    fun cancelNotification(channelId: Int) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_BASE + channelId)
    }

    /**
     * Cancela todas as notificações de segurança.
     */
    fun cancelAllNotifications() {
        NotificationManagerCompat.from(context).cancelAll()
    }
}
