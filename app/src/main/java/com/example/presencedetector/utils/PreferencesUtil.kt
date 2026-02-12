package com.example.presencedetector.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.presencedetector.model.DeviceCategory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Utility class for managing application preferences and history logs. */
open class PreferencesUtil(context: Context) {
    companion object {
        private const val PREF_NAME = "presence_detector_prefs"
        const val KEY_ANTI_THEFT_SENSITIVITY = "anti_theft_sensitivity"

        object Keys {
            const val LAST_DETECTION = "last_detection"
            const val DETECTION_ENABLED = "detection_enabled"
            const val FOREGROUND_SERVICE = "foreground_service"
            const val NOTIFY_ON_PRESENCE = "notify_on_presence"
            const val NOTIFY_WIFI_ARRIVAL = "notify_wifi_arrival"

            // Telegram Settings
            const val TELEGRAM_ENABLED = "telegram_enabled"
            const val TELEGRAM_TOKEN = "telegram_token"
            const val TELEGRAM_CHAT_ID = "telegram_chat_id"

            // Security Settings
            const val SECURITY_ALERT_ENABLED = "security_alert_enabled"
            const val SECURITY_SOUND_ENABLED = "security_sound_enabled"
            const val SECURITY_START_TIME = "security_start_time"
            const val SECURITY_END_TIME = "security_end_time"
            const val ANTI_THEFT_ARMED = "anti_theft_armed"

            const val BIOMETRIC_ENABLED = "biometric_enabled"
            const val APP_LOCK_ENABLED = "app_lock_enabled"
            const val POCKET_MODE_ENABLED = "pocket_mode_enabled"
            const val CHARGER_MODE_ENABLED = "charger_mode_enabled"
            const val SMART_MODE_ENABLED = "smart_mode_enabled"

            const val ALL_BSSIDS = "all_bssids"
            const val TRUSTED_WIFI_SSID = "trusted_wifi_ssid"
            const val SYSTEM_LOGS = "system_logs"
        }

        object Prefixes {
            const val HISTORY = "history_"
            const val NICKNAME = "nickname_"
            const val CATEGORY = "category_"
            const val NOTIFY_ARRIVAL = "notify_arrival_"
            const val NOTIFY_DEPARTURE = "notify_departure_"
            const val CRITICAL_ALERT = "critical_alert_"
            const val TELEGRAM_ALERT = "telegram_alert_"
            const val EVENT_LOGS = "event_logs_"
        }
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
    open fun setDetectionEnabled(enabled: Boolean) = putBoolean(Keys.DETECTION_ENABLED, enabled)
    open fun isDetectionEnabled() = getBoolean(Keys.DETECTION_ENABLED, false)

    open fun setNotifyOnPresence(enabled: Boolean) = putBoolean(Keys.NOTIFY_ON_PRESENCE, enabled)
    fun shouldNotifyOnPresence() = getBoolean(Keys.NOTIFY_ON_PRESENCE, true)

    open fun setNotifyWifiArrival(enabled: Boolean) = putBoolean(Keys.NOTIFY_WIFI_ARRIVAL, enabled)
    open fun shouldNotifyWifiArrival() = getBoolean(Keys.NOTIFY_WIFI_ARRIVAL, false)

    open fun setNotifyArrival(bssid: String, notify: Boolean) = putBoolean(Prefixes.NOTIFY_ARRIVAL + bssid, notify)
    open fun shouldNotifyArrival(bssid: String) = getBoolean(Prefixes.NOTIFY_ARRIVAL + bssid, false)

    open fun setNotifyDeparture(bssid: String, notify: Boolean) = putBoolean(Prefixes.NOTIFY_DEPARTURE + bssid, notify)
    open fun shouldNotifyDeparture(bssid: String) = getBoolean(Prefixes.NOTIFY_DEPARTURE + bssid, false)

    open fun setCriticalAlertEnabled(bssid: String, enabled: Boolean) = putBoolean(Prefixes.CRITICAL_ALERT + bssid, enabled)
    open fun isCriticalAlertEnabled(bssid: String) = getBoolean(Prefixes.CRITICAL_ALERT + bssid, false)

    open fun setTelegramAlertEnabled(bssid: String, enabled: Boolean) = putBoolean(Prefixes.TELEGRAM_ALERT + bssid, enabled)
    open fun isTelegramAlertEnabled(bssid: String) = getBoolean(Prefixes.TELEGRAM_ALERT + bssid, false)

    // --- Telegram Settings ---
    open fun setTelegramEnabled(enabled: Boolean) = putBoolean(Keys.TELEGRAM_ENABLED, enabled)
    open fun isTelegramEnabled() = getBoolean(Keys.TELEGRAM_ENABLED, false)
    open fun setTelegramToken(token: String) = putString(Keys.TELEGRAM_TOKEN, token)
    open fun getTelegramToken(): String? = getString(Keys.TELEGRAM_TOKEN)
    open fun setTelegramChatId(chatId: String) = putString(Keys.TELEGRAM_CHAT_ID, chatId)
    open fun getTelegramChatId(): String? = getString(Keys.TELEGRAM_CHAT_ID)

    // --- Security Settings ---
    open fun setSecurityAlertEnabled(enabled: Boolean) = putBoolean(Keys.SECURITY_ALERT_ENABLED, enabled)
    open fun isSecurityAlertEnabled() = getBoolean(Keys.SECURITY_ALERT_ENABLED, false)
    open fun setSecuritySoundEnabled(enabled: Boolean) = putBoolean(Keys.SECURITY_SOUND_ENABLED, enabled)
    open fun isSecuritySoundEnabled() = getBoolean(Keys.SECURITY_SOUND_ENABLED, false)

    fun setSecuritySchedule(start: String, end: String) {
        preferences.edit().putString(Keys.SECURITY_START_TIME, start).putString(Keys.SECURITY_END_TIME, end).apply()
    }

    fun getSecuritySchedule(): Pair<String, String> {
        val start = getString(Keys.SECURITY_START_TIME, "22:00") ?: "22:00"
        val end = getString(Keys.SECURITY_END_TIME, "06:00") ?: "06:00"
        return Pair(start, end)
    }

    fun isCurrentTimeInSecuritySchedule(): Boolean {
        val (start, end) = getSecuritySchedule()
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val now = dateFormat.format(Date())

        return if (start <= end) {
            now in start..end
        } else {
            now >= start || now <= end
        }
    }

    // --- Anti-Theft Settings ---
    fun setAntiTheftSensitivity(sensitivity: Float) = putFloat(KEY_ANTI_THEFT_SENSITIVITY, sensitivity)
    fun getAntiTheftSensitivity() = getFloat(KEY_ANTI_THEFT_SENSITIVITY, 1.5f)

    fun setAntiTheftArmed(armed: Boolean) = putBoolean(Keys.ANTI_THEFT_ARMED, armed)
    open fun isAntiTheftArmed() = getBoolean(Keys.ANTI_THEFT_ARMED, false)

    open fun setBiometricEnabled(enabled: Boolean) = putBoolean(Keys.BIOMETRIC_ENABLED, enabled)
    open fun isBiometricEnabled() = getBoolean(Keys.BIOMETRIC_ENABLED, false)

    fun setAppLockEnabled(enabled: Boolean) = putBoolean(Keys.APP_LOCK_ENABLED, enabled)
    fun isAppLockEnabled() = getBoolean(Keys.APP_LOCK_ENABLED, false)

    open fun setPocketModeEnabled(enabled: Boolean) = putBoolean(Keys.POCKET_MODE_ENABLED, enabled)
    open fun isPocketModeEnabled() = getBoolean(Keys.POCKET_MODE_ENABLED, false)

    open fun setChargerModeEnabled(enabled: Boolean) = putBoolean(Keys.CHARGER_MODE_ENABLED, enabled)
    open fun isChargerModeEnabled() = getBoolean(Keys.CHARGER_MODE_ENABLED, false)

    open fun setSmartModeEnabled(enabled: Boolean) = putBoolean(Keys.SMART_MODE_ENABLED, enabled)
    open fun isSmartModeEnabled() = getBoolean(Keys.SMART_MODE_ENABLED, false)

    // --- Device Info ---
    open fun saveNickname(bssid: String, nickname: String) = putString(Prefixes.NICKNAME + bssid, nickname)
    open fun getNickname(bssid: String) = getString(Prefixes.NICKNAME + bssid)

    open fun saveManualCategory(bssid: String, category: DeviceCategory) = putString(Prefixes.CATEGORY + bssid, category.name)
    open fun getManualCategory(bssid: String): DeviceCategory? {
        val name = getString(Prefixes.CATEGORY + bssid) ?: return null
        return try { DeviceCategory.valueOf(name) } catch (e: Exception) { null }
    }

    /**
     * Log precise arrival/departure events with time.
     */
    open fun logEvent(bssid: String, eventType: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val logEntry = "[$timestamp] $eventType"
        
        val logs = getStringSet(Prefixes.EVENT_LOGS + bssid, mutableSetOf()) ?: mutableSetOf()
        val newLogs = logs.toMutableSet()
        newLogs.add(logEntry)
        
        // Ensure BSSID is in the master list
        val allBssids = getStringSet(Keys.ALL_BSSIDS, mutableSetOf()) ?: mutableSetOf()
        val newAllBssids = allBssids.toMutableSet()
        newAllBssids.add(bssid)
        
        // Use batch edit manually here as we are updating multiple
        preferences.edit()
            .putStringSet(Prefixes.EVENT_LOGS + bssid, newLogs)
            .putStringSet(Keys.ALL_BSSIDS, newAllBssids)
            .apply()
            
        // Also keep the daily history count logic
        trackDetection(bssid)
    }

    open fun getEventLogs(bssid: String): List<String> {
        return getStringSet(Prefixes.EVENT_LOGS + bssid, emptySet())?.toList()?.sortedDescending() ?: emptyList()
    }

    open fun trackDetection(bssid: String) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val historyKey = Prefixes.HISTORY + bssid
        val history = getStringSet(historyKey, mutableSetOf()) ?: mutableSetOf()
        if (!history.contains(today)) {
            val newHistory = history.toMutableSet()
            newHistory.add(today)
            putStringSet(historyKey, newHistory)
        }
    }

