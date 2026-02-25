package com.example.presencedetector.utils

import com.example.presencedetector.model.DeviceCategory

/** Utility to classify devices based on SSID and BSSID patterns. */
object DeviceClassifier {

    private object Patterns {
        val SMARTPHONE_HIGH_CONFIDENCE = setOf("iphone", "android", "galaxy", "note", "pixel")
        val E_READERS = setOf("kindle", "ebook")
        val SMART_HOME_ASSISTANTS = setOf("alexa", "echo", "amazon")
        val SMART_LIGHTS = setOf("light", "bulb", "hue", "tuya", "smart life", "yeelight", "continua", "batcaverna")
        val SMART_TVS = setOf("tv", "samsung", "lg", "sony", "bravia", "firestick", "chromecast", "roku")
        val ROUTERS = setOf("2.4g", "5g", "router", "gateway", "tp-link", "d-link", "familia", "adriana")
        val MOBILE_HOTSPOT_PATTERNS = setOf(
            "iphone", "android", "samsung", "xiaomi", "redmi", "oneplus", "pixel", "motorola",
            "huawei", "poco", "nokia", "realme", "vivo", "oppo", "honor", "personal", "hotspot",
            "moto", "galaxy", "note", "12 pro", "s21", "s22", "note 20", "iphone 13"
        )
    }

    // Pre-compiled regex for hotspot detection
    private val SHORT_NAME_REGEX = Regex("[A-Za-z0-9]+")

    fun classify(ssid: String, bssid: String, isHotspot: Boolean = false): DeviceCategory {
        if (isHotspot) {
            return DeviceCategory.SMARTPHONE
        }

        val name = ssid.lowercase()

        return when {
            name.containsAny(Patterns.SMARTPHONE_HIGH_CONFIDENCE) -> DeviceCategory.SMARTPHONE
            name.containsAny(Patterns.E_READERS) -> DeviceCategory.KINDLE
            name.containsAny(Patterns.SMART_HOME_ASSISTANTS) -> DeviceCategory.ALEXA
            name.containsAny(Patterns.SMART_LIGHTS) -> DeviceCategory.SMART_LIGHT
            name.containsAny(Patterns.SMART_TVS) -> DeviceCategory.SMART_TV
            // Check hotspot patterns. If it matches, classify as Smartphone (assuming hotspots are mostly phones)
            name.containsAny(Patterns.MOBILE_HOTSPOT_PATTERNS) -> DeviceCategory.SMARTPHONE
            name.containsAny(Patterns.ROUTERS) -> DeviceCategory.ROUTER
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
        val containsMobilePattern = lowerSsid.containsAny(Patterns.MOBILE_HOTSPOT_PATTERNS)

        // Check if SSID is very short (typical for hotspots)
        val isShortName = ssid.length < 15 && !ssid.contains("_") && !ssid.contains("-")

        return containsMobilePattern || (isShortName && ssid.matches(SHORT_NAME_REGEX))
    }

    private fun String.containsAny(patterns: Collection<String>): Boolean {
        return patterns.any { this.contains(it) }
    }
}
