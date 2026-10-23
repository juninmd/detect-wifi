package com.example.presencedetector.presence

import com.example.presencedetector.router.Macs
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.time.Instant

/** Turns the backend `GET /presence` JSON into [PresenceSnapshot]. */
object PresenceParser {
  fun parse(json: String): PresenceSnapshot =
    try {
      val root = JsonParser.parseString(json).asJsonObject
      PresenceSnapshot(
        windowSeconds = root.get("windowSeconds")?.asInt ?: 0,
        known = devices(root, "known"),
        unknown = devices(root, "unknown"),
        anonymousCount = root.get("anonymousCount")?.asInt ?: 0,
      )
    } catch (e: RuntimeException) {
      throw PresenceException("Resposta inválida do backend de presença", e)
    }

  private fun devices(root: JsonObject, key: String): List<PresentDevice> =
    root.getAsJsonArray(key)?.map { toDevice(it.asJsonObject) }.orEmpty()

  private fun toDevice(o: JsonObject) =
    PresentDevice(
      mac = Macs.normalize(o.get("mac").asString),
      rssi = o.get("rssi").asInt,
      lastSeenMs = o.get("lastSeen").stringOrNull()?.let(::parseInstant) ?: 0L,
      label = o.get("vendor").stringOrNull(),
    )

  private fun JsonElement?.stringOrNull(): String? =
    if (this == null || isJsonNull) null else asString

  private fun parseInstant(raw: String): Long? =
    runCatching { Instant.parse(raw).toEpochMilli() }.getOrNull()
}
