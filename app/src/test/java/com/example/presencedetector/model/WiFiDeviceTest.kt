package com.example.presencedetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WiFiDeviceTest {

  @Test
  fun testSignalStrength() {
    val deviceExcellent = WiFiDevice("Test", "00:00:00:00:00:00", -20, 2412)
    assertEquals("🟢 Excellent (-30 to 0 dBm)", deviceExcellent.getSignalStrength())

    val deviceGood = WiFiDevice("Test", "00:00:00:00:00:00", -60, 2412)
    assertEquals("🟡 Good (-67 to -31 dBm)", deviceGood.getSignalStrength())

    val deviceFair = WiFiDevice("Test", "00:00:00:00:00:00", -69, 2412)
    assertEquals("🟠 Fair (-70 to -68 dBm)", deviceFair.getSignalStrength())

    val deviceWeak = WiFiDevice("Test", "00:00:00:00:00:00", -80, 2412)
    assertEquals("🔴 Weak (< -70 dBm)", deviceWeak.getSignalStrength())
  }

  @Test
  fun testIsHidden() {
    val hiddenDevice = WiFiDevice("", "00:00:00:00:00:00", -50, 2412)
    assertTrue(hiddenDevice.isHidden)

    val unknownSsidDevice = WiFiDevice("<unknown ssid>", "00:00:00:00:00:00", -50, 2412)
    assertTrue(unknownSsidDevice.isHidden)

    val visibleDevice = WiFiDevice("MyWiFi", "00:00:00:00:00:00", -50, 2412)
    assertFalse(visibleDevice.isHidden)
  }

  @Test
  fun testCategoryClassification() {
    // Test manual category override
    val manualDevice =
      WiFiDevice("Unknown", "00:00:00:00:00:00", -50, 2412, manualCategory = DeviceCategory.LAPTOP)
    assertEquals(DeviceCategory.LAPTOP, manualDevice.category)

    // Test automatic classification via getter
    val autoDevice = WiFiDevice("iPhone of User", "00:00:00:00:00:00", -50, 2412)
    assertEquals(DeviceCategory.SMARTPHONE, autoDevice.category)
  }

  @Test
  fun testIsWifi6() {
    // Test by standard int
    val wifi6Device = WiFiDevice("Router", "aa:bb:cc", -50, 5000, standard = 6)
    assertTrue(wifi6Device.isWifi6)

    val wifi7Device = WiFiDevice("Router", "aa:bb:cc", -50, 5000, standard = 7)
    assertTrue(wifi7Device.isWifi6)

    // Test by capabilities string
    val wifi6CapDevice = WiFiDevice("Router", "aa:bb:cc", -50, 5000, capabilities = "[WIFI6]")
    assertTrue(wifi6CapDevice.isWifi6)

    val axDevice = WiFiDevice("Router", "aa:bb:cc", -50, 5000, capabilities = "[AX]")
    assertTrue(axDevice.isWifi6)

    // Test negative
    val legacyDevice = WiFiDevice("Router", "aa:bb:cc", -50, 5000, standard = 5)
    assertFalse(legacyDevice.isWifi6)
  }

  @Test
  fun testDataClassMethods() {
    val device1 = WiFiDevice("Name", "AA:BB:CC:DD:EE:FF", -50, 2412)
    val device2 = WiFiDevice("Name", "AA:BB:CC:DD:EE:FF", -50, 2412)
    val device3 = WiFiDevice("Other", "AA:BB:CC:DD:EE:FF", -50, 2412)

    // Equals
    assertTrue(device1 == device2)
    assertFalse(device1 == device3)

    // HashCode
    assertEquals(device1.hashCode(), device2.hashCode())

    // ToString
    assertTrue(device1.toString().contains("Name"))

    // Copy
    val copy = device1.copy(ssid = "Copied")
    assertEquals("Copied", copy.ssid)
  }
}
