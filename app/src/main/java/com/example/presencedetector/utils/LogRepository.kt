package com.example.presencedetector.utils

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Repository for managing logs (both file-based and shared preferences-based). */
class LogRepository(context: Context) {

    private val preferences: SharedPreferences = context.getSharedPreferences(
        "presence_detector_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        const val KEY_ALL_BSSIDS = "all_bssids"
        const val KEY_SYSTEM_LOGS = "system_logs"

        object Prefixes {
            const val HISTORY = "history_"
            const val EVENT_LOGS = "event_logs_"
        }
    }

    // Helper methods
    private fun putString(key: String, value: String?) = preferences.edit().putString(key, value).apply()
    private fun getString(key: String, default: String? = null) = preferences.getString(key, default)
    private fun putStringSet(key: String, value: Set<String>) = preferences.edit().putStringSet(key, value).apply()
    private fun getStringSet(key: String, default: Set<String>? = null) = preferences.getStringSet(key, default)

    /**
     * Log precise arrival/departure events with time.
     */
    fun logEvent(bssid: String, eventType: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val logEntry = "[$timestamp] $eventType"

        val logs = getStringSet(Prefixes.EVENT_LOGS + bssid, mutableSetOf()) ?: mutableSetOf()
        val newLogs = logs.toMutableSet()
        newLogs.add(logEntry)

        // Ensure BSSID is in the master list
        val allBssids = getStringSet(KEY_ALL_BSSIDS, mutableSetOf()) ?: mutableSetOf()
        val newAllBssids = allBssids.toMutableSet()
        newAllBssids.add(bssid)

        // Use batch edit manually here as we are updating multiple
        preferences.edit()
            .putStringSet(Prefixes.EVENT_LOGS + bssid, newLogs)
            .putStringSet(KEY_ALL_BSSIDS, newAllBssids)
            .apply()

        // Also keep the daily history count logic
        trackDetection(bssid)
    }

    fun getEventLogs(bssid: String): List<String> {
        return getStringSet(Prefixes.EVENT_LOGS + bssid, emptySet())?.toList()?.sortedDescending() ?: emptyList()
    }

    fun trackDetection(bssid: String) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val historyKey = Prefixes.HISTORY + bssid
        val history = getStringSet(historyKey, mutableSetOf()) ?: mutableSetOf()
        if (!history.contains(today)) {
            val newHistory = history.toMutableSet()
            newHistory.add(today)
            putStringSet(historyKey, newHistory)
        }
    }

    fun getDetectionHistoryCount(bssid: String): Int {
        return getStringSet(Prefixes.HISTORY + bssid, emptySet())?.size ?: 0
    }

    fun getAllTrackedBssids(): List<String> {
        return getStringSet(KEY_ALL_BSSIDS, emptySet())?.toList() ?: emptyList()
    }

    /** Log general system events (Security, Panic, Errors). */
    fun logSystemEvent(message: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val logEntry = "[$timestamp] $message"

        val logs = getStringSet(KEY_SYSTEM_LOGS, mutableSetOf()) ?: mutableSetOf()
        val newLogs = logs.toMutableSet()
        newLogs.add(logEntry)

        putStringSet(KEY_SYSTEM_LOGS, newLogs)
    }

    fun getSystemLogs(): List<String> {
        return getStringSet(KEY_SYSTEM_LOGS, emptySet())?.toList()?.sortedDescending() ?: emptyList()
    }
}
