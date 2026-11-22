package com.example.presencedetector.utils

import com.example.presencedetector.model.DeviceCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

  @Test
  fun `short patterns match whole words, not fragments of other words`() {
    // "lg" in Algar, "note"/"ebook" in Notebook, "hue" in Hueston, "tv" in Ativa used to hit.
    assertEquals(DeviceCategory.ROUTER, DeviceClassifier.classify("Algar_Fibra_5G", "any"))
    assertEquals(DeviceCategory.UNKNOWN, DeviceClassifier.classify("Notebook_Sala", "any"))
    assertEquals(DeviceCategory.UNKNOWN, DeviceClassifier.classify("Hueston", "any"))
    assertEquals(DeviceCategory.UNKNOWN, DeviceClassifier.classify("Casa Ativa", "any"))
    assertFalse(DeviceClassifier.isMobileHotspot("Notebook_Escritorio"))
  }

  @Test
  fun `short patterns still match as separate or camel-case words`() {
    assertEquals(DeviceCategory.SMART_TV, DeviceClassifier.classify("LG_WebOS_TV", "any"))
    assertEquals(DeviceCategory.SMART_TV, DeviceClassifier.classify("LivingRoomTV", "any"))
    assertEquals(DeviceCategory.SMART_LIGHT, DeviceClassifier.classify("Philips Hue Bridge", "any"))
    assertEquals(DeviceCategory.SMARTPHONE, DeviceClassifier.classify("GalaxyNote", "any"))
    assertEquals(DeviceCategory.ROUTER, DeviceClassifier.classify("Home_WiFi_5G", "any"))
  }

  @Test
  fun `isMobileHotspot detects hotspot patterns`() {
    assertTrue(DeviceClassifier.isMobileHotspot("iPhone of User"))
    assertTrue(DeviceClassifier.isMobileHotspot("AndroidAP"))
    assertTrue(DeviceClassifier.isMobileHotspot("Galaxy S21"))
    assertTrue(DeviceClassifier.isMobileHotspot("MyHotspot"))
    assertTrue(DeviceClassifier.isMobileHotspot("Abc12")) // Short alphanumeric

    assertFalse(DeviceClassifier.isMobileHotspot("Home_WiFi_5G"))
    assertFalse(DeviceClassifier.isMobileHotspot("TP-Link_1234"))
  }
}
