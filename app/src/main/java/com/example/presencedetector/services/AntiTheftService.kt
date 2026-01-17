package com.example.presencedetector.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
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
        const val ACTION_START_CHARGER_MODE = "com.example.presencedetector.action.START_CHARGER_ANTITHEFT"
        const val ACTION_STOP_CHARGER_MODE = "com.example.presencedetector.action.STOP_CHARGER_ANTITHEFT"
        const val ACTION_START_POCKET_MODE = "com.example.presencedetector.action.START_POCKET_ANTITHEFT"
        const val ACTION_STOP_POCKET_MODE = "com.example.presencedetector.action.STOP_POCKET_ANTITHEFT"

        private const val NOTIFICATION_ID = 999
        private const val ALARM_NOTIFICATION_ID = 1000
        private const val MOVEMENT_THRESHOLD = 1.5f // m/s^2 delta
        private const val GRACE_PERIOD_MS = 5000L // Time to put phone down after arming
    }

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var proximitySensor: Sensor? = null

    // States
    private var isMotionArmed = false
    private var isChargerArmed = false
    private var isPocketArmed = false
    private var isPocketSecured = false // True if sensor was covered after arming
    private var isAlarmPlaying = false

    // Sensor data
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var firstReading = true
    private var armingTime = 0L

    private var alarmRingtone: Ringtone? = null

    // Receivers
    private val stopAlarmReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == NotificationActionReceiver.ACTION_STOP_ALARM) {
                stopAlarm()
            }
        }
    }

    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_POWER_DISCONNECTED) {
                if (isChargerArmed) {
                    Log.w(TAG, "Charger disconnected while armed! Triggering alarm.")
                    triggerAlarm("Charger Disconnected!")
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        // Register receiver for stop command
        val filter = IntentFilter(NotificationActionReceiver.ACTION_STOP_ALARM)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stopAlarmReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(stopAlarmReceiver, filter)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startMotionMonitoring()
            ACTION_STOP -> stopMotionMonitoring()
            ACTION_START_CHARGER_MODE -> startChargerMonitoring()
            ACTION_STOP_CHARGER_MODE -> stopChargerMonitoring()
            ACTION_START_POCKET_MODE -> startPocketMonitoring()
            ACTION_STOP_POCKET_MODE -> stopPocketMonitoring()
        }
        return START_STICKY
    }

    private fun startPocketMonitoring() {
        if (isPocketArmed) return
        Log.d(TAG, "Pocket Anti-Theft Armed")
        isPocketArmed = true
        isPocketSecured = false // Reset secured state

        updateForegroundNotification()

        proximitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun stopPocketMonitoring() {
        if (!isPocketArmed) return
        Log.d(TAG, "Pocket Anti-Theft Disarmed")
        isPocketArmed = false
        // Only unregister if motion is also not using sensors (but here we register separate sensors ideally, or share listener)
        // Since we implement SensorEventListener on the Service, we can just unregister the specific sensor if we tracked it,
        // but unregisterListener(this, sensor) is safer.
        proximitySensor?.let {
            sensorManager.unregisterListener(this, it)
        }

        if (isAlarmPlaying) stopAlarm()
        checkIfServiceShouldStop()
    }

    private fun startMotionMonitoring() {
        if (isMotionArmed) return

        Log.d(TAG, "Motion Anti-Theft Armed")
        isMotionArmed = true
        firstReading = true
        armingTime = System.currentTimeMillis()

        updateForegroundNotification()

        // Register Sensor
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun stopMotionMonitoring() {
        if (!isMotionArmed) return
        Log.d(TAG, "Motion Anti-Theft Disarmed")
        isMotionArmed = false
        sensorManager.unregisterListener(this)

        // If alarm was triggered by motion (implied as no specific reason tracking), stop it for good UX
        if (isAlarmPlaying) stopAlarm()

        checkIfServiceShouldStop()
    }

    private fun startChargerMonitoring() {
        if (isChargerArmed) return
        Log.d(TAG, "Charger Anti-Theft Armed")
        isChargerArmed = true

        // Register Power Receiver
        val filter = IntentFilter(Intent.ACTION_POWER_DISCONNECTED)
        registerReceiver(powerReceiver, filter)

        updateForegroundNotification()
    }

    private fun stopChargerMonitoring() {
        if (!isChargerArmed) return
        Log.d(TAG, "Charger Anti-Theft Disarmed")
        isChargerArmed = false
        try {
            unregisterReceiver(powerReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering power receiver", e)
        }

        // If alarm was triggered by charger (implied), stop it for good UX
        if (isAlarmPlaying) stopAlarm()

        checkIfServiceShouldStop()
    }

    private fun checkIfServiceShouldStop() {
        if (!isMotionArmed && !isChargerArmed && !isPocketArmed) {
            stopAlarm()
            stopForeground(true)
            stopSelf()
        } else {
            updateForegroundNotification()
        }
    }

    private fun updateForegroundNotification() {
        NotificationUtil.createNotificationChannels(this)

        val appIntent = Intent(this, MainActivity::class.java)
        val pendingAppIntent = PendingIntent.getActivity(this, 0, appIntent, PendingIntent.FLAG_IMMUTABLE)

        val activeServices = mutableListOf<String>()
        if (isMotionArmed) activeServices.add("Motion")
        if (isChargerArmed) activeServices.add("Charger")
        if (isPocketArmed) activeServices.add("Pocket")

        val statusText = if (activeServices.isNotEmpty()) {
            "${activeServices.joinToString(", ")} Protection Active"
        } else {
            "Security Service Running"
        }

        val builder = NotificationCompat.Builder(this, "presence_detection_channel")
            .setContentTitle("🛡️ Mobile Security Active")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_status_active)
            .setOngoing(true)
            .setContentIntent(pendingAppIntent)

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, builder.build())
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER && isMotionArmed) {
            handleMotionSensor(event)
        } else if (event.sensor.type == Sensor.TYPE_PROXIMITY && isPocketArmed) {
            handlePocketSensor(event)
        }
    }

    private fun handlePocketSensor(event: SensorEvent) {
        val distance = event.values[0]
        val maxRange = event.sensor.maximumRange

        // If something is close (e.g. inside pocket)
        if (distance < maxRange) {
            if (!isPocketSecured) {
                isPocketSecured = true
                Log.d(TAG, "Pocket Mode: SECURED (Covered)")
            }
        } else {
            // If sensor is uncovered
            if (isPocketSecured) {
                Log.w(TAG, "Pocket Mode: UNCOVERED! Triggering Alarm.")
                triggerAlarm("Removed from Pocket!")
            }
        }
    }

    private fun handleMotionSensor(event: SensorEvent) {
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
                triggerAlarm("Motion Detected!")
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

    private fun triggerAlarm(reason: String) {
        if (isAlarmPlaying) return
        isAlarmPlaying = true

        Log.w(TAG, "TRIGGERING ALARM: $reason")

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
        showAlarmNotification(reason)
    }

    private fun showAlarmNotification(reason: String) {
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
            .setContentText(reason)
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
        try {
            unregisterReceiver(stopAlarmReceiver)
        } catch (e: Exception) {}

        if (isChargerArmed) {
            try {
                unregisterReceiver(powerReceiver)
            } catch (e: Exception) {}
        }

        stopMotionMonitoring()
        stopPocketMonitoring()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
