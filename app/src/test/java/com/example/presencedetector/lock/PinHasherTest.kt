package com.example.presencedetector.lock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {
  @Test
  fun `verify accepts the original pin`() {
    assertTrue(PinHasher.verify("4821", PinHasher.hash("4821")))
  }

  @Test
  fun `verify rejects a different pin`() {
    assertFalse(PinHasher.verify("4822", PinHasher.hash("4821")))
  }

  @Test
  fun `same pin gets a different salt and hash each time`() {
    val a = PinHasher.hash("1234")
    val b = PinHasher.hash("1234")
    assertNotEquals(a.salt, b.salt)
    assertNotEquals(a.hash, b.hash)
  }

  @Test
  fun `hash never contains the pin`() {
    assertEquals(false, PinHasher.hash("987654").hash.contains("987654"))
  }
}
