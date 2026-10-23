package com.example.presencedetector.presence

/** A device seen by the passive Wi-Fi sensor (probe requests), not necessarily connected. */
data class PresentDevice(val mac: String, val rssi: Int, val lastSeenMs: Long, val label: String?) {
  val displayName: String
    get() = label?.takeIf { it.isNotBlank() } ?: "Dispositivo ${mac.takeLast(5)}"
}

/** Randomized MACs cannot be identified, so the backend only reports how many there are. */
data class PresenceSnapshot(
  val windowSeconds: Int,
  val known: List<PresentDevice>,
  val unknown: List<PresentDevice>,
  val anonymousCount: Int,
)

/** Reads who is nearby from the presence backend. Blocking: call from a background thread. */
interface PresenceClient {
  fun fetch(windowSeconds: Int): PresenceSnapshot
}

class PresenceException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Finds strangers that appeared since the previous poll. */
object PresenceMonitor {
  fun joinedUnknown(snapshot: PresenceSnapshot, previous: Set<String>?): List<PresentDevice> =
    if (previous == null) emptyList() else snapshot.unknown.filter { it.mac !in previous }
}
