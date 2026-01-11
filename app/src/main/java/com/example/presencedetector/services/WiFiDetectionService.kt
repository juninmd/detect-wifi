package com.example.presencedetector.services

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.*
import com.example.presencedetector.model.WiFiDevice

/**
 * WiFi-based presence detection service.
 */
class WiFiDetectionService(context: Context) {
    companion object {
        private const val TAG = "WiFiDetector"
        private const val SCAN_INTERVAL = 3000L // Updated to 3 seconds as requested
        private const val SIGNAL_THRESHOLD = -85
    }

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private var scanJob: Job? = null
    private var presenceListener: PresenceListener? = null
    private var isScanning = false

    fun interface PresenceListener {
        fun onPresenceDetected(peopleDetected: Boolean, devices: List<WiFiDevice>, details: String)
    }

    fun setPresenceListener(listener: PresenceListener) {
        this.presenceListener = listener
    }

    fun startScanning() {
        if (isScanning) return
        isScanning = true
        scanJob = scope.launch {
            while (isActive) {
                performScan()
                delay(SCAN_INTERVAL)
            }
        }
    }

    fun stopScanning() {
        scanJob?.cancel()
        scanJob = null
        isScanning = false
    }

    private suspend fun performScan() {
        try {
            val scanResults = wifiManager?.scanResults
            if (scanResults.isNullOrEmpty()) {
                notifyPresence(false, emptyList(), "No networks")
                return
            }

            val devices = scanResults.map { result ->
                WiFiDevice(
                    ssid = result.SSID ?: "Unknown",
                    bssid = result.BSSID ?: "00:00:00:00:00:00",
                    level = result.level,
                    frequency = result.frequency
                )
            }

            val presenceDetected = devices.any { it.level >= -70 }
            notifyPresence(presenceDetected, devices, "Found ${devices.size} networks")

        } catch (e: Exception) {
            Log.e(TAG, "Scan error", e)
        }
    }

    private fun notifyPresence(detected: Boolean, devices: List<WiFiDevice>, details: String) {
        presenceListener?.let {
            mainHandler.post { it.onPresenceDetected(detected, devices, details) }
        }
    }

    fun isScanning(): Boolean = isScanning
    
    fun destroy() {
        stopScanning()
        scope.cancel()
    }
}
