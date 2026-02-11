package com.example.presencedetector.model

/** Data class representing a detected device (WiFi or Bluetooth). */
data class WiFiDevice(
  val ssid: String,
  val bssid: String,
  val level: Int, // dBm
  val frequency: Int,
  var nickname: String? = null,
  val lastSeen: Long = System.currentTimeMillis(),
  var manualCategory: DeviceCategory? = null,
  val isHotspot: Boolean = false,
  val source: DeviceSource = DeviceSource.WIFI,

  // New Advanced Fields
  val capabilities: String = "",
  val channelWidth: Int = 0, // MHz
  val standard: Int = 0 // ScanResult.WIFI_STANDARD_... (API 30+)
) {
  val category: DeviceCategory
    get() =
      manualCategory ?: com.example.presencedetector.utils.DeviceClassifier.classify(ssid, bssid)

  val isHidden: Boolean
    get() = ssid.isEmpty() || ssid == "<unknown ssid>"

  val isWifi6: Boolean
    get() =
      standard == 6 ||
        standard == 7 ||
        capabilities.contains("WIFI6") ||
        capabilities.contains("AX")

  /** Get signal strength indicator */
  fun getSignalStrength(): String =
    when (level) {
      in -30..0 -> "🟢 Excellent (-30 to 0 dBm)"
      in -67..-31 -> "🟡 Good (-67 to -31 dBm)"
      in -70..-68 -> "🟠 Fair (-70 to -68 dBm)"
      else -> "🔴 Weak (< -70 dBm)"
    }
}

enum class DeviceSource {
  WIFI,
  BLUETOOTH
}
