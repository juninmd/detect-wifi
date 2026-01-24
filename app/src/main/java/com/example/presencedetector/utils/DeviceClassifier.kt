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
            name.contains("iphone") || name.contains("android") || name.contains("galaxy") ||
            name.contains("note") || name.contains("pixel") -> DeviceCategory.SMARTPHONE

            // Kindle / E-Readers
            name.contains("kindle") || name.contains("ebook") -> DeviceCategory.KINDLE

            // Alexa / Echo
            name.contains("alexa") || name.contains("echo") || name.contains("amazon") -> DeviceCategory.ALEXA

            // Smart Lights
            name.contains("light") || name.contains("bulb") || name.contains("hue") ||
            name.contains("tuya") || name.contains("smart life") || name.contains("yeelight") ||
            name.contains("continua") || name.contains("batcaverna") -> DeviceCategory.SMART_LIGHT

            // Smart TV
            name.contains("tv") || name.contains("samsung") || name.contains("lg") ||
            name.contains("sony") || name.contains("bravia") || name.contains("firestick") ||
            name.contains("chromecast") || name.contains("roku") -> DeviceCategory.SMART_TV

            // Mobile Hotspots (NEW) - Checked after specific devices to avoid false positives (e.g. Samsung TV)
            isMobileHotspot(name) -> DeviceCategory.SMARTPHONE

            // Routers (Frequências comuns em nomes de rede)
            name.contains("2.4g") || name.contains("5g") || name.contains("router") ||
            name.contains("gateway") || name.contains("tp-link") || name.contains("d-link") ||
            name.contains("familia") || name.contains("adriana") -> DeviceCategory.ROUTER

            // Default
            else -> DeviceCategory.UNKNOWN
        }
    }

    /**
     * Detects if SSID looks like a mobile hotspot.
     * Mobile hotspots typically have patterns like:
     * - Device names (iPhone, Android, Samsung, etc.)
     * - Short, simple names with no spaces
     */
    private fun isMobileHotspot(ssid: String): Boolean {
        val lowerSsid = ssid.lowercase()

        // Common mobile hotspot patterns
        val mobilePatterns = listOf(
            "iphone", "android", "samsung", "xiaomi", "redmi",
            "oneplus", "pixel", "motorola", "huawei", "poco",
            "nokia", "realme", "vivo", "oppo", "honor",
            "personal", "hotspot", "moto", "galaxy", "note",
            "12 pro", "s21", "s22", "note 20", "iphone 13"
        )

        return mobilePatterns.any { lowerSsid.contains(it) }
    }
}