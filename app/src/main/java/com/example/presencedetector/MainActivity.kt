package com.example.presencedetector

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
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

    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var btnOpenRadarFromGrid: MaterialCardView
    private lateinit var btnSettings: MaterialCardView
    private lateinit var btnOpenHistory: MaterialCardView
    private lateinit var btnSecuritySettings: MaterialCardView
    private lateinit var btnAntiTheft: MaterialCardView
    private lateinit var statusIndicator: ImageView
    private lateinit var statusText: TextView
    private lateinit var statusDetails: TextView
    private lateinit var detectionLog: TextView
    private lateinit var logScrollView: ScrollView
    private lateinit var cbNotifyPresence: MaterialCheckBox
    
    private lateinit var tvCountKnown: TextView
    private lateinit var tvNamesKnown: TextView
    private lateinit var tvCountUnknown: TextView
    private lateinit var tvAntiTheftStatus: TextView
    private lateinit var ivAntiTheftIcon: ImageView

    private var detectionManager: PresenceDetectionManager? = null
    private var isDetecting = false
    private lateinit var preferences: PreferencesUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        preferences = PreferencesUtil(this)
        NotificationUtil.createNotificationChannels(this)

        initializeViews()
        if (!hasRequiredPermissions()) {
            requestPermissions()
        }
        setupDetectionManager()
        handleIntent(intent)
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
                    addLog("📸 Intruder Photo Saved: $filename")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    override fun onResume() {
        super.onResume()
        updateAntiTheftUI()
    }

    private fun initializeViews() {
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        btnOpenRadarFromGrid = findViewById(R.id.btnOpenRadarFromGrid)
        btnSettings = findViewById(R.id.btnSettings)
        btnOpenHistory = findViewById(R.id.btnOpenHistory)
        statusIndicator = findViewById(R.id.statusIndicator)
        statusText = findViewById(R.id.statusText)
        statusDetails = findViewById(R.id.statusDetails)
        detectionLog = findViewById(R.id.detectionLog)
        logScrollView = findViewById(R.id.logScrollView)
        cbNotifyPresence = findViewById(R.id.cbNotifyPresence)
        
        tvCountKnown = findViewById(R.id.tvCountKnown)
        tvNamesKnown = findViewById(R.id.tvNamesKnown)
        tvCountUnknown = findViewById(R.id.tvCountUnknown)

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

        startButton.setOnClickListener { startDetection() }
        stopButton.setOnClickListener { stopDetection() }

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
        Toast.makeText(this, "🆘 PANIC MODE ACTIVATED!", Toast.LENGTH_LONG).show()

        // 1. Send Telegram Alert
        val telegramService = com.example.presencedetector.services.TelegramService(this)
        telegramService.sendMessage("🆘 SOS ALERT! Panic button pressed on main dashboard!")

        // 2. Play Alarm Sound (Optional - strictly for panic)
        // For now, we just alert externally to avoid accidental deafening, or we could trigger the AntiTheft alarm.

        // 3. Log event
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        detectionLog.append("\n[$timestamp] 🆘 PANIC BUTTON PRESSED")
        preferences.logSystemEvent("🆘 PANIC BUTTON PRESSED")
        logScrollView.post { logScrollView.fullScroll(View.FOCUS_DOWN) }
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
        addLog("Mobile Security Disarmed")
        preferences.logSystemEvent("Mobile Security Disarmed")
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
        addLog("Mobile Security Armed")
        preferences.logSystemEvent("Mobile Security Armed")
        updateAntiTheftUI()
    }

    private fun updateAntiTheftUI() {
        val armed = preferences.isAntiTheftArmed()
        if (armed) {
            tvAntiTheftStatus.text = "Armed (Motion)"
            ivAntiTheftIcon.setImageResource(R.drawable.ic_status_active)
        } else {
            tvAntiTheftStatus.text = "Tap to Arm"
            ivAntiTheftIcon.setImageResource(android.R.drawable.ic_lock_idle_lock)
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
        
        val categories = knownDevices.groupBy { it.category }
            .map { "${it.key.iconRes} ${it.value.size}" }
            .joinToString(" ")
            
        val nicknames = knownDevices.joinToString(", ") { preferences.getNickname(it.bssid) ?: "" }
        tvNamesKnown.text = if (knownDevices.isEmpty()) "Nobody home" else "$categories | $nicknames"
        
        tvCountUnknown.text = unknownDevices.size.toString()

        statusDetails.text = "Monitoring via $method"
        
        if (isDetecting) {
            statusIndicator.setImageResource(R.drawable.ic_status_active)
            statusIndicator.setColorFilter(ContextCompat.getColor(this, R.color.success_color))
            statusText.text = "System Active"
        } else {
            statusIndicator.setImageResource(R.drawable.ic_status_inactive)
            statusIndicator.setColorFilter(ContextCompat.getColor(this, R.color.primary_color))
            statusText.text = "System Idle"
        }

        if (devices.isNotEmpty()) {
            val summary = devices.sortedByDescending { it.level }.take(5).joinToString(", ") { 
                val name = preferences.getNickname(it.bssid) ?: it.ssid.take(8)
                "${it.category.iconRes} $name (${it.level}dBm)" 
            }
            addLog("Scan: ${devices.size} found. Top: $summary")
        }
    }

    private fun startDetection() {
        if (!hasRequiredPermissions()) {
            requestPermissions()
            return
        }

        isDetecting = true
        startButton.visibility = View.GONE
        stopButton.visibility = View.VISIBLE
        statusIndicator.setColorFilter(ContextCompat.getColor(this, R.color.success_color))

        val serviceIntent = Intent(this, DetectionBackgroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        detectionManager?.startDetection()
        statusText.text = "Active"
    }

    private fun stopDetection() {
        isDetecting = false
        startButton.visibility = View.VISIBLE
        stopButton.visibility = View.GONE
        statusIndicator.setColorFilter(null)

        val serviceIntent = Intent(this, DetectionBackgroundService::class.java)
        stopService(serviceIntent)

        detectionManager?.stopDetection()
        statusText.text = "Idle"
        statusIndicator.setImageResource(R.drawable.ic_status_inactive)
    }

    private fun addLog(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        detectionLog.append("\n[$timestamp] $message")
        logScrollView.post { logScrollView.fullScroll(View.FOCUS_DOWN) }
    }

    private fun hasRequiredPermissions(): Boolean {
        val permissions = mutableListOf<String>()
        permissions.add(Manifest.permission.ACCESS_WIFI_STATE)
        permissions.add(Manifest.permission.CHANGE_WIFI_STATE)
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        permissions.add(Manifest.permission.CAMERA) // Added for Intruder Snap

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
                // We should prompt user, but for now just logging or showing a toast
                // In a full implementation, we would launch Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                Toast.makeText(this, "Please disable battery optimization for best performance", Toast.LENGTH_LONG).show()
                addLog("Warning: Battery optimization is enabled. Background scanning may stop.")
            }
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
                Toast.makeText(this, "Permissions required for WiFi scanning", Toast.LENGTH_LONG).show()
            }
        } else if (requestCode == 101) { // Background Location Request Code
             // Whether granted or not, try to start. Service will handle missing perm gracefully or run as foreground-only.
             if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                 addLog("Background Location Granted")
             } else {
                 addLog("Background Location Denied - Scanning may be limited")
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
