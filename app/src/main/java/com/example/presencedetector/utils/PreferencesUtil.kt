package com.example.presencedetector.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.presencedetector.model.DeviceCategory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility class for managing application preferences and history logs.
 */
class PreferencesUtil(context: Context) {
    companion object {
        private const val PREF_NAME = "presence_detector_prefs"
        private const val KEY_LAST_DETECTION = "last_detection"
        private const val KEY_DETECTION_ENABLED = "detection_enabled"
        private const val KEY_FOREGROUND_SERVICE = "foreground_service"
        private const val KEY_NOTIFY_ON_PRESENCE = "notify_on_presence"
        private const val KEY_NOTIFY_WIFI_ARRIVAL = "notify_wifi_arrival"

        // Telegram Settings
        private const val KEY_TELEGRAM_ENABLED = "telegram_enabled"
        private const val KEY_TELEGRAM_TOKEN = "telegram_token"
        private const val KEY_TELEGRAM_CHAT_ID = "telegram_chat_id"

        // Security Settings
        private const val KEY_SECURITY_ALERT_ENABLED = "security_alert_enabled"
        private const val KEY_SECURITY_SOUND_ENABLED = "security_sound_enabled"
        private const val KEY_SECURITY_START_TIME = "security_start_time"
        private const val KEY_SECURITY_END_TIME = "security_end_time"
        private const val KEY_ANTI_THEFT_ARMED = "anti_theft_armed"
        private const val KEY_ANTI_THEFT_SENSITIVITY = "anti_theft_sensitivity"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_POCKET_MODE_ENABLED = "pocket_mode_enabled"
        private const val KEY_CHARGER_MODE_ENABLED = "charger_mode_enabled"

        private const val PREFIX_HISTORY = "history_"
        private const val PREFIX_NICKNAME = "nickname_"
        private const val PREFIX_CATEGORY = "category_"
        private const val PREFIX_NOTIFY_ARRIVAL = "notify_arrival_"
        private const val PREFIX_NOTIFY_DEPARTURE = "notify_departure_"
        private const val PREFIX_CRITICAL_ALERT = "critical_alert_"
        private const val PREFIX_TELEGRAM_ALERT = "telegram_alert_"
        private const val PREFIX_EVENT_LOGS = "event_logs_"
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

    fun setNotifyWifiArrival(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_NOTIFY_WIFI_ARRIVAL, enabled).apply()
    }

