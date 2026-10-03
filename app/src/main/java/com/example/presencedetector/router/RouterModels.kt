package com.example.presencedetector.router

enum class Connection(val label: String) {
  WIRED("Cabo"),
  WIFI_2G("2.4 GHz"),
  WIFI_5G("5 GHz"),
  WIFI_6G("6 GHz"),
  GUEST("Convidados"),
  UNKNOWN("?"),
}

data class RouterClientDevice(
  val mac: String,
  val ip: String?,
  val name: String?,
  val connection: Connection,
) {
  val displayName: String
    get() = name?.takeIf { it.isNotBlank() } ?: "Dispositivo ${mac.takeLast(5)}"
}

data class RouterNetwork(
  val ssid: String,
  val band: String,
  val enabled: Boolean,
  val guest: Boolean,
)

data class RouterSnapshot(
  val devices: List<RouterClientDevice>,
  val networks: List<RouterNetwork>,
  val takenAtMs: Long,
)

/** Reads the current state of a home router. Blocking: call from a background thread. */
interface RouterClient {
  fun fetchSnapshot(): RouterSnapshot
}

class RouterException(message: String, cause: Throwable? = null) : Exception(message, cause)

object Macs {
  fun normalize(raw: String): String =
    raw.trim().uppercase().replace('-', ':').takeIf {
      it.matches(Regex("([0-9A-F]{2}:){5}[0-9A-F]{2}"))
    } ?: raw.trim().uppercase()
}
