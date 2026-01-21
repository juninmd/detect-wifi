package com.example.presencedetector.services

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.presencedetector.security.service.CameraMonitoringService
import com.example.presencedetector.utils.NotificationUtil
import com.example.presencedetector.utils.PreferencesUtil

/**
 * Background service for continuous presence detection.
 * Runs even when the app is not in the foreground.
 * Ensures service keeps running via START_STICKY even when device is locked.
 */
class DetectionBackgroundService : Service() {
    companion object {
        private const val TAG = "DetectionBgService"
        private const val NOTIFICATION_ID = 1
    }

    private val binder = LocalBinder()
    private var detectionManager: PresenceDetectionManager? = null
    private var isRunning = false
    private var cameraPresenceReceiver: BroadcastReceiver? = null
    private lateinit var preferences: PreferencesUtil

    inner class LocalBinder : Binder() {
        fun getService(): DetectionBackgroundService = this@DetectionBackgroundService
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "🚀 Background service created")

        preferences = PreferencesUtil(this)
        detectionManager = PresenceDetectionManager(this, true)
        detectionManager?.setPresenceListener { peoplePresent, method, devices, details ->
            updateForegroundNotification(peoplePresent, method, details)
            checkSafeZone(devices)
        }

        // Register receiver for camera presence events
        registerCameraReceiver()
    }

    private fun checkSafeZone(devices: List<com.example.presencedetector.model.WiFiDevice>) {
        val trustedSsid = preferences.getTrustedWifiSsid()
        if (!trustedSsid.isNullOrEmpty()) {
             // Check if we are connected to trusted SSID.
             // PresenceDetectionManager gives us list of *detected* devices around us.
             // We need to check if we are *connected* to one.
             // However, WiFiDevice list comes from WiFiDetectionService scan results.
             // Scan results show SSIDs around. It doesn't mean we are connected.
             // But if we are connected, it should be in the list with high signal.

             // A better way is to check connectivity manager or WifiManager for connection info.
             val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
             val info = wifiManager?.connectionInfo
             if (info != null && info.ssid != null) {
                 // SSID usually comes with quotes, e.g. "MyWiFi"
                 val currentSsid = info.ssid.replace("\"", "")
                 if (currentSsid == trustedSsid) {
                     // We are in safe zone. Disarm Anti-Theft if armed.
                     if (preferences.isAntiTheftArmed()) {
                         Log.i(TAG, "Safe Zone detected ($currentSsid). Disarming Anti-Theft.")
                         val stopIntent = Intent(this, AntiTheftService::class.java).apply {
                             action = AntiTheftService.ACTION_STOP
                         }
                         startService(stopIntent)

                         // Optionally notify user
                         NotificationUtil.sendPresenceNotification(
                             this,
                             "🛡️ Safe Zone",
                             "Anti-Theft disarmed automatically: Connected to $currentSsid",
                             false
                         )
                     }
                 }
             }
        }
    }

    private fun registerCameraReceiver() {
        cameraPresenceReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == CameraMonitoringService.ACTION_CAMERA_PRESENCE) {
                    val cameraName = intent.getStringExtra(CameraMonitoringService.EXTRA_CAMERA_NAME) ?: "Unknown Camera"
                    Log.i(TAG, "Received camera presence broadcast from $cameraName")
                    detectionManager?.handleExternalPresence("Camera", cameraName)
                }
            }
        }

        val filter = IntentFilter(CameraMonitoringService.ACTION_CAMERA_PRESENCE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(cameraPresenceReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(cameraPresenceReceiver, filter)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "🚀 Background service started (flags=$flags, startId=$startId)")

        if (!isRunning) {
            // Check permissions before starting
            if (!hasRequiredPermissions()) {
                Log.e(TAG, "❌ Missing required permissions for background service")
                stopSelf()
                return START_NOT_STICKY
            }

            isRunning = true

            try {
                // Create foreground notification to keep service alive
                val notification = NotificationUtil.createForegroundNotification(
                    this,
                    "🔍 Presence Detector Active",
                    "Scanning for devices..."
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }

                // Start detection
                detectionManager?.startDetection()
                Log.i(TAG, "✅ Presence detection started in background")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to start foreground service", e)
                isRunning = false
                stopSelf()
                return START_NOT_STICKY
            }
        }

        // START_STICKY: Restarts service if killed by system
        return START_STICKY
    }

    private fun hasRequiredPermissions(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
        return fineLocation == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "⚠️ Background service destroyed - will restart")

        isRunning = false
        detectionManager?.stopDetection()
        detectionManager?.destroy()

        if (cameraPresenceReceiver != null) {
            unregisterReceiver(cameraPresenceReceiver)
            cameraPresenceReceiver = null
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.i(TAG, "📱 Task removed - restarting service")
        // Try to restart when user swipes app away
        val restartService = Intent(applicationContext, DetectionBackgroundService::class.java)
        startService(restartService)
    }

    private fun updateForegroundNotification(peoplePresent: Boolean, method: String, details: String) {
        try {
            val notification = if (peoplePresent) {
                NotificationUtil.createForegroundNotification(
                    this,
                    "✓ Presença Detectada",
                    "$method • $details"
                )
            } else {
                NotificationUtil.createForegroundNotification(
                    this,
                    "✗ Nenhuma Presença",
                    "$method • Aguardando..."
                )
            }

            val notificationManager = getSystemService(android.app.NotificationManager::class.java)
            notificationManager?.notify(NOTIFICATION_ID, notification)

        } catch (e: Exception) {
            Log.e(TAG, "Error updating foreground notification", e)
        }
    }

    fun getDetectionStatus(): String = detectionManager?.getDetectionStatus() ?: "N/A"
}
