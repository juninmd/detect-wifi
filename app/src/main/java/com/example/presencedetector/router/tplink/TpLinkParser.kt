package com.example.presencedetector.router.tplink

import com.example.presencedetector.router.Connection
import com.example.presencedetector.router.Macs
import com.example.presencedetector.router.RouterClientDevice
import com.example.presencedetector.router.RouterException
import com.example.presencedetector.router.RouterNetwork
import com.example.presencedetector.router.RouterSnapshot
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.util.Base64

/** Turns TP-Link JSON replies into router models. Tolerant: unknown fields are ignored. */
object TpLinkParser {
  data class RsaKey(val modulus: String, val exponent: String)

  data class AuthInfo(val key: RsaKey, val seq: Long)

  private val BANDS = mapOf("2g" to "2.4 GHz", "5g" to "5 GHz", "6g" to "6 GHz")
  private val NETWORK_KEY = Regex("^(wireless|guest)_(2g|5g|6g)_ssid$")
  private val WIRED_LISTS = listOf("access_devices_wired")
  private val WIRELESS_LISTS =
    listOf("access_devices_wireless_host", "access_devices_wireless_guest")

  fun passwordKey(json: String): RsaKey = rsaKey(data(json).getAsJsonArray("password"))

  fun authInfo(json: String): AuthInfo {
    val d = data(json)
    return AuthInfo(rsaKey(d.getAsJsonArray("key")), d.get("seq").asLong)
  }

  fun stok(json: String): String =
    data(json).get("stok")?.asString ?: throw RouterException("Login recusado pelo roteador")

  fun snapshot(json: String, nowMs: Long): RouterSnapshot {
    val d = data(json)
    val devices =
      WIRED_LISTS.flatMap { devices(d, it, Connection.WIRED) } +
        WIRELESS_LISTS.flatMap { devices(d, it, null) }
    return RouterSnapshot(devices.distinctBy { it.mac }, networks(d), nowMs)
  }

  private fun data(json: String): JsonObject {
    val root = JsonParser.parseString(json).asJsonObject
    if (root.get("success")?.asBoolean == false) {
      throw RouterException(
        "Roteador respondeu com erro: ${root.get("errorcode") ?: "desconhecido"}"
      )
    }
    return root.getAsJsonObject("data") ?: throw RouterException("Resposta sem dados")
  }

  private fun rsaKey(array: JsonArray) = RsaKey(array[0].asString, array[1].asString)

  private fun devices(d: JsonObject, field: String, fixed: Connection?): List<RouterClientDevice> =
    (d.get(field) as? JsonArray)?.toList().orEmpty().mapNotNull { item ->
      val o = item as? JsonObject ?: return@mapNotNull null
      val mac = o.str("macaddr") ?: o.str("mac") ?: return@mapNotNull null
      RouterClientDevice(
        mac = Macs.normalize(mac),
        ip = o.str("ipaddr") ?: o.str("ip"),
        name = o.str("hostname")?.let(::decodeName),
        connection = fixed ?: connection(o.str("wire_type") ?: o.str("type")),
      )
    }

  private fun networks(d: JsonObject): List<RouterNetwork> =
    d.keySet().mapNotNull { key ->
      val match = NETWORK_KEY.matchEntire(key) ?: return@mapNotNull null
      val ssid = d.str(key)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
      val (kind, band) = match.destructured
      val enabled = d.str("${kind}_${band}_enable")?.lowercase() != "off"
      RouterNetwork(ssid, BANDS.getValue(band), enabled, kind == "guest")
    }

  private fun connection(raw: String?): Connection =
    when (raw?.lowercase()) {
      "2.4g",
      "2g" -> Connection.WIFI_2G
      "5g" -> Connection.WIFI_5G
      "6g" -> Connection.WIFI_6G
      "guest" -> Connection.GUEST
      "wired" -> Connection.WIRED
      else -> Connection.UNKNOWN
    }

  /** Some firmwares send host names base64-encoded; keep the raw text when decoding looks wrong. */
  internal fun decodeName(raw: String): String {
    val decoded = runCatching {
      String(Base64.getDecoder().decode(raw), Charsets.UTF_8)
    }.getOrNull()
    val readable = decoded != null && decoded.none { it.isISOControl() || it == '�' }
    return if (readable && raw.length % 4 == 0 && decoded!!.isNotBlank()) decoded else raw
  }

  private fun JsonObject.str(key: String): String? =
    get(key)?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.asString
}
