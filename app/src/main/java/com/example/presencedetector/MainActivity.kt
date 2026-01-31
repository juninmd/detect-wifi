package com.example.presencedetector

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.presencedetector.model.WiFiDevice
import com.example.presencedetector.security.ui.SecuritySettingsActivity
import com.example.presencedetector.services.AntiTheftService
import com.example.presencedetector.services.DetectionBackgroundService
import com.example.presencedetector.services.PresenceDetectionManager
import com.example.presencedetector.utils.BiometricAuthenticator
import com.example.presencedetector.utils.NotificationUtil
import com.example.presencedetector.utils.PreferencesUtil
import com.google.android.material.card.MaterialCardView
import com.google.android.material.checkbox.MaterialCheckBox
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        const val EXTRA_DISARM_REQUEST = "EXTRA_DISARM_REQUEST"
        const val EXTRA_NOTIFICATION_ID = "EXTRA_NOTIFICATION_ID"
    }

    private lateinit var switchHomeMonitor: com.google.android.material.materialswitch.MaterialSwitch
    private lateinit var switchSmartMode: com.google.android.material.materialswitch.MaterialSwitch
    private lateinit var tvHomeStatusTitle: TextView
    private lateinit var btnOpenRadarFromGrid: android.view.View // Changed to View (LinearLayout in XML)
    private lateinit var btnSettings: MaterialCardView
    private lateinit var btnOpenHistory: MaterialCardView
    private lateinit var btnSecuritySettings: MaterialCardView
    private lateinit var btnAntiTheft: MaterialCardView

    // New Dashboard Views
    private lateinit var ivHomeStatus: ImageView
    private lateinit var tvHomeStatus: TextView
    private lateinit var ivSafeZoneBadge: com.google.android.material.card.MaterialCardView
    private lateinit var ivGlobalStatus: ImageView
    private lateinit var tvGlobalStatus: TextView
    private lateinit var tvGlobalDesc: TextView
    private lateinit var btnQuickScan: android.view.View // Changed to View (LinearLayout in XML)

    private lateinit var detectionLog: TextView
    private lateinit var cbNotifyPresence: MaterialCheckBox
    
    private lateinit var tvCountKnown: TextView
    private lateinit var tvNamesKnown: TextView
    private lateinit var tvCountUnknown: TextView
    private lateinit var tvAntiTheftStatus: TextView
    private lateinit var ivAntiTheftIcon: ImageView

    // Battery Monitor
    private lateinit var tvBatteryLevel: TextView
    private lateinit var tvBatteryTemp: TextView
    private lateinit var tvBatteryVoltage: TextView

    // App Lock
    private lateinit var lockOverlay: FrameLayout
    private lateinit var btnUnlockApp: Button

    private var detectionManager: PresenceDetectionManager? = null
    private var isDetecting = false
    private lateinit var preferences: PreferencesUtil

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let { updateBatteryInfo(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        preferences = PreferencesUtil(this)
        NotificationUtil.createNotificationChannels(this)

        initializeViews()
        checkAppLock()

        loadRecentLogs()
        if (!hasRequiredPermissions()) {
            requestPermissions()
        }
        setupDetectionManager()
        handleIntent(intent)
    }

    private fun checkAppLock() {
        if (preferences.isAppLockEnabled()) {
            lockOverlay.visibility = View.VISIBLE
            // Trigger auth
            BiometricAuthenticator(this).authenticate(
                onSuccess = {
                    lockOverlay.visibility = View.GONE
                    Toast.makeText(this, R.string.msg_unlocked, Toast.LENGTH_SHORT).show()
                    triggerHiddenCamera("App Unlock Success")
                },
                onFail = {
                    Toast.makeText(this, R.string.msg_auth_failed, Toast.LENGTH_SHORT).show()
                    triggerHiddenCamera("App Unlock Failed")
                }
            )
        } else {
            lockOverlay.visibility = View.GONE
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_DISARM_REQUEST, false) == true) {
            // Check if we should capture intruder selfie (silently)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                 captureIntruderSelfie()
            }

            // Trigger biometric check via existing toggle logic
            if (preferences.isAntiTheftArmed()) {
                toggleAntiTheft()
            }
        }
    }

    private fun captureIntruderSelfie() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return

        val textureView = findViewById<android.view.TextureView>(R.id.cameraPreview)
        if (textureView.isAvailable && textureView.surfaceTexture != null) {
            startCameraCapture(textureView.surfaceTexture!!)
        } else {
            textureView.surfaceTextureListener = object : android.view.TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
                    startCameraCapture(surface)
                }
                override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {}
                override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean = true
                override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {}
            }
        }
    }

    private fun startCameraCapture(surfaceTexture: android.graphics.SurfaceTexture) {
        try {
            val cameraManager = getSystemService(android.content.Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull {
                cameraManager.getCameraCharacteristics(it).get(android.hardware.camera2.CameraCharacteristics.LENS_FACING) == android.hardware.camera2.CameraCharacteristics.LENS_FACING_FRONT
            } ?: cameraManager.cameraIdList.firstOrNull()

            if (cameraId == null || ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return

            // Variables to hold references for cleanup
            var activeCamera: android.hardware.camera2.CameraDevice? = null
            var activeSession: android.hardware.camera2.CameraCaptureSession? = null

            // Prepare ImageReader for JPEG
            val imageReader = android.media.ImageReader.newInstance(640, 480, android.graphics.ImageFormat.JPEG, 1)

            val cleanup = {
                try {
                    activeSession?.close()
                    activeCamera?.close()
                    imageReader.close()
                } catch (e: Exception) { e.printStackTrace() }
            }

            imageReader.setOnImageAvailableListener({ reader ->
                try {
                    val image = reader.acquireLatestImage()
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    image.close()
                    saveIntruderImage(bytes)

                    // Close camera resources immediately after capturing
                    android.os.Handler(android.os.Looper.getMainLooper()).post(cleanup)
                } catch (e: Exception) {
                    e.printStackTrace()
                    cleanup()
                }
            }, android.os.Handler(android.os.Looper.getMainLooper()))

            cameraManager.openCamera(cameraId, object : android.hardware.camera2.CameraDevice.StateCallback() {
                override fun onOpened(camera: android.hardware.camera2.CameraDevice) {
                    activeCamera = camera
                    try {
                        val previewSurface = android.view.Surface(surfaceTexture)
                        val captureSurface = imageReader.surface

                        camera.createCaptureSession(listOf(previewSurface, captureSurface), object : android.hardware.camera2.CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: android.hardware.camera2.CameraCaptureSession) {
                                activeSession = session
                                try {
                                    // 1. Start Preview (needed for some devices to warm up)
                                    val previewRequest = camera.createCaptureRequest(android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW)
                                    previewRequest.addTarget(previewSurface)
                                    session.setRepeatingRequest(previewRequest.build(), null, null)

                                    // 2. Capture Still after short delay to allow auto-exposure/focus to settle
                                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                        try {
                                            val captureRequest = camera.createCaptureRequest(android.hardware.camera2.CameraDevice.TEMPLATE_STILL_CAPTURE)
                                            captureRequest.addTarget(captureSurface)
                                            // Set orientation if needed, but for front camera usually 270 or 90.
                                            // Leaving default for now.
                                            session.capture(captureRequest.build(), null, null)
                                            addLog("📸 Taking Intruder Snapshot...")
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            cleanup()
                                        }
                                    }, 500)

                                } catch (e: android.hardware.camera2.CameraAccessException) {
                                    e.printStackTrace()
                                    cleanup()
                                }
                            }

                            override fun onConfigureFailed(session: android.hardware.camera2.CameraCaptureSession) {
                                cleanup()
                            }
                        }, null)
                    } catch (e: Exception) {
                         cleanup()
                    }
                }

                override fun onDisconnected(camera: android.hardware.camera2.CameraDevice) {
                    activeCamera = camera // Ensure we have the reference
                    cleanup()
                }

                override fun onError(camera: android.hardware.camera2.CameraDevice, error: Int) {
                    activeCamera = camera
                    cleanup()
                }
            }, null)

        } catch (e: Exception) {
            android.util.Log.e("IntruderSnap", "Error accessing camera", e)
        }
    }

    private fun saveIntruderImage(bytes: ByteArray) {
        Thread {
            try {
                val filename = "INTRUDER_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".jpg"
                val file = java.io.File(getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), filename)
                java.io.FileOutputStream(file).use { it.write(bytes) }
                runOnUiThread {
                    addLog(getString(R.string.log_intruder_photo, filename))
                }

                // Send to Telegram
                val telegramService = com.example.presencedetector.services.TelegramService(this)
                telegramService.sendPhoto(file, "🚨 INTRUDER DETECTED! Photo captured.")

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    override fun onResume() {
        super.onResume()
        updateAntiTheftUI()
        updateGlobalStatus()
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(batteryReceiver)
    }

    private fun updateBatteryInfo(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryPct = level * 100 / scale.toFloat()

        val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
        val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)

        tvBatteryLevel.text = "${batteryPct.toInt()}%"
        tvBatteryVoltage.text = "${String.format("%.1f", voltage / 1000f)}V"
        tvBatteryTemp.text = "${String.format("%.1f", temp / 10f)}°C"
    }

    private fun initializeViews() {
        switchHomeMonitor = findViewById(R.id.switchHomeMonitor)
        switchSmartMode = findViewById(R.id.switchSmartMode)
        switchSmartMode.isChecked = preferences.isSmartModeEnabled()
        switchSmartMode.setOnCheckedChangeListener { _, isChecked ->
            preferences.setSmartModeEnabled(isChecked)
        }

        tvHomeStatusTitle = findViewById(R.id.tvHomeStatusTitle)
        btnOpenRadarFromGrid = findViewById(R.id.btnOpenRadarFromGrid)
        btnSettings = findViewById(R.id.btnSettings)
        btnOpenHistory = findViewById(R.id.btnOpenHistory)

        // New Dashboard
        ivHomeStatus = findViewById(R.id.ivHomeStatus)
        tvHomeStatus = findViewById(R.id.tvHomeStatus)
        ivSafeZoneBadge = findViewById(R.id.ivSafeZoneBadge)

        ivGlobalStatus = findViewById(R.id.ivGlobalStatus)
        tvGlobalStatus = findViewById(R.id.tvGlobalStatus)
        tvGlobalDesc = findViewById(R.id.tvGlobalDesc)

        // Make Status Card clickable to arm/disarm
        findViewById<View>(R.id.statusCard).setOnClickListener {
            if (preferences.isAntiTheftArmed()) {
                toggleAntiTheft()
            } else {
                // If not armed, check what to do.
                // Prioritize Anti-Theft arming as it's the main "Lock" action.
                toggleAntiTheft()
            }
        }

        detectionLog = findViewById(R.id.detectionLog)
        cbNotifyPresence = findViewById(R.id.cbNotifyPresence)
        
        tvCountKnown = findViewById(R.id.tvCountKnown)
        // tvNamesKnown = findViewById(R.id.tvNamesKnown) // Removed from XML
        tvCountUnknown = findViewById(R.id.tvCountUnknown)

        // Battery
        tvBatteryLevel = findViewById(R.id.tvBatteryLevel)
        tvBatteryTemp = findViewById(R.id.tvBatteryTemp)
        tvBatteryVoltage = findViewById(R.id.tvBatteryVoltage)

        // App Lock
        lockOverlay = findViewById(R.id.lockOverlay)
        btnUnlockApp = findViewById(R.id.btnUnlockApp)
        btnUnlockApp.setOnClickListener { checkAppLock() }

        // Anti-Theft
        btnAntiTheft = findViewById(R.id.btnAntiTheft)
        tvAntiTheftStatus = findViewById(R.id.tvAntiTheftStatus)
        ivAntiTheftIcon = findViewById(R.id.ivAntiTheftIcon)
        btnAntiTheft.setOnClickListener { toggleAntiTheft() }
        updateAntiTheftUI()

        cbNotifyPresence.isChecked = preferences.shouldNotifyOnPresence()
        cbNotifyPresence.setOnCheckedChangeListener { _, isChecked ->
            preferences.setNotifyOnPresence(isChecked)
        }

        switchHomeMonitor.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) startDetection() else stopDetection()
        }

        btnQuickScan = findViewById(R.id.btnQuickScan)
        btnQuickScan.setOnClickListener {
            Toast.makeText(this, "Scanning...", Toast.LENGTH_SHORT).show()
            startDetection()
        }

        btnOpenRadarFromGrid.setOnClickListener {
            startActivity(Intent(this, WifiRadarActivity::class.java))
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        btnOpenHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        // Security Monitoring (RTSP Cameras)
        btnSecuritySettings = findViewById(R.id.btnSecuritySettings)
        btnSecuritySettings.setOnClickListener {
            startActivity(Intent(this, com.example.presencedetector.security.ui.CameraDashboardActivity::class.java))
        }

        val btnPanic = findViewById<View>(R.id.btnPanic)
        btnPanic.setOnClickListener {
            triggerPanicMode()
        }
    }

    private fun triggerPanicMode() {
        Toast.makeText(this, R.string.msg_panic_activated, Toast.LENGTH_LONG).show()

        val serviceIntent = Intent(this, AntiTheftService::class.java).apply {
            action = AntiTheftService.ACTION_PANIC
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        addLog(getString(R.string.log_panic_pressed))
        updateGlobalStatus()
    }

    private fun toggleAntiTheft() {
        val currentlyArmed = preferences.isAntiTheftArmed()
        if (currentlyArmed) {
            if (preferences.isBiometricEnabled()) {
                BiometricAuthenticator(this).authenticate(
                    onSuccess = { performDisarm() }
                )
            } else {
                performDisarm()
            }
        } else {
            performArm()
        }
    }

    private fun performDisarm() {
        val serviceIntent = Intent(this, AntiTheftService::class.java).apply {
            action = AntiTheftService.ACTION_STOP
        }
        startService(serviceIntent)
        preferences.setAntiTheftArmed(false)
        addLog(getString(R.string.log_mobile_disarmed))
        preferences.logSystemEvent("Mobile Security Disarmed") // Keep system log consistent or localized? Let's localize for now
        updateAntiTheftUI()
    }

    private fun performArm() {
        val serviceIntent = Intent(this, AntiTheftService::class.java).apply {
            action = AntiTheftService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        preferences.setAntiTheftArmed(true)
        addLog(getString(R.string.log_mobile_armed))
        preferences.logSystemEvent("Mobile Security Armed")
        updateAntiTheftUI()
    }

    private fun updateAntiTheftUI() {
        val armed = preferences.isAntiTheftArmed()
        if (armed) {
            val modes = mutableListOf("Movimento")
            if (preferences.isPocketModeEnabled()) modes.add("Bolso")
            if (preferences.isChargerModeEnabled()) modes.add("Carregador")

            val modeString = modes.joinToString(", ")
            tvAntiTheftStatus.text = "Ativo: $modeString"
            ivAntiTheftIcon.setImageResource(R.drawable.ic_status_active)
            ivAntiTheftIcon.setColorFilter(ContextCompat.getColor(this, R.color.success_color))
        } else {
            tvAntiTheftStatus.text = getString(R.string.text_tap_to_arm)
            ivAntiTheftIcon.setImageResource(android.R.drawable.ic_lock_idle_lock)
            ivAntiTheftIcon.setColorFilter(ContextCompat.getColor(this, R.color.primary_color))
        }
        updateGlobalStatus()
    }

    private fun updateGlobalStatus() {
        val isArmed = preferences.isAntiTheftArmed()
        val statusCard = findViewById<MaterialCardView>(R.id.statusCard)

        // Determine Global State
        // Priority 1: Armed (Secure)
        // Priority 2: Scanning (Monitoring)
        // Default: Idle

        if (isArmed) {
            statusCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.success_color))
            ivGlobalStatus.setImageResource(R.drawable.ic_status_active)
            ivGlobalStatus.setColorFilter(ContextCompat.getColor(this, R.color.white))
            tvGlobalStatus.text = getString(R.string.status_system_secure)
            tvGlobalStatus.setTextColor(ContextCompat.getColor(this, R.color.white))
            tvGlobalDesc.text = getString(R.string.desc_system_secure)
            tvGlobalDesc.setTextColor(ContextCompat.getColor(this, R.color.white))
        } else if (isDetecting) {
            statusCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.primary_color))
            ivGlobalStatus.setImageResource(android.R.drawable.ic_menu_rotate) // or radar icon
            ivGlobalStatus.setColorFilter(ContextCompat.getColor(this, R.color.white))
            tvGlobalStatus.text = getString(R.string.status_system_monitoring)
            tvGlobalStatus.setTextColor(ContextCompat.getColor(this, R.color.white))
            tvGlobalDesc.text = getString(R.string.desc_system_monitoring)
            tvGlobalDesc.setTextColor(ContextCompat.getColor(this, R.color.white))
        } else {
            statusCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_background_elevated))
            ivGlobalStatus.setImageResource(android.R.drawable.ic_lock_idle_lock)
            ivGlobalStatus.setColorFilter(ContextCompat.getColor(this, R.color.light_text))
            tvGlobalStatus.text = getString(R.string.status_system_idle)
            tvGlobalStatus.setTextColor(ContextCompat.getColor(this, R.color.light_text))
            tvGlobalDesc.text = getString(R.string.desc_system_idle)
            tvGlobalDesc.setTextColor(ContextCompat.getColor(this, R.color.light_text))
        }
    }

    private fun loadRecentLogs() {
        val logs = preferences.getSystemLogs()
        if (logs.isNotEmpty()) {
            val recent = logs.take(5).joinToString("\n")
            detectionLog.text = "--- Recent Events ---\n$recent\n---------------------\n"
        }
    }

    private fun setupDetectionManager() {
        detectionManager = PresenceDetectionManager(this, false)
        detectionManager?.setPresenceListener { _, method, devices, details ->
            runOnUiThread {
                updateDashboard(devices, method, details)
            }
        }
    }

    private fun updateDashboard(devices: List<WiFiDevice>, method: String, details: String) {
        val knownDevices = devices.filter { preferences.getNickname(it.bssid) != null }
        val unknownDevices = devices.filter { preferences.getNickname(it.bssid) == null }
        
        tvCountKnown.text = knownDevices.size.toString()
        // tvNamesKnown.text = ... Removed
        
        tvCountUnknown.text = unknownDevices.size.toString()

        // Update Home Status (WiFi)
        if (isDetecting) {
            ivHomeStatus.setImageResource(R.drawable.ic_status_active)
            ivHomeStatus.setColorFilter(ContextCompat.getColor(this, R.color.success_color))
            tvHomeStatus.text = getString(R.string.status_scanning)
            tvHomeStatus.setTextColor(ContextCompat.getColor(this, R.color.success_color))
            tvHomeStatusTitle.text = "Monitoramento Ativo"
            if (!switchHomeMonitor.isChecked) switchHomeMonitor.isChecked = true
        } else {
            ivHomeStatus.setImageResource(R.drawable.ic_status_inactive)
            ivHomeStatus.setColorFilter(ContextCompat.getColor(this, R.color.primary_color))
            tvHomeStatus.text = getString(R.string.status_home_idle)
            tvHomeStatus.setTextColor(ContextCompat.getColor(this, R.color.light_text))
            tvHomeStatusTitle.text = "Monitoramento Parado"
            if (switchHomeMonitor.isChecked) switchHomeMonitor.isChecked = false
        }

        // Safe Zone Check
        val trustedSsid = preferences.getTrustedWifiSsid()
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
        val info = wifiManager?.connectionInfo
        val currentSsid = info?.ssid?.replace("\"", "")

        if (!trustedSsid.isNullOrEmpty() && currentSsid == trustedSsid) {
            ivSafeZoneBadge.visibility = View.VISIBLE
        } else {
            ivSafeZoneBadge.visibility = View.GONE
        }

        // Update Mobile Status (Anti-Theft) - Also called in updateAntiTheftUI, but good to refresh here
        updateAntiTheftUI()
        updateGlobalStatus()

        if (devices.isNotEmpty()) {
            val summary = devices.sortedByDescending { it.level }.take(5).joinToString(", ") { 
                val name = preferences.getNickname(it.bssid) ?: it.ssid.take(8)
                "${it.category.iconRes} $name (${it.level}dBm)" 
            }
            addLog(getString(R.string.log_scan_summary, devices.size, summary))
        }
    }

    private fun startDetection() {
        if (!hasRequiredPermissions()) {
            requestPermissions()
            return
        }

        isDetecting = true
        if (!switchHomeMonitor.isChecked) switchHomeMonitor.isChecked = true

        // UI Update
        ivHomeStatus.setColorFilter(ContextCompat.getColor(this, R.color.success_color))
        tvHomeStatus.text = getString(R.string.status_scanning)
        tvHomeStatus.setTextColor(ContextCompat.getColor(this, R.color.success_color))

        val serviceIntent = Intent(this, DetectionBackgroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        detectionManager?.startDetection()
    }

    private fun stopDetection() {
        isDetecting = false
        if (switchHomeMonitor.isChecked) switchHomeMonitor.isChecked = false

        // UI Update
        ivHomeStatus.setColorFilter(ContextCompat.getColor(this, R.color.primary_color))
        ivHomeStatus.setImageResource(R.drawable.ic_status_inactive)
        tvHomeStatus.text = getString(R.string.status_home_idle)
        tvHomeStatus.setTextColor(ContextCompat.getColor(this, R.color.light_text))

        val serviceIntent = Intent(this, DetectionBackgroundService::class.java)
        stopService(serviceIntent)

        detectionManager?.stopDetection()
    }

    private fun addLog(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val current = detectionLog.text.toString()
        val newText = "[$timestamp] $message\n$current"
        detectionLog.text = newText.take(1000)
    }

    private fun hasRequiredPermissions(): Boolean {
        val permissions = mutableListOf<String>()
        permissions.add(Manifest.permission.ACCESS_WIFI_STATE)
        permissions.add(Manifest.permission.CHANGE_WIFI_STATE)
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        permissions.add(Manifest.permission.CAMERA) // Added for Intruder Snap
        permissions.add(Manifest.permission.RECORD_AUDIO) // Added for Audio Evidence
        permissions.add(Manifest.permission.READ_PHONE_STATE) // Added for SIM Protection

        // Bluetooth permissions (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        // Background location for Android 10+ (Q)
        // Note: On Android 11+ (R), this must be requested separately or it may be ignored if requested with foreground.
        // For simplicity in this optimization phase, we check it here, but strict flow might require separation.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        permissions.add(Manifest.permission.ACCESS_WIFI_STATE)
        permissions.add(Manifest.permission.CHANGE_WIFI_STATE)
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        permissions.add(Manifest.permission.CAMERA) // Added for Intruder Snap
        permissions.add(Manifest.permission.RECORD_AUDIO) // Added for Audio Evidence
        permissions.add(Manifest.permission.READ_PHONE_STATE) // Added for SIM Protection

        // Bluetooth permissions (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        // On Android 11+ (R), Background Location must be requested SEPARATELY after foreground is granted.
        // If we request it here with others, the system ignores it.
        // So we do NOT add it here for R+. We add it only for Q.
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQUEST_CODE)

        checkBatteryOptimization()
    }

    private fun checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val packageName = packageName
            val pm = getSystemService(android.os.PowerManager::class.java)
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                addLog(getString(R.string.log_battery_opt_warning))

                com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.msg_battery_dialog_title)
                    .setMessage(R.string.msg_battery_dialog_text)
                    .setPositiveButton(R.string.btn_open_settings) { _, _ ->
                        try {
                            val intent = Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(this, "Could not open settings", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show()
            }
        }

        // Check Usage Stats
        checkUsageStatsPermission()
    }

    private fun checkUsageStatsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val appOps = getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
            val mode = appOps.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
            if (mode != android.app.AppOpsManager.MODE_ALLOWED) {
                com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle("Permissão de Monitoramento")
                    .setMessage("Para detectar apps de banco e proteger seu celular, precisamos acessar os dados de uso.")
                    .setPositiveButton("Conceder") { _, _ ->
                        try {
                            startActivity(Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    .show()
            }
        }
    }

    private fun triggerHiddenCamera(reason: String) {
        try {
            val intent = Intent(this, com.example.presencedetector.security.ui.HiddenCameraActivity::class.java)
            startActivity(intent)
            addLog("📸 Camera Triggered: $reason")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Check if all requested permissions were granted
            val allGranted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }

            if (allGranted) {
                // On Android 11+ (R), we must request Background Location separately
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    checkAndRequestBackgroundLocation()
                } else {
                    startDetection()
                }
            } else {
                Toast.makeText(this, R.string.msg_permissions_required, Toast.LENGTH_LONG).show()
            }
        } else if (requestCode == 101) { // Background Location Request Code
             // Whether granted or not, try to start. Service will handle missing perm gracefully or run as foreground-only.
             if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                 addLog(getString(R.string.msg_bg_location_granted))
             } else {
                 addLog(getString(R.string.msg_bg_location_denied))
             }
             startDetection()
        }
    }

    private fun checkAndRequestBackgroundLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // We should show a dialog explaining why we need this before requesting
            // For now, request directly to keep flow moving
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                101
            )
        } else {
            startDetection()
        }
    }
}
