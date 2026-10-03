package com.example.presencedetector.router

/** Compares router snapshots against the trusted list to find strangers on the home network. */
object RouterMonitor {
  data class Diff(
    val unknown: List<RouterClientDevice>,
    val joinedUnknown: List<RouterClientDevice>,
    val left: Set<String>,
  )

  /**
   * @param previous MACs seen in the last scan, or null on the very first scan (no baseline yet).
   */
  fun evaluate(snapshot: RouterSnapshot, trusted: Set<String>, previous: Set<String>?): Diff {
    val current = snapshot.devices.map { it.mac }.toSet()
    val unknown = snapshot.devices.filter { it.mac !in trusted }
    val joined = if (previous == null) emptyList() else unknown.filter { it.mac !in previous }
    return Diff(unknown, joined, previous.orEmpty() - current)
  }
}