    fun shouldNotifyWifiArrival(): Boolean {
        return preferences.getBoolean(KEY_NOTIFY_WIFI_ARRIVAL, false)
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

    // Telegram
    fun setTelegramEnabled(enabled: Boolean) = preferences.edit().putBoolean(KEY_TELEGRAM_ENABLED, enabled).apply()
    fun isTelegramEnabled() = preferences.getBoolean(KEY_TELEGRAM_ENABLED, false)
    fun setTelegramToken(token: String) = preferences.edit().putString(KEY_TELEGRAM_TOKEN, token).apply()
    fun getTelegramToken(): String? = preferences.getString(KEY_TELEGRAM_TOKEN, null)
    fun setTelegramChatId(chatId: String) = preferences.edit().putString(KEY_TELEGRAM_CHAT_ID, chatId).apply()
    fun getTelegramChatId(): String? = preferences.getString(KEY_TELEGRAM_CHAT_ID, null)

    // Security
    fun setSecurityAlertEnabled(enabled: Boolean) = preferences.edit().putBoolean(KEY_SECURITY_ALERT_ENABLED, enabled).apply()
    fun isSecurityAlertEnabled() = preferences.getBoolean(KEY_SECURITY_ALERT_ENABLED, false)
    fun setSecuritySoundEnabled(enabled: Boolean) = preferences.edit().putBoolean(KEY_SECURITY_SOUND_ENABLED, enabled).apply()
    fun isSecuritySoundEnabled() = preferences.getBoolean(KEY_SECURITY_SOUND_ENABLED, false)
    fun setSecuritySchedule(start: String, end: String) {
        preferences.edit().putString(KEY_SECURITY_START_TIME, start).putString(KEY_SECURITY_END_TIME, end).apply()
    }
    fun getSecuritySchedule(): Pair<String, String> {
        val start = preferences.getString(KEY_SECURITY_START_TIME, "22:00") ?: "22:00"
        val end = preferences.getString(KEY_SECURITY_END_TIME, "06:00") ?: "06:00"
        return Pair(start, end)
    }
    fun isCurrentTimeInSecuritySchedule(): Boolean {
        val (startStr, endStr) = getSecuritySchedule()
        val now = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        return if (startStr > endStr) now >= startStr || now <= endStr else now in startStr..endStr
    }

    fun setAntiTheftArmed(armed: Boolean) {
        preferences.edit().putBoolean(KEY_ANTI_THEFT_ARMED, armed).apply()
    }

    fun isAntiTheftArmed(): Boolean {
        return preferences.getBoolean(KEY_ANTI_THEFT_ARMED, false)
    }

    fun setAntiTheftSensitivity(sensitivity: Float) {
        preferences.edit().putFloat(KEY_ANTI_THEFT_SENSITIVITY, sensitivity).apply()
    }

    fun getAntiTheftSensitivity(): Float {
        return preferences.getFloat(KEY_ANTI_THEFT_SENSITIVITY, 1.5f)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun isBiometricEnabled(): Boolean {
        return preferences.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun setPocketModeEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_POCKET_MODE_ENABLED, enabled).apply()
    }

    fun isPocketModeEnabled(): Boolean {
        return preferences.getBoolean(KEY_POCKET_MODE_ENABLED, false)
    }

    fun setChargerModeEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_CHARGER_MODE_ENABLED, enabled).apply()
    }

    fun isChargerModeEnabled(): Boolean {
        return preferences.getBoolean(KEY_CHARGER_MODE_ENABLED, false)
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
        return try { DeviceCategory.valueOf(name) } catch (e: Exception) { null }
    }

    /**
     * Log precise arrival/departure events with time.
     */
    fun logEvent(bssid: String, eventType: String) {
        val timestamp = SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()).format(Date())
        val logEntry = "[$timestamp] $eventType"
        
        val logs = preferences.getStringSet(PREFIX_EVENT_LOGS + bssid, mutableSetOf()) ?: mutableSetOf()
        val newLogs = logs.toMutableSet()
        newLogs.add(logEntry)
        
        // Ensure BSSID is in the master list
        val allBssids = preferences.getStringSet(KEY_ALL_BSSIDS, mutableSetOf()) ?: mutableSetOf()
        val newAllBssids = allBssids.toMutableSet()
        newAllBssids.add(bssid)
        
        preferences.edit()
            .putStringSet(PREFIX_EVENT_LOGS + bssid, newLogs)
            .putStringSet(KEY_ALL_BSSIDS, newAllBssids)
            .apply()
            
        // Also keep the daily history count logic
        trackDetection(bssid)
    }

    fun getEventLogs(bssid: String): List<String> {
        return preferences.getStringSet(PREFIX_EVENT_LOGS + bssid, emptySet())?.toList()?.sortedDescending() ?: emptyList()
    }

    fun trackDetection(bssid: String) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val historyKey = PREFIX_HISTORY + bssid
        val history = preferences.getStringSet(historyKey, mutableSetOf()) ?: mutableSetOf()
        if (!history.contains(today)) {
            val newHistory = history.toMutableSet()
            newHistory.add(today)
            preferences.edit().putStringSet(historyKey, newHistory).apply()
        }
    }

    fun getDetectionHistoryCount(bssid: String): Int {
        return preferences.getStringSet(PREFIX_HISTORY + bssid, emptySet())?.size ?: 0
    }

    fun getAllTrackedBssids(): List<String> {
        return preferences.getStringSet(KEY_ALL_BSSIDS, emptySet())?.toList() ?: emptyList()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }
}
