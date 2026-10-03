package com.example.presencedetector.router.tplink

import com.example.presencedetector.router.HttpTransport
import com.example.presencedetector.router.RouterException
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import javax.crypto.Cipher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Stub that behaves like the router: same RSA/AES handshake, decrypting what the client sends. */
private class FakeRouter(private val password: String, private val username: String = "admin") :
  HttpTransport {
  private val crypto = TpLinkCrypto()
  private val pwdPair = pair(1024)
  private val authPair = pair(512)
  val seq = 1000L
  val calls = mutableListOf<String>()
  var loggedOut = false
  private var key = ""
  private var iv = ""

  private fun pair(bits: Int) =
    KeyPairGenerator.getInstance("RSA").apply { initialize(bits) }.generateKeyPair()

  private fun keys(p: java.security.KeyPair): String {
    val pub = p.public as RSAPublicKey
    return """["${pub.modulus.toString(16)}","${pub.publicExponent.toString(16)}"]"""
  }

  private fun rsaDecrypt(hex: String, p: java.security.KeyPair, keyBytes: Int): String {
    val cipher =
      Cipher.getInstance("RSA/ECB/PKCS1Padding").apply {
        init(Cipher.DECRYPT_MODE, p.private as RSAPrivateKey)
      }
    return hex.chunked(keyBytes * 2).joinToString("") { b ->
      String(cipher.doFinal(b.chunked(2).map { it.toInt(16).toByte() }.toByteArray()))
    }
  }

  private fun reply(json: String) = """{"data":"${crypto.aesEncrypt(json, key, iv)}"}"""

  override fun postForm(url: String, form: Map<String, String>): String {
    calls += url.substringAfter("luci/")
    return when {
      url.endsWith("/login?form=keys") ->
        """{"success":true,"data":{"password":${keys(pwdPair)}}}"""
      url.endsWith("/login?form=auth") ->
        """{"success":true,"data":{"key":${keys(authPair)},"seq":$seq}}"""
      url.endsWith("/login?form=login") -> login(form)
      else -> authenticated(url, form)
    }
  }

  private fun signature(form: Map<String, String>) =
    rsaDecrypt(form.getValue("sign"), authPair, 64).split("&").associate {
      it.substringBefore("=") to it.substringAfter("=")
    }

  private fun login(form: Map<String, String>): String {
    val sign = signature(form)
    key = sign.getValue("k")
    iv = sign.getValue("i")
    check(sign.getValue("s").toLong() == seq + form.getValue("data").length)
    val body = crypto.aesDecrypt(form.getValue("data"), key, iv)
    val sent = rsaDecrypt(body.substringAfter("password=").substringBefore("&"), pwdPair, 128)
    val sameIdentity = sign.getValue("h") == crypto.md5Hex(username + password)
    return if (sent == password && sameIdentity) reply("""{"success":true,"data":{"stok":"TOK"}}""")
    else reply("""{"success":false,"errorcode":"login failed"}""")
  }

  private fun authenticated(url: String, form: Map<String, String>): String {
    check(url.contains(";stok=TOK/")) { "missing session token" }
    check(crypto.aesDecrypt(form.getValue("data"), key, iv).startsWith("operation="))
    if (url.contains("form=logout")) {
      loggedOut = true
      return reply("""{"success":true}""")
    }
    return reply(
      """{"success":true,"data":{"wireless_2g_ssid":"Casa","wireless_2g_enable":"on",
      "access_devices_wireless_host":[{"macaddr":"AA:BB:CC:DD:EE:02","ipaddr":"192.168.0.11",
      "hostname":"Celular","wire_type":"5G"}]}}"""
    )
  }
}

class TpLinkClientTest {
  private fun client(router: FakeRouter, password: String = "secret") =
    TpLinkClient("192.168.0.1", "admin", password, router)

  @Test
  fun `completes the encrypted handshake and returns the snapshot`() {
    val router = FakeRouter("secret")
    val snapshot = client(router).fetchSnapshot()
    assertEquals(listOf("AA:BB:CC:DD:EE:02"), snapshot.devices.map { it.mac })
    assertEquals("Casa", snapshot.networks.single().ssid)
    assertEquals("login?form=keys", router.calls.first().substringAfter("stok=/"))
  }

  @Test
  fun `always logs out so the owner is not locked out of the web ui`() {
    val router = FakeRouter("secret")
    client(router).fetchSnapshot()
    assertTrue(router.loggedOut)
  }

  @Test
  fun `wrong password raises RouterException`() {
    try {
      client(FakeRouter("secret"), password = "wrong").fetchSnapshot()
      fail("expected RouterException")
    } catch (e: RouterException) {
      assertTrue(e.message!!.contains("login failed"))
    }
  }
}
