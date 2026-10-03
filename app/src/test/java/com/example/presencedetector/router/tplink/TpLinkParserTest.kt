package com.example.presencedetector.router.tplink

import com.example.presencedetector.router.Connection
import com.example.presencedetector.router.RouterException
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TpLinkParserTest {
  private val b64 = { s: String -> Base64.getEncoder().encodeToString(s.toByteArray()) }

  private val status =
    """
    {"success":true,"data":{
      "wireless_2g_ssid":"Casa","wireless_2g_enable":"on",
      "wireless_5g_ssid":"Casa_5G","wireless_5g_enable":"off",
      "guest_2g_ssid":"Visitas","guest_2g_enable":"on",
      "access_devices_wired":[{"macaddr":"aa-bb-cc-dd-ee-01","ipaddr":"192.168.0.10","hostname":"PC-Sala"}],
      "access_devices_wireless_host":[
        {"macaddr":"AA:BB:CC:DD:EE:02","ipaddr":"192.168.0.11","hostname":"${b64("iPhone da Ana")}","wire_type":"5G"},
        {"macaddr":"AA:BB:CC:DD:EE:03","ipaddr":"192.168.0.12","hostname":"","wire_type":"2.4G"}
      ]}}
    """

  @Test
  fun `parses wired and wireless clients`() {
    val devices = TpLinkParser.snapshot(status, 5L).devices
    assertEquals(3, devices.size)
    assertEquals("AA:BB:CC:DD:EE:01", devices[0].mac)
    assertEquals(Connection.WIRED, devices[0].connection)
    assertEquals("iPhone da Ana", devices[1].name)
    assertEquals(Connection.WIFI_5G, devices[1].connection)
    assertEquals(Connection.WIFI_2G, devices[2].connection)
    assertEquals("Dispositivo EE:03", devices[2].displayName)
  }

  @Test
  fun `parses router networks with state`() {
    val networks = TpLinkParser.snapshot(status, 0L).networks.associateBy { it.ssid }
    assertTrue(networks.getValue("Casa").enabled)
    assertEquals("2.4 GHz", networks.getValue("Casa").band)
    assertFalse(networks.getValue("Casa_5G").enabled)
    assertTrue(networks.getValue("Visitas").guest)
  }

  @Test
  fun `plain host names are kept as they are`() {
    assertEquals("Galaxy-S23", TpLinkParser.decodeName("Galaxy-S23"))
    assertEquals("test", TpLinkParser.decodeName("test"))
  }

  @Test
  fun `parses key exchange replies`() {
    assertEquals(
      "AB",
      TpLinkParser.passwordKey("""{"success":true,"data":{"password":["AB","10001"]}}""").modulus,
    )
    val auth = TpLinkParser.authInfo("""{"success":true,"data":{"key":["CD","10001"],"seq":42}}""")
    assertEquals(42L, auth.seq)
    assertEquals("CD", auth.key.modulus)
  }

  @Test
  fun `error replies raise RouterException`() {
    try {
      TpLinkParser.stok("""{"success":false,"errorcode":"login failed"}""")
      fail("expected RouterException")
    } catch (e: RouterException) {
      assertTrue(e.message!!.contains("login failed"))
    }
  }
}
