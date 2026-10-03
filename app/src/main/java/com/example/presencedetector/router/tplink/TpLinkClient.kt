package com.example.presencedetector.router.tplink

import com.example.presencedetector.router.HttpTransport
import com.example.presencedetector.router.RouterClient
import com.example.presencedetector.router.RouterException
import com.example.presencedetector.router.RouterSnapshot
import com.google.gson.JsonParser

/**
 * Client for the encrypted web API of TP-Link Archer/Deco style firmwares: RSA protects the
 * password and request signature, AES-CBC protects every payload.
 */
class TpLinkClient(
  host: String,
  private val username: String,
  private val password: String,
  private val http: HttpTransport,
  private val crypto: TpLinkCrypto = TpLinkCrypto(),
  private val clock: () -> Long = System::currentTimeMillis,
) : RouterClient {
  private val base = "http://$host/cgi-bin/luci/;stok="
  private val aesKey = crypto.newDigits()
  private val aesIv = crypto.newDigits()
  private val identityHash = crypto.md5Hex(username + password)
  private lateinit var auth: TpLinkParser.AuthInfo
  private var stok = ""

  override fun fetchSnapshot(): RouterSnapshot {
    login()
    try {
      return TpLinkParser.snapshot(call("admin/status?form=all", "operation=read"), clock())
    } finally {
      logout()
    }
  }

  private fun login() {
    val pwdKey = TpLinkParser.passwordKey(plain("login?form=keys"))
    auth = TpLinkParser.authInfo(plain("login?form=auth"))
    val encryptedPwd = crypto.rsaEncryptHex(password, pwdKey.modulus, pwdKey.exponent)
    val reply = send("$base/login?form=login", "password=$encryptedPwd&operation=login", true)
    stok = TpLinkParser.stok(reply)
  }

  /** The router allows a single admin session, so always leave it to not lock the owner out. */
  private fun logout() {
    runCatching { call("admin/system?form=logout", "operation=write") }
  }

  private fun plain(path: String): String =
    http.postForm("$base/$path", mapOf("operation" to "read"))

  private fun call(path: String, body: String): String = send("$base$stok/$path", body, false)

  private fun send(url: String, body: String, isLogin: Boolean): String {
    val payload = crypto.aesEncrypt(body, aesKey, aesIv)
    val form = mapOf("sign" to sign(payload.length, isLogin), "data" to payload)
    val reply = JsonParser.parseString(http.postForm(url, form)).asJsonObject
    val encrypted = reply.get("data")?.takeIf { it.isJsonPrimitive }?.asString
    return encrypted?.let { crypto.aesDecrypt(it, aesKey, aesIv) }
      ?: throw RouterException("Resposta inesperada do roteador")
  }

  private fun sign(dataLength: Int, isLogin: Boolean): String {
    val seq = auth.seq + dataLength
    val text =
      if (isLogin) "k=$aesKey&i=$aesIv&h=$identityHash&s=$seq" else "h=$identityHash&s=$seq"
    return crypto.rsaEncryptHex(text, auth.key.modulus, auth.key.exponent)
  }
}
