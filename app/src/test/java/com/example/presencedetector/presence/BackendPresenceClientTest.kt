package com.example.presencedetector.presence

import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class BackendPresenceClientTest {
  private val http: HttpGetter = mock()

  @Test
  fun `queries presence with api key and window`() {
    whenever(http.get(any(), any())).thenReturn("""{"windowSeconds":60,"anonymousCount":2}""")

    val snapshot = BackendPresenceClient("192.168.1.10:3000/", "secret", http).fetch(60)

    verify(http)
      .get(eq("http://192.168.1.10:3000/presence?window=60"), eq(mapOf("x-api-key" to "secret")))
    assertEquals(2, snapshot.anonymousCount)
  }

  @Test
  fun `keeps an explicit https scheme`() {
    whenever(http.get(any(), any())).thenReturn("{}")

    BackendPresenceClient(" https://home.example ", "k", http).fetch(300)

    verify(http).get(eq("https://home.example/presence?window=300"), any())
  }

  @Test(expected = PresenceException::class)
  fun `propagates transport failures`() {
    whenever(http.get(any(), any())).thenAnswer { throw PresenceException("down") }

    BackendPresenceClient("http://x", "k", http).fetch(60)
  }
}
