package com.example.presencedetector.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.presencedetector.data.preferences.DetectionPreferences
import com.example.presencedetector.data.preferences.DeviceInfoPreferences
import com.example.presencedetector.data.preferences.SecurityPreferences
import com.example.presencedetector.data.preferences.TelegramPreferences
import com.example.presencedetector.model.DeviceCategory

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
      const val SILENT_MODE_ENABLED = "silent_mode_enabled"

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

  private val preferences: SharedPreferences =
    context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

  private val detectionPreferences = DetectionPreferences(context)
  private val securityPreferences = SecurityPreferences(context)
  private val telegramPreferences = TelegramPreferences(context)
  private val deviceInfoPreferences = DeviceInfoPreferences(context)

  // --- Detection Settings ---
  open fun setDetectionEnabled(enabled: Boolean) = detectionPreferences.setDetectionEnabled(enabled)

  open fun isDetectionEnabled() = detectionPreferences.isDetectionEnabled()

  open fun setNotifyOnPresence(enabled: Boolean) = detectionPreferences.setNotifyOnPresence(enabled)

  fun shouldNotifyOnPresence() = detectionPreferences.shouldNotifyOnPresence()

  open fun setNotifyWifiArrival(enabled: Boolean) = detectionPreferences.setNotifyWifiArrival(enabled)

  open fun shouldNotifyWifiArrival() = detectionPreferences.shouldNotifyWifiArrival()

  open fun setNotifyArrival(bssid: String, notify: Boolean) =
    detectionPreferences.setNotifyArrival(bssid, notify)

  open fun shouldNotifyArrival(bssid: String) = detectionPreferences.shouldNotifyArrival(bssid)

  open fun setNotifyDeparture(bssid: String, notify: Boolean) =
    detectionPreferences.setNotifyDeparture(bssid, notify)

  open fun shouldNotifyDeparture(bssid: String) =
    detectionPreferences.shouldNotifyDeparture(bssid)

  open fun setCriticalAlertEnabled(bssid: String, enabled: Boolean) =
    detectionPreferences.setCriticalAlertEnabled(bssid, enabled)

  open fun isCriticalAlertEnabled(bssid: String) =
    detectionPreferences.isCriticalAlertEnabled(bssid)

  open fun setTelegramAlertEnabled(bssid: String, enabled: Boolean) =
    detectionPreferences.setTelegramAlertEnabled(bssid, enabled)

  open fun isTelegramAlertEnabled(bssid: String) =
    detectionPreferences.isTelegramAlertEnabled(bssid)

  // --- Telegram Settings ---
  open fun setTelegramEnabled(enabled: Boolean) = telegramPreferences.setTelegramEnabled(enabled)

  open fun isTelegramEnabled() = telegramPreferences.isTelegramEnabled()

  open fun setTelegramToken(token: String) = telegramPreferences.setTelegramToken(token)

  open fun getTelegramToken(): String? = telegramPreferences.getTelegramToken()

  open fun setTelegramChatId(chatId: String) = telegramPreferences.setTelegramChatId(chatId)

  open fun getTelegramChatId(): String? = telegramPreferences.getTelegramChatId()

  // --- Security Settings ---
  open fun setSecurityAlertEnabled(enabled: Boolean) =
    securityPreferences.setSecurityAlertEnabled(enabled)

  open fun isSecurityAlertEnabled() = securityPreferences.isSecurityAlertEnabled()

  open fun setSecuritySoundEnabled(enabled: Boolean) =
    securityPreferences.setSecuritySoundEnabled(enabled)

  open fun isSecuritySoundEnabled() = securityPreferences.isSecuritySoundEnabled()

  fun setSecuritySchedule(start: String, end: String) =
    securityPreferences.setSecuritySchedule(start, end)

  fun getSecuritySchedule(): Pair<String, String> = securityPreferences.getSecuritySchedule()

  open fun isCurrentTimeInSecuritySchedule(): Boolean =
    securityPreferences.isCurrentTimeInSecuritySchedule()

  // --- Anti-Theft Settings ---
  fun setAntiTheftArmed(armed: Boolean) = securityPreferences.setAntiTheftArmed(armed)

  open fun isAntiTheftArmed() = securityPreferences.isAntiTheftArmed()

  fun setAntiTheftSensitivity(value: Float) = securityPreferences.setAntiTheftSensitivity(value)

  fun getAntiTheftSensitivity(): Float = securityPreferences.getAntiTheftSensitivity()

  open fun setBiometricEnabled(enabled: Boolean) = securityPreferences.setBiometricEnabled(enabled)

  open fun isBiometricEnabled() = securityPreferences.isBiometricEnabled()

  fun setAppLockEnabled(enabled: Boolean) = securityPreferences.setAppLockEnabled(enabled)

  fun isAppLockEnabled() = securityPreferences.isAppLockEnabled()

  open fun setPocketModeEnabled(enabled: Boolean) = securityPreferences.setPocketModeEnabled(enabled)

  open fun isPocketModeEnabled() = securityPreferences.isPocketModeEnabled()

  open fun setChargerModeEnabled(enabled: Boolean) = securityPreferences.setChargerModeEnabled(enabled)

  open fun isChargerModeEnabled() = securityPreferences.isChargerModeEnabled()

  open fun setSmartModeEnabled(enabled: Boolean) = securityPreferences.setSmartModeEnabled(enabled)

  open fun isSmartModeEnabled() = securityPreferences.isSmartModeEnabled()

  open fun setSilentModeEnabled(enabled: Boolean) = securityPreferences.setSilentModeEnabled(enabled)

  open fun isSilentModeEnabled() = securityPreferences.isSilentModeEnabled()

  // --- Device Info ---
  open fun saveNickname(bssid: String, nickname: String) =
    deviceInfoPreferences.saveNickname(bssid, nickname)

  open fun getNickname(bssid: String) = deviceInfoPreferences.getNickname(bssid)

  open fun saveManualCategory(bssid: String, category: DeviceCategory) =
    deviceInfoPreferences.saveManualCategory(bssid, category)

  open fun getManualCategory(bssid: String): DeviceCategory? =
    deviceInfoPreferences.getManualCategory(bssid)

  open fun trackDetection(bssid: String) = deviceInfoPreferences.trackDetection(bssid)

  open fun getDetectionHistoryCount(bssid: String): Int =
    deviceInfoPreferences.getDetectionHistoryCount(bssid)

  fun getAllTrackedBssids(): List<String> = deviceInfoPreferences.getAllTrackedBssids()

  fun setTrustedWifiSsid(ssid: String) = securityPreferences.setTrustedWifiSsid(ssid)

  fun getTrustedWifiSsid(): String? = securityPreferences.getTrustedWifiSsid()

  fun clear() {
    preferences.edit().clear().apply()
  }

  fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
    preferences.registerOnSharedPreferenceChangeListener(listener)
  }

  fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
    preferences.unregisterOnSharedPreferenceChangeListener(listener)
  }
}
