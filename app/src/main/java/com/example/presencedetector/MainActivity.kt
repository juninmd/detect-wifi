package com.example.presencedetector

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.presencedetector.services.DetectionBackgroundService
import com.example.presencedetector.services.PresenceDetectionManager
import com.example.presencedetector.utils.NotificationUtil
import com.example.presencedetector.utils.PreferencesUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Main activity for the Presence Detection application.
 * Manages the UI and controls the detection services.
 */
class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_REQUEST_CODE = 100
    }

    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var statusIndicator: ImageView
    private lateinit var statusText: TextView
    private lateinit var detectionLog: TextView
    private lateinit var logScrollView: ScrollView

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
        handleNotificationIntent()
    }

    private fun initializeViews() {
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        statusIndicator = findViewById(R.id.statusIndicator)
        statusText = findViewById(R.id.statusText)
        detectionLog = findViewById(R.id.detectionLog)
        logScrollView = findViewById(R.id.logScrollView)

        startButton.setOnClickListener { startDetection() }
        stopButton.setOnClickListener { stopDetection() }
        stopButton.isEnabled = false
    }

    private fun setupDetectionManager() {
        detectionManager = PresenceDetectionManager(this)
        detectionManager?.setPresenceListener { peoplePresent, method, details ->
            updatePresenceUI(peoplePresent, method, details)
        }
    }

    private fun handleNotificationIntent() {
        val presenceDetected = intent.getBooleanExtra("presence_detected", false)
        val message = intent.getStringExtra("notification_message")

        if (!message.isNullOrEmpty()) {
            addLog("Notification received: $message")
        }
    }

    private fun startDetection() {
        if (isDetecting) {
            Toast.makeText(this, "Detection already running", Toast.LENGTH_SHORT).show()
            return
        }

        if (!hasRequiredPermissions()) {
            requestPermissions()
            return
        }

        isDetecting = true
        startButton.isEnabled = false
        stopButton.isEnabled = true

        // Iniciar serviço de background
        val serviceIntent = Intent(this, DetectionBackgroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        detectionManager?.startDetection()
        addLog("Detection started (Background Service Active)...")
        statusText.text = "Status: Detecting..."
        preferences.setDetectionEnabled(true)
        preferences.setForegroundServiceRunning(true)
    }

    private fun stopDetection() {
        if (!isDetecting) {
            return
        }

        isDetecting = false
        startButton.isEnabled = true
        stopButton.isEnabled = false

        detectionManager?.stopDetection()

        // Parar serviço de background
        val serviceIntent = Intent(this, DetectionBackgroundService::class.java)
        stopService(serviceIntent)

        addLog("Detection stopped")
        statusText.text = "Status: Stopped"
        statusIndicator.setImageResource(R.drawable.ic_status_inactive)
        preferences.setDetectionEnabled(false)
        preferences.setForegroundServiceRunning(false)
    }

    private fun updatePresenceUI(peoplePresent: Boolean, method: String, details: String) {
        val color = if (peoplePresent) {
            ContextCompat.getColor(this, R.color.presence_detected)
        } else {
            ContextCompat.getColor(this, R.color.presence_not_detected)
        }

        statusIndicator.setImageResource(
            if (peoplePresent) R.drawable.ic_status_active else R.drawable.ic_status_inactive
        )

        statusText.setTextColor(color)
        statusText.text = buildString {
            append("Status: ")
            append(if (peoplePresent) "PEOPLE DETECTED ✓" else "No one home")
            append("\nMethod: ")
            append(method)
            append("\nDetails: ")
            append(details)
        }

        addLog(
            "[${method}] ${if (peoplePresent) "✓ PRESENCE DETECTED" else "✗ No presence"} - $details"
        )
    }

    private fun addLog(message: String) {
        runOnUiThread {
            val timeFormat = SimpleDateFormat.getTimeInstance(SimpleDateFormat.MEDIUM, Locale.getDefault())
            val timestamp = timeFormat.format(Date())

            detectionLog.append("\n[$timestamp] $message")

            // Auto-scroll to bottom
            logScrollView.post {
                logScrollView.smoothScrollTo(0, detectionLog.bottom)
            }
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }

            if (allGranted) {
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
                startDetection()
            } else {
                Toast.makeText(this, "Some permissions denied - app might have limited functionality", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Não parar o serviço aqui para manter a detecção em background
        detectionManager?.destroy()
    }
}
