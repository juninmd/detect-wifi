package com.example.presencedetector.services

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.*

/**
 * WiFi-based presence detection service.
 * Scans for WiFi networks to detect if there are devices/people in the area.
 */
class WiFiDetectionService(context: Context) {
    companion object {
        private const val TAG = "WiFiDetector"
        private const val SCAN_INTERVAL = 5000L // 5 seconds
        private const val SIGNAL_THRESHOLD = -70 // dBm
    }

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private var scanJob: Job? = null
    private var presenceListener: PresenceListener? = null
    private var isScanning = false

    fun interface PresenceListener {
        fun onPresenceDetected(peopleDetected: Boolean, details: String)
    }

    fun setPresenceListener(listener: PresenceListener) {
        this.presenceListener = listener
    }

    fun startScanning() {
        if (isScanning) {
            Log.w(TAG, "WiFi scanning already started")
            return
        }

        isScanning = true
        Log.i(TAG, "Starting WiFi scanning")

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
        Log.i(TAG, "WiFi scanning stopped")
    }

    private suspend fun performScan() {
        try {
            if (wifiManager == null) {
                Log.e(TAG, "WiFi Manager not available")
                return
            }

            val scanResults = wifiManager!!.scanResults

            if (scanResults.isNullOrEmpty()) {
                Log.d(TAG, "No WiFi networks found")
                notifyPresence(false, "No WiFi networks detected")
                return
            }

            val presenceDetected = analyzeSignals(scanResults)
            val details = "Found ${scanResults.size} networks. Presence: ${if (presenceDetected) "YES" else "NO"}"

            notifyPresence(presenceDetected, details)

        } catch (e: Exception) {
            Log.e(TAG, "Error during WiFi scan", e)
        }
    }

    private fun analyzeSignals(results: List<android.net.wifi.ScanResult>): Boolean {
        var strongSignalCount = 0
        val totalNetworks = results.size

        for (result in results) {
            val level = result.level
            Log.d(TAG, "Network: ${result.SSID} Level: ${level}dBm")

            if (level >= SIGNAL_THRESHOLD) {
                strongSignalCount++
            }
        }

        return strongSignalCount > 0 && totalNetworks > 0
    }

    private fun notifyPresence(detected: Boolean, details: String) {
        presenceListener?.let {
            mainHandler.post {
                it.onPresenceDetected(detected, details)
            }
        }
        Log.i(TAG, "Presence detection result: $details")
    }

    fun isScanning(): Boolean = isScanning

    fun destroy() {
        stopScanning()
        scope.cancel()
    }
}
