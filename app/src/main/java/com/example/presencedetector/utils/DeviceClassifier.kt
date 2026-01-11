package com.example.presencedetector.utils

import com.example.presencedetector.model.DeviceCategory

/**
 * Utility to classify devices based on SSID and BSSID patterns.
 */
object DeviceClassifier {
    
    fun classify(ssid: String, bssid: String): DeviceCategory {
        val name = ssid.lowercase()
        
        return when {
            // Hidden Networks
            ssid.isEmpty() || ssid == "<unknown ssid>" -> DeviceCategory.ROUTER
            
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
            
            // Routers (Frequências comuns em nomes de rede)
            name.contains("2.4g") || name.contains("5g") || name.contains("router") || 
            name.contains("gateway") || name.contains("tp-link") || name.contains("d-link") ||
            name.contains("familia") || name.contains("adriana") -> DeviceCategory.ROUTER
            
            // Smartphones
            name.contains("iphone") || name.contains("android") || name.contains("galaxy") || 
            name.contains("note") || name.contains("pixel") -> DeviceCategory.SMARTPHONE
            
            // Default
            else -> DeviceCategory.UNKNOWN
        }
    }
}
