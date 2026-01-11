package com.example.presencedetector.services

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.presencedetector.utils.NotificationUtil

/**
 * Combined presence detection service using WiFi as primary
 * and Bluetooth as fallback method.
 */
class PresenceDetectionManager(private val context: Context) {
    companion object {
        private const val TAG = "PresenceDetectionManager"
        private const val DETECTION_TIMEOUT = 30000L // 30 seconds
    }

    private val wifiService = WiFiDetectionService(context)
    private val bluetoothService = BluetoothDetectionService(context)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var presenceListener: PresenceListener? = null
    private var wifiPresenceDetected = false
    private var bluetoothPresenceDetected = false
    private var lastWifiDetection = 0L
    private var lastBluetoothDetection = 0L
    private var lastNotificationTime = 0L
    private var lastPresenceState = false

    fun interface PresenceListener {
        fun onPresenceChanged(peoplePresent: Boolean, method: String, details: String)
    }

    init {
        setupListeners()
    }

    private fun setupListeners() {
        // WiFi listener
        wifiService.setPresenceListener { detected, details ->
            Log.d(TAG, "WiFi detection: $detected - $details")
            wifiPresenceDetected = detected

            if (detected) {
                lastWifiDetection = System.currentTimeMillis()
            }

            evaluatePresence("WiFi", details)
        }

        // Bluetooth listener
        bluetoothService.setPresenceListener { detected, details ->
            Log.d(TAG, "Bluetooth detection: $detected - $details")
            bluetoothPresenceDetected = detected

            if (detected) {
                lastBluetoothDetection = System.currentTimeMillis()
            }

            evaluatePresence("Bluetooth", details)
        }
    }

    fun startDetection() {
        Log.i(TAG, "Starting presence detection")
        notifyStatus("Starting detection...")

        wifiService.startScanning()
        bluetoothService.startScanning()
    }

    fun stopDetection() {
        Log.i(TAG, "Stopping presence detection")
        wifiService.stopScanning()
        bluetoothService.stopScanning()
        notifyStatus("Detection stopped")
    }

    private fun evaluatePresence(method: String, details: String) {
        val peoplePresent = isPeoplePresent()

        // Notificar mudança de estado
        if (peoplePresent != lastPresenceState) {
            lastPresenceState = peoplePresent
            sendNotification(peoplePresent, method, details)
        }

        presenceListener?.let {
            mainHandler.post {
                it.onPresenceChanged(peoplePresent, method, details)
            }
        }

        Log.i(TAG, "Presence evaluation - WiFi: $wifiPresenceDetected, Bluetooth: $bluetoothPresenceDetected, Overall: $peoplePresent")
    }

    private fun isPeoplePresent(): Boolean {
        val now = System.currentTimeMillis()

        // WiFi is primary - if detected within timeout, people are present
        if (wifiPresenceDetected && (now - lastWifiDetection) < DETECTION_TIMEOUT) {
            return true
        }

        // Bluetooth as fallback
        if (bluetoothPresenceDetected && (now - lastBluetoothDetection) < DETECTION_TIMEOUT) {
            return true
        }

        return false
    }

    private fun sendNotification(peoplePresent: Boolean, method: String, details: String) {
        val now = System.currentTimeMillis()

        // Evita notificações muito frequentes (máximo uma a cada 30 segundos)
        if (now - lastNotificationTime < 30000) {
            return
        }

        lastNotificationTime = now

        val title = if (peoplePresent) "🏠 Presença Detectada!" else "🏠 Casa Vazia"
        val message = "Método: $method - $details"

        NotificationUtil.sendPresenceNotification(context, title, message, peoplePresent)
        Log.i(TAG, "Notification sent: $title - $message")
    }

    private fun notifyStatus(status: String) {
        Log.d(TAG, "Status: $status")
    }

    fun setPresenceListener(listener: PresenceListener) {
        this.presenceListener = listener
    }

    fun getDetectionStatus(): String {
        return buildString {
            append("WiFi: ${if (wifiService.isScanning()) "Scanning" else "Stopped"}")
            append(" | Bluetooth: ${if (bluetoothService.isScanning()) "Scanning" else "Stopped"}")
            append(" | People Detected: ${if (isPeoplePresent()) "YES" else "NO"}")
        }
    }

    fun destroy() {
        stopDetection()
        wifiService.destroy()
        bluetoothService.destroy()
    }
}
