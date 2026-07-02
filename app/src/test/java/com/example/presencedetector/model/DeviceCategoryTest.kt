package com.example.presencedetector.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceCategoryTest {

  @Test
  fun testEnumValues() {
    assertEquals("Smartphone", DeviceCategory.SMARTPHONE.displayName)
    assertEquals("📱", DeviceCategory.SMARTPHONE.iconRes)

    assertEquals("Unknown Device", DeviceCategory.UNKNOWN.displayName)
    assertEquals("❓", DeviceCategory.UNKNOWN.iconRes)

    // Iterate all values to ensure full coverage
    DeviceCategory.values().forEach {
      it.displayName
      it.iconRes
    }
  }
}
