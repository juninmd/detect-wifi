package com.example.presencedetector.services

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.presencedetector.model.WiFiDevice
import com.example.presencedetector.utils.DeviceClassifier
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

/** WiFi-based presence detection service. */
open class WiFiDetectionService(private val context: Context) {
  companion object {
    private const val TAG = "WiFiDetector"
    private const val SCAN_INTERVAL = 15000L // Optimized to 15 seconds for battery
    private const val SIGNAL_THRESHOLD = -85
  }

  private val wifiManager =
    context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

  // Use Default dispatcher for CPU-intensive tasks (parsing scan results)
  // Use IO if we consider WifiManager IPC as IO, but Default is fine for the mix
  private val scope = CoroutineScope(Dispatchers.Default + Job())

  private var scanJob: Job? = null
  private var presenceListener: PresenceListener? = null
  private val isScanning = AtomicBoolean(false)

  fun interface PresenceListener {
    fun onPresenceDetected(peopleDetected: Boolean, devices: List<WiFiDevice>, details: String)
  }

  open fun setPresenceListener(listener: PresenceListener) {
    this.presenceListener = listener
  }

  open fun startScanning() {
    if (isScanning.get()) return
    isScanning.set(true)

    // Launch scanning loop on background thread
    scanJob =
      scope.launch {
        while (isActive) {
          performScan()
          delay(SCAN_INTERVAL)
        }
      }
  }

  open fun stopScanning() {
    scanJob?.cancel()
    scanJob = null
    isScanning.set(false)
  }

  // Removed @RequiresPermission to handle it safely inside
  private suspend fun performScan() {
    if (
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED
    ) {
      Log.w(TAG, "Skipping scan: Location permission not granted")
      notifyPresence(false, emptyList(), "Permission Denied")
      return
    }

    try {
      // WifiManager.scanResults is an IPC call, so it's good to be off the main thread
      val scanResults = wifiManager?.scanResults
      if (scanResults.isNullOrEmpty()) {
        notifyPresence(false, emptyList(), "No networks")
        return
      }

      // Use withContext to ensure this mapping runs on Default dispatcher if not already
      // (though performScan is called from Default scope, so redundant but explicit)
      val devices = withContext(Dispatchers.Default) {
          // Detect both standard networks AND mobile hotspots
          scanResults.mapNotNull { result ->
            val ssid = result.SSID ?: "Unknown"
            val isHotspot = DeviceClassifier.isMobileHotspot(ssid)

            // Extract WiFi Standard (API 30+)
            val standard =
              if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                result.wifiStandard
              } else {
                0
              }

            WiFiDevice(
              ssid = ssid,
              bssid = result.BSSID ?: "00:00:00:00:00:00",
              level = result.level,
              frequency = result.frequency,
              nickname = if (isHotspot) "📱 $ssid (Hotspot)" else ssid,
              capabilities = result.capabilities ?: "",
              channelWidth = result.channelWidth, // 0 if unknown
              standard = standard
            )
          }
      }

      val presenceDetected = devices.any { it.level >= -70 }

      // More details including hotspot count
      val hotspotCount = devices.count { it.nickname?.contains("Hotspot") == true }
      val details =
        "Found ${devices.size} networks" + if (hotspotCount > 0) " ($hotspotCount hotspots)" else ""

      notifyPresence(presenceDetected, devices, details)
    } catch (e: Exception) {
      Log.e(TAG, "Scan error", e)
    }
  }

  private fun notifyPresence(detected: Boolean, devices: List<WiFiDevice>, details: String) {
    // Notify listener on the current (background) thread.
    // The consumer (PresenceDetectionManager) is responsible for handling threading if needed.
    presenceListener?.onPresenceDetected(detected, devices, details)
  }

  fun isScanning(): Boolean = isScanning.get()

  fun destroy() {
    stopScanning()
    scope.cancel()
  }
}
