package com.example.presencedetector.services

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import com.example.presencedetector.model.WiFiDevice

/**
 * WiFi-based presence detection service.
 */
class WiFiDetectionService(private val context: Context) {
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

    // Removed @RequiresPermission to handle it safely inside
    private suspend fun performScan() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
             Log.w(TAG, "Skipping scan: Location permission not granted")
             notifyPresence(false, emptyList(), "Permission Denied")
             return
        }

        try {
            val scanResults = wifiManager?.scanResults
            if (scanResults.isNullOrEmpty()) {
                notifyPresence(false, emptyList(), "No networks")
                return
            }

            // Detect both standard networks AND mobile hotspots
            val devices = scanResults.mapNotNull { result ->
                val ssid = result.SSID ?: "Unknown"
                val isHotspot = isLikelyMobileHotspot(ssid)

                WiFiDevice(
                    ssid = ssid,
                    bssid = result.BSSID ?: "00:00:00:00:00:00",
                    level = result.level,
                    frequency = result.frequency,
                    nickname = if (isHotspot) "📱 $ssid (Hotspot)" else ssid
                )
            }

            val presenceDetected = devices.any { it.level >= -70 }

            // More details including hotspot count
            val hotspotCount = devices.count { it.nickname?.contains("Hotspot") == true }
            val details = "Found ${devices.size} networks" +
                         if (hotspotCount > 0) " ($hotspotCount hotspots)" else ""

            notifyPresence(presenceDetected, devices, details)

        } catch (e: Exception) {
            Log.e(TAG, "Scan error", e)
        }
    }

    /**
     * Detects if SSID looks like a mobile hotspot.
     * Mobile hotspots typically have patterns like:
     * - "iPhone", "Android", "Samsung", etc.
     * - End with numbers or alphanumerics
     * - No spaces or special chars (usually)
     */
    private fun isLikelyMobileHotspot(ssid: String): Boolean {
        val lowerSsid = ssid.lowercase()

        // Common mobile hotspot patterns
        val mobilePatterns = listOf(
            "iphone", "android", "samsung", "xiaomi", "redmi",
            "oneplus", "pixel", "motorola", "huawei", "poco",
            "nokia", "realme", "vivo", "oppo", "honor",
            "personal", "hotspot", "moto", "galaxy", "note"
        )

        // Check if contains mobile patterns
        val containsMobilePattern = mobilePatterns.any { lowerSsid.contains(it) }

        // Check if SSID is very short (typical for hotspots)
        val isShortName = ssid.length < 15 && !ssid.contains("_") && !ssid.contains("-")

        return containsMobilePattern || (isShortName && ssid.matches(Regex("[A-Za-z0-9]+")))
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
