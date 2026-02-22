package com.example.presencedetector.data.preferences

import android.content.Context

class DetectionPreferences(context: Context) : BasePreferences(context, PREF_NAME) {

    companion object {
        const val PREF_NAME = "presence_detector_prefs"
        private const val KEY_DETECTION_ENABLED = "detection_enabled"
        private const val KEY_NOTIFY_ON_PRESENCE = "notify_on_presence"
        private const val KEY_NOTIFY_WIFI_ARRIVAL = "notify_wifi_arrival"

        private const val PREFIX_NOTIFY_ARRIVAL = "notify_arrival_"
        private const val PREFIX_NOTIFY_DEPARTURE = "notify_departure_"
        private const val PREFIX_CRITICAL_ALERT = "critical_alert_"
        private const val PREFIX_TELEGRAM_ALERT = "telegram_alert_"
    }

    fun setDetectionEnabled(enabled: Boolean) = putBoolean(KEY_DETECTION_ENABLED, enabled)

    fun isDetectionEnabled() = getBoolean(KEY_DETECTION_ENABLED, false)

    fun setNotifyOnPresence(enabled: Boolean) = putBoolean(KEY_NOTIFY_ON_PRESENCE, enabled)

    fun shouldNotifyOnPresence() = getBoolean(KEY_NOTIFY_ON_PRESENCE, true)

    fun setNotifyWifiArrival(enabled: Boolean) = putBoolean(KEY_NOTIFY_WIFI_ARRIVAL, enabled)

    fun shouldNotifyWifiArrival() = getBoolean(KEY_NOTIFY_WIFI_ARRIVAL, false)

    fun setNotifyArrival(bssid: String, notify: Boolean) =
        putBoolean(PREFIX_NOTIFY_ARRIVAL + bssid, notify)

    fun shouldNotifyArrival(bssid: String) = getBoolean(PREFIX_NOTIFY_ARRIVAL + bssid, false)

    fun setNotifyDeparture(bssid: String, notify: Boolean) =
        putBoolean(PREFIX_NOTIFY_DEPARTURE + bssid, notify)

    fun shouldNotifyDeparture(bssid: String) =
        getBoolean(PREFIX_NOTIFY_DEPARTURE + bssid, false)

    fun setCriticalAlertEnabled(bssid: String, enabled: Boolean) =
        putBoolean(PREFIX_CRITICAL_ALERT + bssid, enabled)

    fun isCriticalAlertEnabled(bssid: String) =
        getBoolean(PREFIX_CRITICAL_ALERT + bssid, false)

    fun setTelegramAlertEnabled(bssid: String, enabled: Boolean) =
        putBoolean(PREFIX_TELEGRAM_ALERT + bssid, enabled)

    fun isTelegramAlertEnabled(bssid: String) =
        getBoolean(PREFIX_TELEGRAM_ALERT + bssid, false)
}
