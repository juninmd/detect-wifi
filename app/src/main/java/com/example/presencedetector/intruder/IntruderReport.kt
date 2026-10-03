package com.example.presencedetector.intruder

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class GeoPoint(val latitude: Double, val longitude: Double, val accuracyMeters: Float? = null)

/** Builds the text sent to Telegram together with the intruder photo. */
object IntruderReport {
  private val TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")

  fun mapsUrl(point: GeoPoint): String =
    String.format(
      Locale.US,
      "https://maps.google.com/?q=%.6f,%.6f",
      point.latitude,
      point.longitude,
    )

  fun caption(
    reason: String,
    timeMs: Long,
    point: GeoPoint?,
    zone: ZoneId = ZoneId.systemDefault(),
  ): String {
    val time = TIME_FORMAT.format(Instant.ofEpochMilli(timeMs).atZone(zone))
    val where = point?.let { "📍 ${mapsUrl(it)}${accuracy(it)}" } ?: "📍 Localização indisponível"
    return "🚨 $reason\n🕒 $time\n$where"
  }

  private fun accuracy(point: GeoPoint) =
    point.accuracyMeters?.let { String.format(Locale.US, " (±%.0f m)", it) } ?: ""
}
