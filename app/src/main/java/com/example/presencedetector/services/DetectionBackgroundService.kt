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

    // App Usage Monitoring
    private var appUsageMonitor: AppUsageMonitor? = null
    private val appMonitorHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val appMonitorRunnable = object : Runnable {
        override fun run() {
            appUsageMonitor?.checkForegroundApp { packageName ->
                 triggerHiddenCamera(packageName)
            }
            appMonitorHandler.postDelayed(this, 2000) // Check every 2s
        }
    }

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
            checkSafeZone()
        }

        // Register receiver for camera presence events
        registerCameraReceiver()

        // Init App Monitor
        appUsageMonitor = AppUsageMonitor(this)
    }

    private fun triggerHiddenCamera(packageName: String) {
        Log.w(TAG, "Triggering Hidden Camera for sensitive app: $packageName")
        try {
            val intent = Intent(this, com.example.presencedetector.security.ui.HiddenCameraActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)

            NotificationUtil.sendPresenceNotification(
                this,
                "⚠️ Security Alert",
                "Sensitive App Accessed: $packageName. Photo captured.",
                true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch hidden camera", e)
        }
    }

    private var lastAutoArmSuggestionTime = 0L

    private fun checkSafeZone() {
        val trustedSsid = preferences.getTrustedWifiSsid()
        if (!trustedSsid.isNullOrEmpty()) {
             val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
             val info = wifiManager?.connectionInfo
             if (info != null && info.ssid != null) {
                 val currentSsid = info.ssid.replace("\"", "")
                 if (currentSsid == trustedSsid) {
                     // We are in safe zone. Disarm Anti-Theft if armed.
                     if (preferences.isAntiTheftArmed()) {
                         Log.i(TAG, "Safe Zone detected ($currentSsid). Disarming Anti-Theft.")
                         val stopIntent = Intent(this, AntiTheftService::class.java).apply {
                             action = AntiTheftService.ACTION_STOP
                         }
                         startService(stopIntent)

                         NotificationUtil.sendPresenceNotification(
                             this,
                             "🛡️ Zona Segura",
                             "Anti-Furto desativado: Conectado a $currentSsid",
                             false
                         )
                     }
                 } else {
                     // Not connected to trusted SSID
                     checkForAutoArmSuggestion()
                 }
             } else {
                 // No WiFi connection
                 checkForAutoArmSuggestion()
             }
        }
    }

    private fun checkForAutoArmSuggestion() {
        if (preferences.isAntiTheftArmed()) return

        // SMART MODE LOGIC: Auto-arm if enabled
        if (preferences.isSmartModeEnabled()) {
             Log.i(TAG, "Smart Mode Active: Outside Safe Zone -> Auto-Arming Anti-Theft.")
             val startIntent = Intent(this, AntiTheftService::class.java).apply {
                 action = AntiTheftService.ACTION_START
             }
             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                 startForegroundService(startIntent)
             } else {
                 startService(startIntent)
             }
             NotificationUtil.sendPresenceNotification(
                 this,
                 "🛡️ Modo Inteligente",
                 "Anti-Furto ativado automaticamente ao sair de casa.",
                 false
             )
             return
        }

        if (System.currentTimeMillis() - lastAutoArmSuggestionTime > 30 * 60 * 1000L) { // Suggest every 30m if still unarmed

            Log.i(TAG, "Outside Safe Zone and unarmed. Suggesting Anti-Theft.")

            val enableIntent = Intent(this, com.example.presencedetector.receivers.NotificationActionReceiver::class.java).apply {
                action = "com.example.presencedetector.ACTION_ENABLE_ANTITHEFT"
            }
            val pendingEnableIntent = android.app.PendingIntent.getBroadcast(
                this,
                2002,
                enableIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            NotificationUtil.sendPresenceNotification(
                this,
                "⚠️ Fora da Zona Segura",
                "Você saiu de casa. Deseja ativar o Anti-Furto?",
                false, // Info priority (user can ignore)
                "Ativar Agora",
                pendingEnableIntent
            )

            lastAutoArmSuggestionTime = System.currentTimeMillis()
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
                    "Monitoramento Residencial Ativo",
                    "Escaneando dispositivos...",
                    NotificationUtil.HOME_SECURITY_CHANNEL_ID
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }

                // Start detection
                detectionManager?.startDetection()

                // Start App Monitor
                appMonitorHandler.post(appMonitorRunnable)

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

        appMonitorHandler.removeCallbacks(appMonitorRunnable)

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
                    "$method • $details",
                    NotificationUtil.HOME_SECURITY_CHANNEL_ID
                )
            } else {
                NotificationUtil.createForegroundNotification(
                    this,
                    "✗ Nenhuma Presença",
                    "$method • Aguardando...",
                    NotificationUtil.HOME_SECURITY_CHANNEL_ID
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
