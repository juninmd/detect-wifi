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
            // Trigger biometric check via existing toggle logic
            if (preferences.isAntiTheftArmed()) {
                toggleAntiTheft()
            }
        }
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

        // Bluetooth permissions (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
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

        // Bluetooth permissions (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                startDetection()
            } else {
                Toast.makeText(this, "Permissions required for WiFi scanning", Toast.LENGTH_LONG).show()
            }
        }
    }
}
