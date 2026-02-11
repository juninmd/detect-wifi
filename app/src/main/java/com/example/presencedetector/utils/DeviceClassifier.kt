package com.example.presencedetector.utils

import com.example.presencedetector.model.DeviceCategory

/**
 * Utility to classify devices based on SSID and BSSID patterns.
 */
object DeviceClassifier {

    fun classify(ssid: String, bssid: String): DeviceCategory {
        val name = ssid.lowercase()

        return when {
            // Explicit Smartphones (High Confidence)
            name.containsAny(DevicePatterns.SMARTPHONE_HIGH_CONFIDENCE) -> DeviceCategory.SMARTPHONE

            // Kindle / E-Readers
            name.containsAny(DevicePatterns.E_READERS) -> DeviceCategory.KINDLE

            // Alexa / Echo
            name.containsAny(DevicePatterns.SMART_HOME_ASSISTANTS) -> DeviceCategory.ALEXA

            // Smart Lights
            name.containsAny(DevicePatterns.SMART_LIGHTS) -> DeviceCategory.SMART_LIGHT

            // Smart TV
            name.containsAny(DevicePatterns.SMART_TVS) -> DeviceCategory.SMART_TV

            // Mobile Hotspots (NEW) - Checked after specific devices to avoid false positives (e.g. Samsung TV)
            name.containsAny(DevicePatterns.MOBILE_HOTSPOT_PATTERNS) -> DeviceCategory.SMARTPHONE

            // Routers
            name.containsAny(DevicePatterns.ROUTERS) -> DeviceCategory.ROUTER

            // Default
            else -> DeviceCategory.UNKNOWN
        }
    }

    private fun String.containsAny(patterns: List<String>): Boolean {
        return patterns.any { this.contains(it) }
    }
}
