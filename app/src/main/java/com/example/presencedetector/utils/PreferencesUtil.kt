package com.example.presencedetector.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.presencedetector.model.DeviceCategory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility class for managing application preferences.
 */
class PreferencesUtil(context: Context) {
    companion object {
        private const val PREF_NAME = "presence_detector_prefs"
        private const val KEY_LAST_DETECTION = "last_detection"
        private const val KEY_DETECTION_ENABLED = "detection_enabled"
        private const val KEY_FOREGROUND_SERVICE = "foreground_service"
        private const val KEY_NOTIFY_ON_PRESENCE = "notify_on_presence"
        private const val PREFIX_HISTORY = "history_"
        private const val PREFIX_NICKNAME = "nickname_"
        private const val PREFIX_CATEGORY = "category_"
        private const val PREFIX_NOTIFY_ARRIVAL = "notify_arrival_"
        private const val PREFIX_NOTIFY_DEPARTURE = "notify_departure_"
    }

    private val preferences: SharedPreferences = context.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE
    )

    fun setDetectionEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_DETECTION_ENABLED, enabled).apply()
    }

    fun isDetectionEnabled(): Boolean {
        return preferences.getBoolean(KEY_DETECTION_ENABLED, false)
    }

    fun setNotifyOnPresence(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_NOTIFY_ON_PRESENCE, enabled).apply()
    }

    fun shouldNotifyOnPresence(): Boolean {
        return preferences.getBoolean(KEY_NOTIFY_ON_PRESENCE, true)
    }

    fun setNotifyArrival(bssid: String, notify: Boolean) {
        preferences.edit().putBoolean(PREFIX_NOTIFY_ARRIVAL + bssid, notify).apply()
    }

    fun shouldNotifyArrival(bssid: String): Boolean {
        return preferences.getBoolean(PREFIX_NOTIFY_ARRIVAL + bssid, false)
    }

    fun setNotifyDeparture(bssid: String, notify: Boolean) {
        preferences.edit().putBoolean(PREFIX_NOTIFY_DEPARTURE + bssid, notify).apply()
    }

    fun shouldNotifyDeparture(bssid: String): Boolean {
        return preferences.getBoolean(PREFIX_NOTIFY_DEPARTURE + bssid, false)
    }

    fun setForegroundServiceRunning(running: Boolean) {
        preferences.edit().putBoolean(KEY_FOREGROUND_SERVICE, running).apply()
    }

    fun isForegroundServiceRunning(): Boolean {
        return preferences.getBoolean(KEY_FOREGROUND_SERVICE, false)
    }

    fun setLastDetection(timestamp: Long) {
        preferences.edit().putLong(KEY_LAST_DETECTION, timestamp).apply()
    }

    fun getLastDetection(): Long {
        return preferences.getLong(KEY_LAST_DETECTION, 0)
    }

    fun saveNickname(bssid: String, nickname: String) {
        preferences.edit().putString(PREFIX_NICKNAME + bssid, nickname).apply()
    }

    fun getNickname(bssid: String): String? {
        return preferences.getString(PREFIX_NICKNAME + bssid, null)
    }

    fun saveManualCategory(bssid: String, category: DeviceCategory) {
        preferences.edit().putString(PREFIX_CATEGORY + bssid, category.name).apply()
    }

    fun getManualCategory(bssid: String): DeviceCategory? {
        val name = preferences.getString(PREFIX_CATEGORY + bssid, null) ?: return null
        return try {
            DeviceCategory.valueOf(name)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Track detection history. Counts 1x per day.
     */
    fun trackDetection(bssid: String) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val history = preferences.getStringSet(PREFIX_HISTORY + bssid, mutableSetOf()) ?: mutableSetOf()
        
        if (!history.contains(today)) {
            val newHistory = history.toMutableSet()
            newHistory.add(today)
            preferences.edit().putStringSet(PREFIX_HISTORY + bssid, newHistory).apply()
        }
    }

    fun getDetectionHistoryCount(bssid: String): Int {
        return preferences.getStringSet(PREFIX_HISTORY + bssid, emptySet())?.size ?: 0
    }

    fun clear() {
        preferences.edit().clear().apply()
    }
}
