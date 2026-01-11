package com.example.presencedetector.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Utility class for managing application preferences.
 */
class PreferencesUtil(context: Context) {
    companion object {
        private const val PREF_NAME = "presence_detector_prefs"
        private const val KEY_LAST_DETECTION = "last_detection"
        private const val KEY_DETECTION_ENABLED = "detection_enabled"
        private const val KEY_FOREGROUND_SERVICE = "foreground_service"
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

    fun clear() {
        preferences.edit().clear().apply()
    }
}
