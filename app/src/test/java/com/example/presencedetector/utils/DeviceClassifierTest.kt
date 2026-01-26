package com.example.presencedetector.utils

import com.example.presencedetector.model.DeviceCategory
import org.junit.Test
import org.junit.Assert.*

class DeviceClassifierTest {

    @Test
    fun `classify should identify smartphones`() {
        assertEquals(DeviceCategory.SMARTPHONE, DeviceClassifier.classify("iPhone 13", "00:00:00:00:00:00"))
        assertEquals(DeviceCategory.SMARTPHONE, DeviceClassifier.classify("Galaxy S22", "00:00:00:00:00:00"))
        assertEquals(DeviceCategory.SMARTPHONE, DeviceClassifier.classify("Pixel 7", "00:00:00:00:00:00"))
        assertEquals(DeviceCategory.SMARTPHONE, DeviceClassifier.classify("Android", "00:00:00:00:00:00"))
    }

    @Test
    fun `classify should identify smart TVs`() {
        assertEquals(DeviceCategory.SMART_TV, DeviceClassifier.classify("Samsung TV", "00:00:00:00:00:00"))
        assertEquals(DeviceCategory.SMART_TV, DeviceClassifier.classify("LG WebOS", "00:00:00:00:00:00"))
        assertEquals(DeviceCategory.SMART_TV, DeviceClassifier.classify("Roku Stick", "00:00:00:00:00:00"))
    }

    @Test
    fun `classify should identify smart lights`() {
        assertEquals(DeviceCategory.SMART_LIGHT, DeviceClassifier.classify("Philips Hue", "00:00:00:00:00:00"))
        assertEquals(DeviceCategory.SMART_LIGHT, DeviceClassifier.classify("Tuya Bulb", "00:00:00:00:00:00"))
        assertEquals(DeviceCategory.SMART_LIGHT, DeviceClassifier.classify("Yeelight", "00:00:00:00:00:00"))
    }

    @Test
    fun `classify should identify Alexa devices`() {
        assertEquals(DeviceCategory.ALEXA, DeviceClassifier.classify("Echo Dot", "00:00:00:00:00:00"))
        assertEquals(DeviceCategory.ALEXA, DeviceClassifier.classify("Alexa", "00:00:00:00:00:00"))
    }

    @Test
    fun `classify should identify routers`() {
        assertEquals(DeviceCategory.ROUTER, DeviceClassifier.classify("TP-Link_5G", "00:00:00:00:00:00"))
        assertEquals(DeviceCategory.ROUTER, DeviceClassifier.classify("D-Link Router", "00:00:00:00:00:00"))
    }

    @Test
    fun `classify should detect mobile hotspots`() {
        // Based on isMobileHotspot logic
        assertEquals(DeviceCategory.SMARTPHONE, DeviceClassifier.classify("John's iPhone", "00:00:00:00:00:00"))
        assertEquals(DeviceCategory.SMARTPHONE, DeviceClassifier.classify("Redmi Note 10", "00:00:00:00:00:00"))
    }

    @Test
    fun `classify should return unknown for generic names`() {
        assertEquals(DeviceCategory.UNKNOWN, DeviceClassifier.classify("Unknown Device", "00:00:00:00:00:00"))
        assertEquals(DeviceCategory.UNKNOWN, DeviceClassifier.classify("MyNetwork", "00:00:00:00:00:00"))
    }
}
