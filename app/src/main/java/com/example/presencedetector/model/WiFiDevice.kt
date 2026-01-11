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
    var manualCategory: DeviceCategory? = null
) {
    val category: DeviceCategory
        get() = manualCategory ?: com.example.presencedetector.utils.DeviceClassifier.classify(ssid, bssid)
        
    val isHidden: Boolean
        get() = ssid.isEmpty() || ssid == "<unknown ssid>"
}
