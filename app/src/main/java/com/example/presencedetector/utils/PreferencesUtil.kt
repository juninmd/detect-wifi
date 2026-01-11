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

        // Telegram Settings
        private const val KEY_TELEGRAM_ENABLED = "telegram_enabled"
        private const val KEY_TELEGRAM_TOKEN = "telegram_token"
        private const val KEY_TELEGRAM_CHAT_ID = "telegram_chat_id"

        // Security Settings
        private const val KEY_SECURITY_ALERT_ENABLED = "security_alert_enabled"
        private const val KEY_SECURITY_SOUND_ENABLED = "security_sound_enabled"
        private const val KEY_SECURITY_START_TIME = "security_start_time" // HH:mm
        private const val KEY_SECURITY_END_TIME = "security_end_time"     // HH:mm

        private const val PREFIX_HISTORY = "history_"
        private const val PREFIX_NICKNAME = "nickname_"
        private const val PREFIX_CATEGORY = "category_"
        private const val PREFIX_NOTIFY_ARRIVAL = "notify_arrival_"
        private const val PREFIX_NOTIFY_DEPARTURE = "notify_departure_"
        private const val PREFIX_CRITICAL_ALERT = "critical_alert_"
        private const val PREFIX_TELEGRAM_ALERT = "telegram_alert_"
        private const val KEY_ALL_BSSIDS = "all_bssids"
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

    fun setCriticalAlertEnabled(bssid: String, enabled: Boolean) {
        preferences.edit().putBoolean(PREFIX_CRITICAL_ALERT + bssid, enabled).apply()
    }

    fun isCriticalAlertEnabled(bssid: String): Boolean {
        return preferences.getBoolean(PREFIX_CRITICAL_ALERT + bssid, false)
    }

    fun setTelegramAlertEnabled(bssid: String, enabled: Boolean) {
        preferences.edit().putBoolean(PREFIX_TELEGRAM_ALERT + bssid, enabled).apply()
    }

    fun isTelegramAlertEnabled(bssid: String): Boolean {
        return preferences.getBoolean(PREFIX_TELEGRAM_ALERT + bssid, false)
    }

    // Telegram Getters/Setters
    fun setTelegramEnabled(enabled: Boolean) = preferences.edit().putBoolean(KEY_TELEGRAM_ENABLED, enabled).apply()
    fun isTelegramEnabled() = preferences.getBoolean(KEY_TELEGRAM_ENABLED, false)

    fun setTelegramToken(token: String) = preferences.edit().putString(KEY_TELEGRAM_TOKEN, token).apply()
    fun getTelegramToken(): String? = preferences.getString(KEY_TELEGRAM_TOKEN, null)

    fun setTelegramChatId(chatId: String) = preferences.edit().putString(KEY_TELEGRAM_CHAT_ID, chatId).apply()
    fun getTelegramChatId(): String? = preferences.getString(KEY_TELEGRAM_CHAT_ID, null)

    // Security Getters/Setters
    fun setSecurityAlertEnabled(enabled: Boolean) = preferences.edit().putBoolean(KEY_SECURITY_ALERT_ENABLED, enabled).apply()
    fun isSecurityAlertEnabled() = preferences.getBoolean(KEY_SECURITY_ALERT_ENABLED, false)

    fun setSecuritySoundEnabled(enabled: Boolean) = preferences.edit().putBoolean(KEY_SECURITY_SOUND_ENABLED, enabled).apply()
    fun isSecuritySoundEnabled() = preferences.getBoolean(KEY_SECURITY_SOUND_ENABLED, false)

    fun setSecuritySchedule(start: String, end: String) {
        preferences.edit()
            .putString(KEY_SECURITY_START_TIME, start)
            .putString(KEY_SECURITY_END_TIME, end)
            .apply()
    }

    fun getSecuritySchedule(): Pair<String, String> {
        val start = preferences.getString(KEY_SECURITY_START_TIME, "22:00") ?: "22:00"
        val end = preferences.getString(KEY_SECURITY_END_TIME, "06:00") ?: "06:00"
        return Pair(start, end)
    }

    fun isCurrentTimeInSecuritySchedule(): Boolean {
        val (startStr, endStr) = getSecuritySchedule()
        val now = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        // Simple string comparison works for HH:mm 24h format
        // Case 1: 22:00 to 06:00 (Overnight)
        if (startStr > endStr) {
            return now >= startStr || now <= endStr
        }
        // Case 2: 09:00 to 17:00 (Daytime)
        return now in startStr..endStr
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
        val historyKey = PREFIX_HISTORY + bssid
        val history = preferences.getStringSet(historyKey, mutableSetOf()) ?: mutableSetOf()
        
        if (!history.contains(today)) {
            val newHistory = history.toMutableSet()
            newHistory.add(today)

            // Update all BSSIDs list
            val allBssids = preferences.getStringSet(KEY_ALL_BSSIDS, mutableSetOf()) ?: mutableSetOf()
            val newAllBssids = allBssids.toMutableSet()
            newAllBssids.add(bssid)

            preferences.edit()
                .putStringSet(historyKey, newHistory)
                .putStringSet(KEY_ALL_BSSIDS, newAllBssids)
                .apply()
        }
    }

    fun getDetectionHistory(bssid: String): List<String> {
        return preferences.getStringSet(PREFIX_HISTORY + bssid, emptySet())?.toList()?.sortedDescending() ?: emptyList()
    }

    fun getAllTrackedBssids(): List<String> {
        return preferences.getStringSet(KEY_ALL_BSSIDS, emptySet())?.toList() ?: emptyList()
    }

    fun getDetectionHistoryCount(bssid: String): Int {
        return preferences.getStringSet(PREFIX_HISTORY + bssid, emptySet())?.size ?: 0
    }

    fun clear() {
        preferences.edit().clear().apply()
    }
}
