package com.example.presencedetector.services

import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages in-memory signal history for real-time graphing. Singleton to be accessed by UI and
 * Service.
 */
object SignalHistoryManager {
  // Map<BSSID, List<Pair<Timestamp, SignalLevel>>>
  // Using ArrayDeque for better performance and memory locality compared to LinkedList
  private val history = ConcurrentHashMap<String, ArrayDeque<Pair<Long, Int>>>()

  private const val MAX_HISTORY_POINTS =
    60 // Keep last 60 points (approx 3-5 mins depending on scan rate)

  fun addPoint(bssid: String, level: Int) {
    val list = history.getOrPut(bssid) { ArrayDeque(MAX_HISTORY_POINTS + 5) }
    synchronized(list) {
      list.add(System.currentTimeMillis() to level)
      if (list.size > MAX_HISTORY_POINTS) {
        list.removeFirst()
      }
    }
  }

  fun getHistory(bssid: String): List<Pair<Long, Int>> {
    // ArrayDeque is not a List, but toList() creates a copy which is safe for external consumption
    val list = history[bssid] ?: return emptyList()
    synchronized(list) {
        return list.toList()
    }
  }

  fun clear() {
    history.clear()
  }
}
