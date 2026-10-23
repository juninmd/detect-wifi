package com.example.presencedetector.presence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class PresenceParserTest {
  private val json =
    """
    {"windowSeconds":300,
     "known":[{"mac":"aa-bb-cc-dd-ee-ff","rssi":-50,
       "lastSeen":"2026-01-01T00:00:00.000Z","vendor":"Ana"}],
     "unknown":[{"mac":"00:11:22:33:44:55","rssi":-80,"lastSeen":"bad"}],
     "anonymousCount":3}
    """
      .trimIndent()

  @Test
  fun `parses known unknown and anonymous devices`() {
    val snapshot = PresenceParser.parse(json)

    assertEquals(300, snapshot.windowSeconds)
    assertEquals(3, snapshot.anonymousCount)
    val ana = snapshot.known.single()
    assertEquals("AA:BB:CC:DD:EE:FF", ana.mac)
    assertEquals(-50, ana.rssi)
    assertEquals(1767225600000L, ana.lastSeenMs)
    assertEquals("Ana", ana.displayName)
  }

  @Test
  fun `invalid timestamp and missing vendor fall back to defaults`() {
    val stranger = PresenceParser.parse(json).unknown.single()

    assertEquals(0L, stranger.lastSeenMs)
    assertNull(stranger.label)
    assertEquals("Dispositivo 44:55", stranger.displayName)
  }

  @Test
  fun `missing fields default to empty values`() {
    val snapshot = PresenceParser.parse("{}")

    assertEquals(0, snapshot.windowSeconds)
    assertEquals(0, snapshot.anonymousCount)
    assertEquals(emptyList<PresentDevice>(), snapshot.known)
  }

  @Test
  fun `malformed json becomes a PresenceException`() {
    assertThrows(PresenceException::class.java) { PresenceParser.parse("not json") }
    assertThrows(PresenceException::class.java) { PresenceParser.parse("[]") }
    assertThrows(PresenceException::class.java) { PresenceParser.parse("""{"known":[{}]}""") }
  }
}
