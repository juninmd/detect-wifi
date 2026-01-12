package com.example.presencedetector.model

/**
 * Data class representing a detected WiFi device/network.
 */
data class WiFiDevice(
    val ssid: String,
    val bssid: String,
    val level: Int, // dBm
    val frequency: Int,
    var nickname: String? = null,
    val lastSeen: Long = System.currentTimeMillis(),
    var manualCategory: DeviceCategory? = null,
    val isHotspot: Boolean = false  // New: indicates if this is a mobile hotspot
) {
    val category: DeviceCategory
        get() = manualCategory ?: com.example.presencedetector.utils.DeviceClassifier.classify(ssid, bssid)

    val isHidden: Boolean
        get() = ssid.isEmpty() || ssid == "<unknown ssid>"

    /**
     * Get signal strength indicator
     */
    fun getSignalStrength(): String = when (level) {
        in -30..0 -> "🟢 Excellent (-30 to 0 dBm)"
        in -67..-31 -> "🟡 Good (-67 to -31 dBm)"
        in -70..-68 -> "🟠 Fair (-70 to -68 dBm)"
        else -> "🔴 Weak (< -70 dBm)"
    }
}
