package com.example.presencedetector.utils

import com.example.presencedetector.model.DeviceCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceClassifierTest {

    @Test
    fun testClassifySmartphones() {
        assertEquals(DeviceCategory.SMARTPHONE, DeviceClassifier.classify("iPhone 13", "any:mac"))
        assertEquals(DeviceCategory.SMARTPHONE, DeviceClassifier.classify("Samsung Galaxy", "any:mac"))
        assertEquals(DeviceCategory.SMARTPHONE, DeviceClassifier.classify("Pixel 6", "any:mac"))
        assertEquals(DeviceCategory.SMARTPHONE, DeviceClassifier.classify("AndroidAP", "any:mac"))
    }

    @Test
    fun testClassifySmartTVs() {
        assertEquals(DeviceCategory.SMART_TV, DeviceClassifier.classify("Samsung Smart TV", "any:mac"))
        assertEquals(DeviceCategory.SMART_TV, DeviceClassifier.classify("LG WebOS TV", "any:mac"))
        assertEquals(DeviceCategory.SMART_TV, DeviceClassifier.classify("Roku Stick", "any:mac"))
        assertEquals(DeviceCategory.SMART_TV, DeviceClassifier.classify("Chromecast", "any:mac"))
    }

    @Test
    fun testClassifySmartLights() {
        assertEquals(DeviceCategory.SMART_LIGHT, DeviceClassifier.classify("Hue Light", "any:mac"))
        assertEquals(DeviceCategory.SMART_LIGHT, DeviceClassifier.classify("Yeelight Color", "any:mac"))
        assertEquals(DeviceCategory.SMART_LIGHT, DeviceClassifier.classify("Tuya Bulb", "any:mac"))
    }

    @Test
    fun testClassifyRouters() {
        assertEquals(DeviceCategory.ROUTER, DeviceClassifier.classify("TP-Link_2.4G", "any:mac"))
        assertEquals(DeviceCategory.ROUTER, DeviceClassifier.classify("D-Link Router", "any:mac"))
        assertEquals(DeviceCategory.ROUTER, DeviceClassifier.classify("Familia Smith", "any:mac"))
    }

    @Test
    fun testClassifyAlexa() {
        assertEquals(DeviceCategory.ALEXA, DeviceClassifier.classify("Alexa Dot", "any:mac"))
        assertEquals(DeviceCategory.ALEXA, DeviceClassifier.classify("Amazon Echo", "any:mac"))
    }

    @Test
    fun testClassifyKindle() {
        assertEquals(DeviceCategory.KINDLE, DeviceClassifier.classify("Kindle Paperwhite", "any:mac"))
    }

    @Test
    fun testClassifyUnknown() {
        assertEquals(DeviceCategory.UNKNOWN, DeviceClassifier.classify("RandomDeviceName", "any:mac"))
    }
}
