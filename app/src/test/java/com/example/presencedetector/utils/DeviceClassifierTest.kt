package com.example.presencedetector.utils

import com.example.presencedetector.model.DeviceCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceClassifierTest {

    @Test
    fun `classify identifies Smartphones`() {
        assertEquals(DeviceCategory.SMARTPHONE, DeviceClassifier.classify("iPhone of User", "any"))
        assertEquals(DeviceCategory.SMARTPHONE, DeviceClassifier.classify("My Android", "any"))
        assertEquals(DeviceCategory.SMARTPHONE, DeviceClassifier.classify("Galaxy S21", "any"))
        assertEquals(DeviceCategory.SMARTPHONE, DeviceClassifier.classify("Pixel 6", "any"))
    }

    @Test
    fun `classify identifies Mobile Hotspots`() {
        // Based on logic in DeviceClassifier.isMobileHotspot
        assertEquals(DeviceCategory.SMARTPHONE, DeviceClassifier.classify("Personal Hotspot", "any"))
        assertEquals(DeviceCategory.SMARTPHONE, DeviceClassifier.classify("Redmi Note 8", "any"))
    }

    @Test
    fun `classify identifies Smart Home Devices`() {
        assertEquals(DeviceCategory.ALEXA, DeviceClassifier.classify("Echo Dot", "any"))
        assertEquals(DeviceCategory.SMART_LIGHT, DeviceClassifier.classify("Living Room Light", "any"))
        assertEquals(DeviceCategory.SMART_TV, DeviceClassifier.classify("Samsung TV", "any"))
        assertEquals(DeviceCategory.SMART_TV, DeviceClassifier.classify("Roku Stick", "any"))
    }

    @Test
    fun `classify identifies Routers`() {
        assertEquals(DeviceCategory.ROUTER, DeviceClassifier.classify("TP-Link_Guest", "any"))
        assertEquals(DeviceCategory.ROUTER, DeviceClassifier.classify("Familia Souza 2.4G", "any"))
    }

    @Test
    fun `classify returns Unknown for generic names`() {
        assertEquals(DeviceCategory.UNKNOWN, DeviceClassifier.classify("MyWifi", "any"))
        assertEquals(DeviceCategory.UNKNOWN, DeviceClassifier.classify("Visitor", "any"))
    }
}
