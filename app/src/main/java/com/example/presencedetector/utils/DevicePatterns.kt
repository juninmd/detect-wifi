package com.example.presencedetector.utils

/**
 * Constants for device classification patterns.
 */
object DevicePatterns {

    val SMARTPHONE_HIGH_CONFIDENCE = listOf(
        "iphone", "android", "galaxy", "note", "pixel"
    )

    val E_READERS = listOf("kindle", "ebook")

    val SMART_HOME_ASSISTANTS = listOf("alexa", "echo", "amazon")

    val SMART_LIGHTS = listOf(
        "light", "bulb", "hue", "tuya", "smart life",
        "yeelight", "continua", "batcaverna"
    )

    val SMART_TVS = listOf(
        "tv", "samsung", "lg", "sony", "bravia",
        "firestick", "chromecast", "roku"
    )

    val ROUTERS = listOf(
        "2.4g", "5g", "router", "gateway",
        "tp-link", "d-link", "familia", "adriana"
    )

    val MOBILE_HOTSPOT_PATTERNS = listOf(
        "iphone", "android", "samsung", "xiaomi", "redmi",
        "oneplus", "pixel", "motorola", "huawei", "poco",
        "nokia", "realme", "vivo", "oppo", "honor",
        "personal", "hotspot", "moto", "galaxy", "note",
        "12 pro", "s21", "s22", "note 20", "iphone 13"
    )
}
