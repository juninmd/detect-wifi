package com.example.presencedetector.utils

import org.junit.Assert.assertTrue
import org.junit.Test

class DevicePatternsTest {

    @Test
    fun patterns_areNotEmpty() {
        assertTrue(DevicePatterns.SMARTPHONE_HIGH_CONFIDENCE.isNotEmpty())
        assertTrue(DevicePatterns.E_READERS.isNotEmpty())
        assertTrue(DevicePatterns.SMART_HOME_ASSISTANTS.isNotEmpty())
        assertTrue(DevicePatterns.SMART_LIGHTS.isNotEmpty())
        assertTrue(DevicePatterns.SMART_TVS.isNotEmpty())
        assertTrue(DevicePatterns.ROUTERS.isNotEmpty())
        assertTrue(DevicePatterns.MOBILE_HOTSPOT_PATTERNS.isNotEmpty())
    }

    @Test
    fun patterns_containExpectedValues() {
        assertTrue(DevicePatterns.SMARTPHONE_HIGH_CONFIDENCE.contains("iphone"))
        assertTrue(DevicePatterns.MOBILE_HOTSPOT_PATTERNS.contains("android"))
        assertTrue(DevicePatterns.SMART_TVS.contains("samsung"))
        assertTrue(DevicePatterns.ROUTERS.contains("router"))
    }
}
