package com.example.presencedetector.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.presencedetector.MainActivity
import com.example.presencedetector.R
import com.example.presencedetector.receivers.NotificationActionReceiver
import com.example.presencedetector.utils.NotificationUtil
import kotlin.math.sqrt

class AntiTheftService : Service(), SensorEventListener {

    companion object {
        private const val TAG = "AntiTheftService"
        const val ACTION_START = "com.example.presencedetector.action.START_ANTITHEFT"
        const val ACTION_STOP = "com.example.presencedetector.action.STOP_ANTITHEFT"
        private const val NOTIFICATION_ID = 999
        private const val ALARM_NOTIFICATION_ID = 1000
        private const val MOVEMENT_THRESHOLD = 1.5f // m/s^2 delta
        private const val GRACE_PERIOD_MS = 5000L // Time to put phone down after arming
    }

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var isArmed = false
    private var isAlarmPlaying = false
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var firstReading = true
    private var armingTime = 0L

    private var alarmRingtone: Ringtone? = null
    private val stopAlarmReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == NotificationActionReceiver.ACTION_STOP_ALARM) {
                stopAlarm()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Register receiver for stop command
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stopAlarmReceiver, IntentFilter(NotificationActionReceiver.ACTION_STOP_ALARM), Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(stopAlarmReceiver, IntentFilter(NotificationActionReceiver.ACTION_STOP_ALARM))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startMonitoring()
            ACTION_STOP -> stopMonitoring()
        }
        return START_STICKY
    }

    private fun startMonitoring() {
        if (isArmed) return

        Log.d(TAG, "Anti-Theft Armed")
        isArmed = true
        firstReading = true
        armingTime = System.currentTimeMillis()

        // Start Foreground
        startForeground(NOTIFICATION_ID, createForegroundNotification())

        // Register Sensor
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun stopMonitoring() {
        Log.d(TAG, "Anti-Theft Disarmed")
        isArmed = false
        stopAlarm()
        sensorManager.unregisterListener(this)
        stopForeground(true)
        stopSelf()
    }

    private fun createForegroundNotification(): Notification {
        NotificationUtil.createNotificationChannels(this)

        val stopIntent = Intent(this, AntiTheftService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStopIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, "presence_detection_channel")
            .setContentTitle("🛡️ Mobile Security Active")
            .setContentText("Motion detector is armed.")
            .setSmallIcon(R.drawable.ic_status_active)
            .setOngoing(true)
            .addAction(R.drawable.ic_status_inactive, "Disarm", pendingStopIntent)

        // Add intent to open app
        val appIntent = Intent(this, MainActivity::class.java)
        val pendingAppIntent = PendingIntent.getActivity(this, 0, appIntent, PendingIntent.FLAG_IMMUTABLE)
        builder.setContentIntent(pendingAppIntent)

        return builder.build()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isArmed || event == null) return

        // Grace period to set the phone down
        if (System.currentTimeMillis() - armingTime < GRACE_PERIOD_MS) {
            lastX = event.values[0]
            lastY = event.values[1]
            lastZ = event.values[2]
            return
        }

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        if (!firstReading) {
            val deltaX = kotlin.math.abs(x - lastX)
            val deltaY = kotlin.math.abs(y - lastY)
            val deltaZ = kotlin.math.abs(z - lastZ)

            // Calculate total magnitude of change
            val totalDelta = sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)

            if (totalDelta > MOVEMENT_THRESHOLD) {
                Log.w(TAG, "Motion Detected! Delta: $totalDelta")
                triggerAlarm()
            }
        } else {
            firstReading = false
        }

        lastX = x
        lastY = y
        lastZ = z
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }

    private fun triggerAlarm() {
        if (isAlarmPlaying) return
        isAlarmPlaying = true

        Log.w(TAG, "TRIGGERING ALARM")

        // 1. Play Sound
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            alarmRingtone = RingtoneManager.getRingtone(applicationContext, alarmUri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                alarmRingtone?.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }
            alarmRingtone?.play()
        } catch (e: Exception) {
            Log.e(TAG, "Error playing alarm", e)
        }

        // 2. Show Alert Notification with Action to Stop
        showAlarmNotification()
    }

    private fun showAlarmNotification() {
        val stopActionIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_STOP_ALARM
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, ALARM_NOTIFICATION_ID)
        }
        val pendingStopIntent = PendingIntent.getBroadcast(
            this,
            1,
            stopActionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "presence_alerts_channel")
            .setContentTitle("🚨 THEFT ALERT!")
            .setContentText("Motion detected on your device!")
            .setSmallIcon(R.drawable.ic_notification_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingStopIntent, true) // Try to wake screen
            .addAction(R.drawable.ic_status_inactive, "STOP ALARM", pendingStopIntent)
            .setDeleteIntent(pendingStopIntent) // Stop if dismissed
            .build()

        val notificationManager = androidx.core.app.NotificationManagerCompat.from(this)
        try {
            notificationManager.notify(ALARM_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission missing for notification", e)
        }
    }

    private fun stopAlarm() {
        if (!isAlarmPlaying) return
        isAlarmPlaying = false

        try {
            alarmRingtone?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Cancel the alert notification
        val notificationManager = androidx.core.app.NotificationManagerCompat.from(this)
        notificationManager.cancel(ALARM_NOTIFICATION_ID)
    }

    override fun onDestroy() {
        unregisterReceiver(stopAlarmReceiver)
        stopMonitoring()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
