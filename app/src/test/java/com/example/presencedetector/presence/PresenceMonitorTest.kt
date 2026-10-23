package com.example.presencedetector.presence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PresenceMonitorTest {
  private fun device(mac: String) = PresentDevice(mac, -60, 0L, null)

  private fun snapshot(vararg unknown: String) =
    PresenceSnapshot(300, emptyList(), unknown.map(::device), 0)

  @Test
  fun `first poll only sets the baseline`() {
    assertTrue(PresenceMonitor.joinedUnknown(snapshot("AA:AA"), null).isEmpty())
  }

  @Test
  fun `new unknown device is reported`() {
    val joined = PresenceMonitor.joinedUnknown(snapshot("AA:AA", "BB:BB"), setOf("AA:AA"))

    assertEquals(listOf("BB:BB"), joined.map { it.mac })
  }

  @Test
  fun `already seen devices are not reported again`() {
    assertTrue(PresenceMonitor.joinedUnknown(snapshot("AA:AA"), setOf("AA:AA")).isEmpty())
  }
}
