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
import com.example.presencedetector.utils.MotionDetector
import com.example.presencedetector.utils.NotificationUtil
import com.example.presencedetector.utils.PreferencesUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.SharedPreferences

class AntiTheftService : Service(), SensorEventListener, SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        private const val TAG = "AntiTheftService"
        const val ACTION_START = "com.example.presencedetector.action.START_ANTITHEFT"
        const val ACTION_STOP = "com.example.presencedetector.action.STOP_ANTITHEFT"
        private const val NOTIFICATION_ID = 999
        private const val ALARM_NOTIFICATION_ID = 1000
        private const val GRACE_PERIOD_MS = 5000L // Time to put phone down after arming
    }

    private lateinit var sensorManager: SensorManager
    private lateinit var preferences: PreferencesUtil
    private lateinit var telegramService: TelegramService
    private var motionDetector: MotionDetector? = null
    private var accelerometer: Sensor? = null
    private var proximitySensor: Sensor? = null

    private var isArmed = false
    private var isAlarmPlaying = false

    // Motion state
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var firstReading = true

    // Pocket Mode state
    private var isPocketModeArmed = false
    private var isDeviceInPocket = false

    // Charger Mode state
    private var isChargerModeArmed = false

    private var armingTime = 0L

    // Battery Sentinel State
    private var lowBatteryNotified = false

    private var alarmRingtone: Ringtone? = null

    private val reportHandler = Handler(Looper.getMainLooper())
    private val reportRunnable = object : Runnable {
        override fun run() {
            if (isAlarmPlaying) {
                val battery = getBatteryLevel()
                telegramService.sendMessage("🔋 TRACKING: Battery at $battery%. Alarm still active.")
                reportHandler.postDelayed(this, 120000) // 2 minutes
            }
        }
    }

    private val stopAlarmReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == NotificationActionReceiver.ACTION_STOP_ALARM) {
                stopAlarm()
            }
        }
    }

    private val batteryLevelReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val level = intent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val status = intent?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == android.os.BatteryManager.BATTERY_STATUS_FULL

            if (level != -1 && level <= 15 && !isCharging && isArmed && !lowBatteryNotified) {
                Log.w(TAG, "Critical Battery: $level%")
                context?.let { NotificationUtil.sendBatteryAlert(it, level) }
                telegramService.sendMessage("⚠️ LOW BATTERY WARNING: Security Device at $level%! Connect charger immediately.")
                lowBatteryNotified = true
            } else if (isCharging || level > 15) {
                lowBatteryNotified = false // Reset if charged
            }
        }
    }

    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_POWER_DISCONNECTED && isChargerModeArmed) {
                Log.w(TAG, "Charger disconnected! Triggering alarm.")
                triggerAlarm("Charger Disconnected")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        preferences = PreferencesUtil(this)
        telegramService = TelegramService(this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        // Register receiver for stop command
        val stopFilter = IntentFilter(NotificationActionReceiver.ACTION_STOP_ALARM)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stopAlarmReceiver, stopFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(stopAlarmReceiver, stopFilter)
        }

        // Register power receiver
        val powerFilter = IntentFilter(Intent.ACTION_POWER_DISCONNECTED)
        registerReceiver(powerReceiver, powerFilter)

        preferences.registerListener(this)
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

        val sensitivity = preferences.getAntiTheftSensitivity()
        motionDetector = MotionDetector(sensitivity)

        isPocketModeArmed = preferences.isPocketModeEnabled()
        isChargerModeArmed = preferences.isChargerModeEnabled()

        Log.d(TAG, "Anti-Theft Armed. Motion: ON, Pocket: $isPocketModeArmed, Charger: $isChargerModeArmed")

        preferences.setAntiTheftArmed(true) // Persist state
        isArmed = true
        firstReading = true
        isDeviceInPocket = false
        armingTime = System.currentTimeMillis()

        // Start Foreground
        try {
            startForeground(NOTIFICATION_ID, createForegroundNotification())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service", e)
            stopSelf()
            return
        }

        // Register Battery Sentinel
        val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryLevelReceiver, batteryFilter)

        // Register Sensors
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        if (isPocketModeArmed) {
            proximitySensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
    }

    private fun stopMonitoring() {
        Log.d(TAG, "Anti-Theft Disarmed")
        isArmed = false
        isPocketModeArmed = false
        isChargerModeArmed = false
        preferences.setAntiTheftArmed(false) // Persist state

        try {
            unregisterReceiver(batteryLevelReceiver)
        } catch (e: Exception) {
            // Ignore
        }

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

        val builder = NotificationCompat.Builder(this, NotificationUtil.CHANNEL_ID)
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

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            handleMotion(event)
        } else if (event.sensor.type == Sensor.TYPE_PROXIMITY && isPocketModeArmed) {
            handlePocketMode(event)
        }
    }

    private fun handleMotion(event: SensorEvent) {
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
            if (motionDetector?.isMotionDetected(x, y, z, lastX, lastY, lastZ) == true) {
                Log.w(TAG, "Motion Detected!")
                triggerAlarm("Motion Detected")
            }
        } else {
            firstReading = false
        }

        lastX = x
        lastY = y
        lastZ = z
    }

    private fun handlePocketMode(event: SensorEvent) {
        val distance = event.values[0]
        val maxRange = event.sensor.maximumRange

        // Check if object is close (in pocket)
        // Usually < 5cm or < maxRange
        val isClose = distance < maxRange && distance < 5.0f

        if (isClose) {
            isDeviceInPocket = true
            Log.d(TAG, "Pocket Mode: Device covered (Safe)")
        }

        // Trigger if:
        // 1. Grace period passed
        // 2. Device WAS in pocket (or we assume it was if grace period passed? No, better to track state)
        // 3. Now it is NOT close (removed)

        if (System.currentTimeMillis() - armingTime > GRACE_PERIOD_MS) {
            if (!isClose) {
                 // If we require that it WAS in pocket first, check isDeviceInPocket.
                 // Otherwise, if you arm it outside pocket, it triggers after 5s.
                 // Let's assume standard behavior: Trigger if uncovered.
                 Log.w(TAG, "Pocket Mode: Device uncovered! Triggering.")
                 triggerAlarm("Pocket Mode Triggered")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }

    private fun triggerAlarm(reason: String) {
        if (isAlarmPlaying) return
        isAlarmPlaying = true

        Log.w(TAG, "TRIGGERING ALARM: $reason")
        preferences.logSystemEvent("🚨 Alarm Triggered: $reason")

        // 1. Send Telegram Alert
        val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        telegramService.sendMessage("🚨 ANTI-THEFT ALARM: $reason at $time!")

        // Start Reporting
        reportHandler.post(reportRunnable)

        // 2. Play Sound
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

        // 3. Show Alert Notification with Action to Stop
        showAlarmNotification(reason)
    }

    private fun showAlarmNotification(reason: String) {
        // Action 1: Disarm (Secure - opens App)
        val disarmIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_DISARM_REQUEST, true)
            putExtra(MainActivity.EXTRA_NOTIFICATION_ID, ALARM_NOTIFICATION_ID)
        }
        val pendingDisarmIntent = PendingIntent.getActivity(
            this,
            ALARM_NOTIFICATION_ID,
            disarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: Call Emergency (Dialer)
        val emergencyIntent = Intent(Intent.ACTION_DIAL).apply {
            data = android.net.Uri.parse("tel:190") // 190 (Brazil) / 112 (EU) - Standard Emergency
        }
        val pendingEmergencyIntent = PendingIntent.getActivity(
            this,
            ALARM_NOTIFICATION_ID + 1,
            emergencyIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Select icon based on reason
        val icon = when {
            reason.contains("Charger", true) -> android.R.drawable.ic_lock_power_off
            reason.contains("Pocket", true) -> android.R.drawable.ic_menu_view
            else -> R.drawable.ic_notification_alert
        }

        val notification = NotificationCompat.Builder(this, NotificationUtil.ALERT_CHANNEL_ID)
            .setContentTitle("🚨 THEFT ALERT!")
            .setContentText("$reason! Tap to Disarm.")
            .setSmallIcon(icon)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(pendingDisarmIntent, true) // Try to wake screen and show
            .addAction(R.drawable.ic_status_inactive, "UNLOCK & DISARM", pendingDisarmIntent)
            .addAction(android.R.drawable.ic_menu_call, "EMERGENCY CALL", pendingEmergencyIntent)
            .setDeleteIntent(pendingDisarmIntent) // If dismissed, try to open app to ensure user sees it? Or just let it be.
            .setAutoCancel(false)
            .setOngoing(true) // Cannot be swiped away easily while alarming
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
        reportHandler.removeCallbacks(reportRunnable)

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
        if (::preferences.isInitialized) {
            preferences.unregisterListener(this)
        }
        try {
            unregisterReceiver(stopAlarmReceiver)
            unregisterReceiver(powerReceiver)
        } catch (e: Exception) {
            // Ignore if not registered
        }
        stopMonitoring()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (isArmed) {
            Log.d(TAG, "Task removed, restarting service to maintain protection")
            val restartServiceIntent = Intent(applicationContext, AntiTheftService::class.java).apply {
                action = ACTION_START
            }
            val restartServicePendingIntent = PendingIntent.getService(
                applicationContext, 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmService = applicationContext.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            alarmService.set(
                android.app.AlarmManager.ELAPSED_REALTIME,
                android.os.SystemClock.elapsedRealtime() + 1000,
                restartServicePendingIntent
            )
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == PreferencesUtil.KEY_ANTI_THEFT_SENSITIVITY) {
            val sensitivity = preferences.getAntiTheftSensitivity()
            Log.d(TAG, "Sensitivity updated to: $sensitivity")
            motionDetector = MotionDetector(sensitivity)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun getBatteryLevel(): Int {
        val bm = getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
        return bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }
}
