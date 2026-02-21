package com.example.presencedetector.data.preferences

import android.content.Context
import com.example.presencedetector.utils.TimeUtil

class SecurityPreferences(context: Context) : BasePreferences(context, PREF_NAME) {

    companion object {
        const val PREF_NAME = "presence_detector_prefs"
        private const val KEY_SECURITY_ALERT_ENABLED = "security_alert_enabled"
        private const val KEY_SECURITY_SOUND_ENABLED = "security_sound_enabled"
        private const val KEY_SECURITY_START_TIME = "security_start_time"
        private const val KEY_SECURITY_END_TIME = "security_end_time"
        private const val KEY_ANTI_THEFT_ARMED = "anti_theft_armed"
        private const val KEY_ANTI_THEFT_SENSITIVITY = "anti_theft_sensitivity"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        private const val KEY_POCKET_MODE_ENABLED = "pocket_mode_enabled"
        private const val KEY_CHARGER_MODE_ENABLED = "charger_mode_enabled"
        private const val KEY_SMART_MODE_ENABLED = "smart_mode_enabled"
        private const val KEY_SILENT_MODE_ENABLED = "silent_mode_enabled"
        private const val KEY_TRUSTED_WIFI_SSID = "trusted_wifi_ssid"
    }

    fun setSecurityAlertEnabled(enabled: Boolean) =
        putBoolean(KEY_SECURITY_ALERT_ENABLED, enabled)

    fun isSecurityAlertEnabled() = getBoolean(KEY_SECURITY_ALERT_ENABLED, false)

    fun setSecuritySoundEnabled(enabled: Boolean) =
        putBoolean(KEY_SECURITY_SOUND_ENABLED, enabled)

    fun isSecuritySoundEnabled() = getBoolean(KEY_SECURITY_SOUND_ENABLED, false)

    fun setSecuritySchedule(start: String, end: String) {
        preferences.edit()
            .putString(KEY_SECURITY_START_TIME, start)
            .putString(KEY_SECURITY_END_TIME, end)
            .apply()
    }

    fun getSecuritySchedule(): Pair<String, String> {
        val start = getString(KEY_SECURITY_START_TIME, "22:00") ?: "22:00"
        val end = getString(KEY_SECURITY_END_TIME, "06:00") ?: "06:00"
        return Pair(start, end)
    }

    fun isCurrentTimeInSecuritySchedule(): Boolean {
        val (startStr, endStr) = getSecuritySchedule()
        return TimeUtil.isCurrentTimeInSchedule(startStr, endStr)
    }

    fun setAntiTheftArmed(armed: Boolean) = putBoolean(KEY_ANTI_THEFT_ARMED, armed)

    fun isAntiTheftArmed() = getBoolean(KEY_ANTI_THEFT_ARMED, false)

    fun setAntiTheftSensitivity(value: Float) = putFloat(KEY_ANTI_THEFT_SENSITIVITY, value)

    fun getAntiTheftSensitivity(): Float = getFloat(KEY_ANTI_THEFT_SENSITIVITY, 1.5f)

    fun setBiometricEnabled(enabled: Boolean) = putBoolean(KEY_BIOMETRIC_ENABLED, enabled)

    fun isBiometricEnabled() = getBoolean(KEY_BIOMETRIC_ENABLED, false)

    fun setAppLockEnabled(enabled: Boolean) = putBoolean(KEY_APP_LOCK_ENABLED, enabled)

    fun isAppLockEnabled() = getBoolean(KEY_APP_LOCK_ENABLED, false)

    fun setPocketModeEnabled(enabled: Boolean) = putBoolean(KEY_POCKET_MODE_ENABLED, enabled)

    fun isPocketModeEnabled() = getBoolean(KEY_POCKET_MODE_ENABLED, false)

    fun setChargerModeEnabled(enabled: Boolean) = putBoolean(KEY_CHARGER_MODE_ENABLED, enabled)

    fun isChargerModeEnabled() = getBoolean(KEY_CHARGER_MODE_ENABLED, false)

    fun setSmartModeEnabled(enabled: Boolean) = putBoolean(KEY_SMART_MODE_ENABLED, enabled)

    fun isSmartModeEnabled() = getBoolean(KEY_SMART_MODE_ENABLED, false)

    fun setSilentModeEnabled(enabled: Boolean) = putBoolean(KEY_SILENT_MODE_ENABLED, enabled)

    fun isSilentModeEnabled() = getBoolean(KEY_SILENT_MODE_ENABLED, false)

    fun setTrustedWifiSsid(ssid: String) = putString(KEY_TRUSTED_WIFI_SSID, ssid)

    fun getTrustedWifiSsid(): String? = getString(KEY_TRUSTED_WIFI_SSID)
}
