package com.example.presencedetector.intruder

import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IntruderReportTest {
  private val point = GeoPoint(-23.55052, -46.633308, 12.4f)

  @Test
  fun `mapsUrl uses dot decimals regardless of locale`() {
    assertEquals("https://maps.google.com/?q=-23.550520,-46.633308", IntruderReport.mapsUrl(point))
  }

  @Test
  fun `caption has reason time and location`() {
    val text = IntruderReport.caption("Senha errada", 0L, point, ZoneOffset.UTC)
    assertEquals(
      "🚨 Senha errada\n🕒 01/01/1970 00:00:00\n📍 https://maps.google.com/?q=-23.550520,-46.633308 (±12 m)",
      text,
    )
  }

  @Test
  fun `caption says when location is unavailable`() {
    val text = IntruderReport.caption("Senha errada", 0L, null, ZoneOffset.UTC)
    assertTrue(text.endsWith("📍 Localização indisponível"))
  }
}
