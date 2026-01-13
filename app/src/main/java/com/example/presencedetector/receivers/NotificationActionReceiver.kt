package com.example.presencedetector.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.example.presencedetector.services.AntiTheftService

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_STOP_ALARM = "com.example.presencedetector.ACTION_STOP_ALARM"
        const val EXTRA_NOTIFICATION_ID = "com.example.presencedetector.EXTRA_NOTIFICATION_ID"
        private const val TAG = "NotificationAction"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_STOP_ALARM) {
            Log.d(TAG, "Received Stop Alarm request")

            // 1. Send broadcast to stop all alarms (PresenceManager, AntiTheft, etc.)
            val stopIntent = Intent(ACTION_STOP_ALARM)
            context.sendBroadcast(stopIntent)

            // 2. Dismiss the notification
            val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
            if (notificationId != -1) {
                NotificationManagerCompat.from(context).cancel(notificationId)
            }

            Toast.makeText(context, "Alarm Silenced", Toast.LENGTH_SHORT).show()
        }
    }
}
