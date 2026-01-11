package com.example.presencedetector.model

/**
 * Categories for different types of detected devices.
 */
enum class DeviceCategory(val displayName: String, val iconRes: String) {
    SMARTPHONE("Smartphone", "📱"),
    SMART_TV("Smart TV", "📺"),
    SMART_LIGHT("Smart Light", "💡"),
    ALEXA("Alexa/Speaker", "🔊"),
    LAPTOP("Laptop/PC", "💻"),
    ROUTER("Router/AP", "🌐"),
    UNKNOWN("Unknown Device", "❓")
}
