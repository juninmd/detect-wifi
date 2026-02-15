package com.example.presencedetector.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.example.presencedetector.utils.PreferencesUtil

class NotificationActionReceiver : BroadcastReceiver() {

  companion object {
    const val ACTION_STOP_ALARM = "com.example.presencedetector.ACTION_STOP_ALARM"
    const val ACTION_SNOOZE = "com.example.presencedetector.ACTION_SNOOZE"
    const val ACTION_PANIC = "com.example.presencedetector.ACTION_PANIC"
    const val ACTION_MARK_SAFE = "com.example.presencedetector.ACTION_MARK_SAFE"
    const val ACTION_ENABLE_ANTITHEFT = "com.example.presencedetector.ACTION_ENABLE_ANTITHEFT"
    const val EXTRA_NOTIFICATION_ID = "com.example.presencedetector.EXTRA_NOTIFICATION_ID"
    const val EXTRA_BSSID = "com.example.presencedetector.EXTRA_BSSID"
    private const val TAG = "NotificationAction"
  }

  override fun onReceive(context: Context, intent: Intent) {
    val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
    val notificationManager = NotificationManagerCompat.from(context)

    when (intent.action) {
      ACTION_STOP_ALARM -> {
        Log.d(TAG, "Received Stop Alarm request")

        // Stop AntiTheftService directly
        val serviceIntent =
          Intent(context, com.example.presencedetector.services.AntiTheftService::class.java)
            .apply { action = com.example.presencedetector.services.AntiTheftService.ACTION_STOP }
        context.startService(serviceIntent)

        // 2. Dismiss the notification
        if (notificationId != -1) {
          notificationManager.cancel(notificationId)
        }

        Toast.makeText(context, "Alarme Parado", Toast.LENGTH_SHORT).show()
      }
      ACTION_SNOOZE -> {
        Log.d(TAG, "Received Snooze request")

        // Send command to Service to snooze
        val serviceIntent =
          Intent(context, com.example.presencedetector.services.AntiTheftService::class.java)
            .apply { action = com.example.presencedetector.services.AntiTheftService.ACTION_SNOOZE }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
          context.startForegroundService(serviceIntent)
        } else {
          context.startService(serviceIntent)
        }

        Toast.makeText(context, "Soneca (30s)...", Toast.LENGTH_SHORT).show()
      }
      ACTION_PANIC -> {
        Log.d(TAG, "Received PANIC request")
        Toast.makeText(context, "🚨 ALERTA DE PÂNICO ENVIADO! 🚨", Toast.LENGTH_LONG).show()

        // Send Telegram
        val telegramService = com.example.presencedetector.services.TelegramService(context)
        telegramService.sendMessage("🆘 SOS: Botão de PÂNICO pressionado via Notificação!")
      }
      ACTION_MARK_SAFE -> {
        val bssid = intent.getStringExtra(EXTRA_BSSID)
        Log.d(TAG, "Received Mark Safe request. BSSID: $bssid")

        if (!bssid.isNullOrEmpty()) {
          val prefs = PreferencesUtil(context)
          // Mark as trusted by giving it a nickname
          prefs.saveNickname(bssid, "Trusted Device")
          // Also ensure we don't alert again immediately
          prefs.trackDetection(bssid)

          Toast.makeText(context, "Device marked as Safe", Toast.LENGTH_SHORT).show()
        } else {
            // No BSSID means it's a device security alarm (AntiTheft)
            // Log that it was a false alarm / safe
            PreferencesUtil(context).logSystemEvent("Marked as Safe by User")
            Toast.makeText(context, "Alarme cancelado (Seguro)", Toast.LENGTH_SHORT).show()
        }

        // Stop alarm just in case it's ringing
        val serviceIntent =
          Intent(context, com.example.presencedetector.services.AntiTheftService::class.java)
            .apply { action = com.example.presencedetector.services.AntiTheftService.ACTION_STOP }
        context.startService(serviceIntent)

        if (notificationId != -1) {
          notificationManager.cancel(notificationId)
        }
      }
      ACTION_ENABLE_ANTITHEFT -> {
        Log.d(TAG, "Received Enable Anti-Theft request")

        val serviceIntent =
          Intent(context, com.example.presencedetector.services.AntiTheftService::class.java)
            .apply { action = com.example.presencedetector.services.AntiTheftService.ACTION_START }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
          context.startForegroundService(serviceIntent)
        } else {
          context.startService(serviceIntent)
        }

        Toast.makeText(context, "Anti-Theft Armed", Toast.LENGTH_SHORT).show()

        if (notificationId != -1) {
          notificationManager.cancel(notificationId)
        }
      }
    }
  }
}
