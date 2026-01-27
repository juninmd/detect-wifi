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

                // 1. Send broadcast to stop all alarms (PresenceManager, AntiTheft, etc.)
                val stopIntent = Intent(ACTION_STOP_ALARM)
                context.sendBroadcast(stopIntent)

                // 2. Dismiss the notification
                if (notificationId != -1) {
                    notificationManager.cancel(notificationId)
                }

                Toast.makeText(context, "Alarm Silenced", Toast.LENGTH_SHORT).show()
            }

            ACTION_MARK_SAFE -> {
                val bssid = intent.getStringExtra(EXTRA_BSSID)
                Log.d(TAG, "Received Mark Safe request for BSSID: $bssid")

                if (!bssid.isNullOrEmpty()) {
                    val prefs = PreferencesUtil(context)
                    // Mark as trusted by giving it a nickname
                    prefs.saveNickname(bssid, "Trusted Device")
                    // Also ensure we don't alert again immediately
                    prefs.trackDetection(bssid)

                    Toast.makeText(context, "Device marked as Safe", Toast.LENGTH_SHORT).show()
                }

                // Stop alarm just in case it's ringing
                val stopIntent = Intent(ACTION_STOP_ALARM)
                context.sendBroadcast(stopIntent)

                if (notificationId != -1) {
                    notificationManager.cancel(notificationId)
                }
            }

            ACTION_ENABLE_ANTITHEFT -> {
                Log.d(TAG, "Received Enable Anti-Theft request")

                val serviceIntent = Intent(context, com.example.presencedetector.services.AntiTheftService::class.java).apply {
                    action = com.example.presencedetector.services.AntiTheftService.ACTION_START
                }

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
