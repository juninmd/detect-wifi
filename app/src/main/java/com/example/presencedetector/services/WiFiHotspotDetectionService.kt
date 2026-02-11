package com.example.presencedetector.services

import android.Manifest
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.*

/**
 * WiFi Hotspot detection service - detects mobile devices sharing WiFi. Complements
 * WiFiDetectionService with advanced hotspot recognition.
 */
class WiFiHotspotDetectionService(private val context: Context) {
  companion object {
    private const val TAG = "WiFiHotspot"
    private const val SCAN_INTERVAL = 5000L // 5 seconds
  }

  private val wifiManager: WifiManager? =
    context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
  private val mainHandler = Handler(Looper.getMainLooper())
  private val scope = CoroutineScope(Dispatchers.Main + Job())

  private var scanJob: Job? = null
  private var hotspotListener: HotspotListener? = null
  private var isScanning = false
  private val detectedHotspots = mutableMapOf<String, Long>()

  interface HotspotListener {
    fun onHotspotDetected(ssid: String, bssid: String, signal: Int)

    fun onHotspotsUpdated(count: Int)
  }

  fun setHotspotListener(listener: HotspotListener) {
    this.hotspotListener = listener
  }

  @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
  fun startDetection() {
    if (isScanning) return
    isScanning = true
    Log.i(TAG, "🔥 Starting hotspot detection...")

    scanJob =
      scope.launch {
        while (isActive) {
          performScan()
          delay(SCAN_INTERVAL)
        }
      }
  }

  fun stopDetection() {
    scanJob?.cancel()
    scanJob = null
    isScanning = false
    Log.i(TAG, "❌ Hotspot detection stopped")
  }

  @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
  private suspend fun performScan() {
    try {
      val scanResults = wifiManager?.scanResults
      if (scanResults.isNullOrEmpty()) return

      scanResults.forEach { result ->
        val ssid = result.SSID ?: return@forEach
        if (isMobileHotspot(ssid)) {
          val bssid = result.BSSID ?: "00:00:00:00:00:00"
          val signal = result.level

          detectedHotspots[bssid] = System.currentTimeMillis()
          mainHandler.post { hotspotListener?.onHotspotDetected(ssid, bssid, signal) }
        }
      }

      // Update count
      mainHandler.post { hotspotListener?.onHotspotsUpdated(detectedHotspots.size) }
    } catch (e: Exception) {
      Log.e(TAG, "Scan error", e)
    }
  }

  private fun isMobileHotspot(ssid: String): Boolean {
    val lowerSsid = ssid.lowercase()

    val mobilePatterns =
      listOf(
        "iphone",
        "android",
        "samsung",
        "xiaomi",
        "redmi",
        "oneplus",
        "pixel",
        "motorola",
        "huawei",
        "poco",
        "nokia",
        "realme",
        "vivo",
        "oppo",
        "honor",
        "personal",
        "hotspot",
        "moto",
        "galaxy",
        "note"
      )

    return mobilePatterns.any { lowerSsid.contains(it) }
  }

  fun isScanning(): Boolean = isScanning

  fun getDetectedHotspotCount(): Int = detectedHotspots.size

  fun destroy() {
    stopDetection()
    scope.cancel()
  }
}
