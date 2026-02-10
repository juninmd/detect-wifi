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
open class PreferencesUtil(context: Context) {
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
        const val KEY_ANTI_THEFT_SENSITIVITY = "anti_theft_sensitivity"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        private const val KEY_POCKET_MODE_ENABLED = "pocket_mode_enabled"
        private const val KEY_CHARGER_MODE_ENABLED = "charger_mode_enabled"
        private const val KEY_SMART_MODE_ENABLED = "smart_mode_enabled"

        private const val PREFIX_HISTORY = "history_"
        private const val PREFIX_NICKNAME = "nickname_"
        private const val PREFIX_CATEGORY = "category_"
        private const val PREFIX_NOTIFY_ARRIVAL = "notify_arrival_"
        private const val PREFIX_NOTIFY_DEPARTURE = "notify_departure_"
        private const val PREFIX_CRITICAL_ALERT = "critical_alert_"
        private const val PREFIX_TELEGRAM_ALERT = "telegram_alert_"
        private const val PREFIX_EVENT_LOGS = "event_logs_"
        private const val KEY_ALL_BSSIDS = "all_bssids"
        private const val KEY_TRUSTED_WIFI_SSID = "trusted_wifi_ssid"
        private const val KEY_SYSTEM_LOGS = "system_logs"
    }

    private val preferences: SharedPreferences = context.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE
    )

    // --- Helpers ---
    private fun putBoolean(key: String, value: Boolean) = preferences.edit().putBoolean(key, value).apply()
    private fun getBoolean(key: String, default: Boolean) = preferences.getBoolean(key, default)
    private fun putString(key: String, value: String?) = preferences.edit().putString(key, value).apply()
    private fun getString(key: String, default: String? = null) = preferences.getString(key, default)
    private fun putFloat(key: String, value: Float) = preferences.edit().putFloat(key, value).apply()
    private fun getFloat(key: String, default: Float) = preferences.getFloat(key, default)
    private fun putStringSet(key: String, value: Set<String>) = preferences.edit().putStringSet(key, value).apply()
    private fun getStringSet(key: String, default: Set<String>? = null) = preferences.getStringSet(key, default)

    // --- Detection Settings ---
    open fun setDetectionEnabled(enabled: Boolean) = putBoolean(KEY_DETECTION_ENABLED, enabled)
    open fun isDetectionEnabled() = getBoolean(KEY_DETECTION_ENABLED, false)

    open fun setNotifyOnPresence(enabled: Boolean) = putBoolean(KEY_NOTIFY_ON_PRESENCE, enabled)
    fun shouldNotifyOnPresence() = getBoolean(KEY_NOTIFY_ON_PRESENCE, true)

    open fun setNotifyWifiArrival(enabled: Boolean) = putBoolean(KEY_NOTIFY_WIFI_ARRIVAL, enabled)
    open fun shouldNotifyWifiArrival() = getBoolean(KEY_NOTIFY_WIFI_ARRIVAL, false)

    open fun setNotifyArrival(bssid: String, notify: Boolean) = putBoolean(PREFIX_NOTIFY_ARRIVAL + bssid, notify)
    open fun shouldNotifyArrival(bssid: String) = getBoolean(PREFIX_NOTIFY_ARRIVAL + bssid, false)

    open fun setNotifyDeparture(bssid: String, notify: Boolean) = putBoolean(PREFIX_NOTIFY_DEPARTURE + bssid, notify)
    open fun shouldNotifyDeparture(bssid: String) = getBoolean(PREFIX_NOTIFY_DEPARTURE + bssid, false)

    open fun setCriticalAlertEnabled(bssid: String, enabled: Boolean) = putBoolean(PREFIX_CRITICAL_ALERT + bssid, enabled)
    open fun isCriticalAlertEnabled(bssid: String) = getBoolean(PREFIX_CRITICAL_ALERT + bssid, false)

    open fun setTelegramAlertEnabled(bssid: String, enabled: Boolean) = putBoolean(PREFIX_TELEGRAM_ALERT + bssid, enabled)
    open fun isTelegramAlertEnabled(bssid: String) = getBoolean(PREFIX_TELEGRAM_ALERT + bssid, false)

    // --- Telegram Settings ---
    open fun setTelegramEnabled(enabled: Boolean) = putBoolean(KEY_TELEGRAM_ENABLED, enabled)
    open fun isTelegramEnabled() = getBoolean(KEY_TELEGRAM_ENABLED, false)
    open fun setTelegramToken(token: String) = putString(KEY_TELEGRAM_TOKEN, token)
    open fun getTelegramToken(): String? = getString(KEY_TELEGRAM_TOKEN)
    open fun setTelegramChatId(chatId: String) = putString(KEY_TELEGRAM_CHAT_ID, chatId)
    open fun getTelegramChatId(): String? = getString(KEY_TELEGRAM_CHAT_ID)

    // --- Security Settings ---
    open fun setSecurityAlertEnabled(enabled: Boolean) = putBoolean(KEY_SECURITY_ALERT_ENABLED, enabled)
    open fun isSecurityAlertEnabled() = getBoolean(KEY_SECURITY_ALERT_ENABLED, false)
    open fun setSecuritySoundEnabled(enabled: Boolean) = putBoolean(KEY_SECURITY_SOUND_ENABLED, enabled)
    open fun isSecuritySoundEnabled() = getBoolean(KEY_SECURITY_SOUND_ENABLED, false)

    fun setSecuritySchedule(start: String, end: String) {
        preferences.edit().putString(KEY_SECURITY_START_TIME, start).putString(KEY_SECURITY_END_TIME, end).apply()
    }

    fun getSecuritySchedule(): Pair<String, String> {
        val start = getString(KEY_SECURITY_START_TIME, "22:00") ?: "22:00"
        val end = getString(KEY_SECURITY_END_TIME, "06:00") ?: "06:00"
        return Pair(start, end)
    }

    fun isCurrentTimeInSecuritySchedule(): Boolean {
        val (startStr, endStr) = getSecuritySchedule()
        val now = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        return if (startStr > endStr) now >= startStr || now <= endStr else now in startStr..endStr
    }

    // --- Anti-Theft Settings ---
    fun setAntiTheftArmed(armed: Boolean) = putBoolean(KEY_ANTI_THEFT_ARMED, armed)
    open fun isAntiTheftArmed() = getBoolean(KEY_ANTI_THEFT_ARMED, false)

    open fun setAntiTheftSensitivity(sensitivity: Float) = putFloat(KEY_ANTI_THEFT_SENSITIVITY, sensitivity)
    open fun getAntiTheftSensitivity() = getFloat(KEY_ANTI_THEFT_SENSITIVITY, 1.5f)

    open fun setBiometricEnabled(enabled: Boolean) = putBoolean(KEY_BIOMETRIC_ENABLED, enabled)
    open fun isBiometricEnabled() = getBoolean(KEY_BIOMETRIC_ENABLED, false)

    fun setAppLockEnabled(enabled: Boolean) = putBoolean(KEY_APP_LOCK_ENABLED, enabled)
    fun isAppLockEnabled() = getBoolean(KEY_APP_LOCK_ENABLED, false)

    open fun setPocketModeEnabled(enabled: Boolean) = putBoolean(KEY_POCKET_MODE_ENABLED, enabled)
    open fun isPocketModeEnabled() = getBoolean(KEY_POCKET_MODE_ENABLED, false)

    open fun setChargerModeEnabled(enabled: Boolean) = putBoolean(KEY_CHARGER_MODE_ENABLED, enabled)
    open fun isChargerModeEnabled() = getBoolean(KEY_CHARGER_MODE_ENABLED, false)

    open fun setSmartModeEnabled(enabled: Boolean) = putBoolean(KEY_SMART_MODE_ENABLED, enabled)
    open fun isSmartModeEnabled() = getBoolean(KEY_SMART_MODE_ENABLED, false)

    // --- Device Info ---
    open fun saveNickname(bssid: String, nickname: String) = putString(PREFIX_NICKNAME + bssid, nickname)
    open fun getNickname(bssid: String) = getString(PREFIX_NICKNAME + bssid)

    open fun saveManualCategory(bssid: String, category: DeviceCategory) = putString(PREFIX_CATEGORY + bssid, category.name)
    open fun getManualCategory(bssid: String): DeviceCategory? {
        val name = getString(PREFIX_CATEGORY + bssid) ?: return null
        return try { DeviceCategory.valueOf(name) } catch (e: Exception) { null }
    }

    /**
     * Log precise arrival/departure events with time.
     */
    open fun logEvent(bssid: String, eventType: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val logEntry = "[$timestamp] $eventType"
        
        val logs = getStringSet(PREFIX_EVENT_LOGS + bssid, mutableSetOf()) ?: mutableSetOf()
        val newLogs = logs.toMutableSet()
        newLogs.add(logEntry)
        
        // Ensure BSSID is in the master list
        val allBssids = getStringSet(KEY_ALL_BSSIDS, mutableSetOf()) ?: mutableSetOf()
        val newAllBssids = allBssids.toMutableSet()
        newAllBssids.add(bssid)
        
        // Use batch edit manually here as we are updating multiple
        preferences.edit()
            .putStringSet(PREFIX_EVENT_LOGS + bssid, newLogs)
            .putStringSet(KEY_ALL_BSSIDS, newAllBssids)
            .apply()
            
        // Also keep the daily history count logic
        trackDetection(bssid)
    }

    open fun getEventLogs(bssid: String): List<String> {
        return getStringSet(PREFIX_EVENT_LOGS + bssid, emptySet())?.toList()?.sortedDescending() ?: emptyList()
    }

    open fun trackDetection(bssid: String) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val historyKey = PREFIX_HISTORY + bssid
        val history = getStringSet(historyKey, mutableSetOf()) ?: mutableSetOf()
        if (!history.contains(today)) {
            val newHistory = history.toMutableSet()
            newHistory.add(today)
            putStringSet(historyKey, newHistory)
        }
    }

    open fun getDetectionHistoryCount(bssid: String): Int {
        return getStringSet(PREFIX_HISTORY + bssid, emptySet())?.size ?: 0
    }

    fun getAllTrackedBssids(): List<String> {
        return getStringSet(KEY_ALL_BSSIDS, emptySet())?.toList() ?: emptyList()
    }

    fun setTrustedWifiSsid(ssid: String) = putString(KEY_TRUSTED_WIFI_SSID, ssid)
    fun getTrustedWifiSsid(): String? = getString(KEY_TRUSTED_WIFI_SSID)

    fun clear() {
        preferences.edit().clear().apply()
    }

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        preferences.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        preferences.unregisterOnSharedPreferenceChangeListener(listener)
    }

    /**
     * Log general system events (Security, Panic, Errors).
     */
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