    open fun getDetectionHistoryCount(bssid: String): Int {
        return getStringSet(Prefixes.HISTORY + bssid, emptySet())?.size ?: 0
    }

    fun getAllTrackedBssids(): List<String> {
        return getStringSet(Keys.ALL_BSSIDS, emptySet())?.toList() ?: emptyList()
    }

    fun setTrustedWifiSsid(ssid: String) = putString(Keys.TRUSTED_WIFI_SSID, ssid)
    fun getTrustedWifiSsid(): String? = getString(Keys.TRUSTED_WIFI_SSID)

    fun clear() {
        preferences.edit().clear().apply()
    }

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        preferences.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        preferences.unregisterOnSharedPreferenceChangeListener(listener)
    }

    /** Log general system events (Security, Panic, Errors). */
    fun logSystemEvent(message: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val logEntry = "[$timestamp] $message"

        val logs = getStringSet(Keys.SYSTEM_LOGS, mutableSetOf()) ?: mutableSetOf()
        val newLogs = logs.toMutableSet()
        newLogs.add(logEntry)

        putStringSet(Keys.SYSTEM_LOGS, newLogs)
    }

    fun getSystemLogs(): List<String> {
        return getStringSet(Keys.SYSTEM_LOGS, emptySet())?.toList()?.sortedDescending() ?: emptyList()
    }
}
