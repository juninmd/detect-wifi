package com.example.presencedetector.router

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouterMonitorTest {
  private fun device(mac: String) = RouterClientDevice(mac, "192.168.0.2", null, Connection.WIFI_5G)

  private fun snapshot(vararg macs: String) = RouterSnapshot(macs.map(::device), emptyList(), 0L)

  @Test
  fun `first scan has no joined devices but lists the unknown ones`() {
    val diff = RouterMonitor.evaluate(snapshot("AA:AA", "BB:BB"), setOf("AA:AA"), null)
    assertEquals(listOf("BB:BB"), diff.unknown.map { it.mac })
    assertTrue(diff.joinedUnknown.isEmpty())
  }

  @Test
  fun `new untrusted device is reported as joined`() {
    val diff = RouterMonitor.evaluate(snapshot("AA:AA", "CC:CC"), setOf("AA:AA"), setOf("AA:AA"))
    assertEquals(listOf("CC:CC"), diff.joinedUnknown.map { it.mac })
  }

  @Test
  fun `trusted newcomer is not an alert`() {
    val diff =
      RouterMonitor.evaluate(snapshot("AA:AA", "CC:CC"), setOf("AA:AA", "CC:CC"), setOf("AA:AA"))
    assertTrue(diff.joinedUnknown.isEmpty())
  }

  @Test
  fun `devices that disappeared are listed as left`() {
    val diff = RouterMonitor.evaluate(snapshot("AA:AA"), setOf("AA:AA"), setOf("AA:AA", "BB:BB"))
    assertEquals(setOf("BB:BB"), diff.left)
  }

  @Test
  fun `macs are normalised to upper case with colons`() {
    assertEquals("AA:BB:CC:DD:EE:FF", Macs.normalize("aa-bb-cc-dd-ee-ff"))
  }
}
