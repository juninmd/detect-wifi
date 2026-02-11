package com.example.presencedetector.services

import java.util.LinkedList
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages in-memory signal history for real-time graphing. Singleton to be accessed by UI and
 * Service.
 */
object SignalHistoryManager {
  // Map<BSSID, List<Pair<Timestamp, SignalLevel>>>
  private val history = ConcurrentHashMap<String, LinkedList<Pair<Long, Int>>>()

  private const val MAX_HISTORY_POINTS =
    60 // Keep last 60 points (approx 3-5 mins depending on scan rate)

  fun addPoint(bssid: String, level: Int) {
    val list = history.getOrPut(bssid) { LinkedList() }
    synchronized(list) {
      list.add(System.currentTimeMillis() to level)
      if (list.size > MAX_HISTORY_POINTS) {
        list.removeFirst()
      }
    }
  }

  fun getHistory(bssid: String): List<Pair<Long, Int>> {
    return history[bssid]?.toList() ?: emptyList()
  }

  fun clear() {
    history.clear()
  }
}
