package com.example.presencedetector.utils

import com.example.presencedetector.model.DeviceCategory

/** Utility to classify devices based on SSID and BSSID patterns. */
object DeviceClassifier {

    private val SMARTPHONE_HIGH_CONFIDENCE = listOf("iphone", "android", "galaxy", "note", "pixel")
    private val E_READERS = listOf("kindle", "ebook")
    private val SMART_HOME_ASSISTANTS = listOf("alexa", "echo", "amazon")
    private val SMART_LIGHTS = listOf("light", "bulb", "hue", "tuya", "smart life", "yeelight", "continua", "batcaverna")
    private val SMART_TVS = listOf("tv", "samsung", "lg", "sony", "bravia", "firestick", "chromecast", "roku")
    private val ROUTERS = listOf("2.4g", "5g", "router", "gateway", "tp-link", "d-link", "familia", "adriana")
    private val MOBILE_HOTSPOT_PATTERNS = listOf(
        "iphone", "android", "samsung", "xiaomi", "redmi", "oneplus", "pixel", "motorola",
        "huawei", "poco", "nokia", "realme", "vivo", "oppo", "honor", "personal", "hotspot",
        "moto", "galaxy", "note", "12 pro", "s21", "s22", "note 20", "iphone 13"
    )

    fun classify(ssid: String, bssid: String): DeviceCategory {
        val name = ssid.lowercase()

        return when {
            name.containsAny(SMARTPHONE_HIGH_CONFIDENCE) -> DeviceCategory.SMARTPHONE
            name.containsAny(E_READERS) -> DeviceCategory.KINDLE
            name.containsAny(SMART_HOME_ASSISTANTS) -> DeviceCategory.ALEXA
            name.containsAny(SMART_LIGHTS) -> DeviceCategory.SMART_LIGHT
            name.containsAny(SMART_TVS) -> DeviceCategory.SMART_TV
            // Check hotspot patterns. If it matches, classify as Smartphone (assuming hotspots are mostly phones)
            name.containsAny(MOBILE_HOTSPOT_PATTERNS) -> DeviceCategory.SMARTPHONE
            name.containsAny(ROUTERS) -> DeviceCategory.ROUTER
            else -> DeviceCategory.UNKNOWN
        }
    }

    /**
     * Detects if SSID looks like a mobile hotspot. Encapsulates logic for hotspot detection including
     * patterns and heuristics.
     */
    fun isMobileHotspot(ssid: String): Boolean {
        val lowerSsid = ssid.lowercase()

        // Check if contains mobile patterns
        val containsMobilePattern = lowerSsid.containsAny(MOBILE_HOTSPOT_PATTERNS)

        // Check if SSID is very short (typical for hotspots)
        val isShortName = ssid.length < 15 && !ssid.contains("_") && !ssid.contains("-")

        return containsMobilePattern || (isShortName && ssid.matches(Regex("[A-Za-z0-9]+")))
    }

    private fun String.containsAny(patterns: List<String>): Boolean {
        return patterns.any { this.contains(it) }
    }
}
