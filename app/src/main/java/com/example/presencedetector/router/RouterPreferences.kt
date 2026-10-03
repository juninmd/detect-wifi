package com.example.presencedetector.router

import android.content.Context
import com.example.presencedetector.data.preferences.BasePreferences

/** Router connection settings, the trusted device list and the last scan baseline. */
class RouterPreferences(context: Context) : BasePreferences(context, PREF_NAME) {
  companion object {
    const val PREF_NAME = "router_prefs"
    const val DEFAULT_HOST = "192.168.0.1"
    private const val KEY_HOST = "host"
    private const val KEY_USER = "username"
    private const val KEY_PASSWORD = "password_enc"
    private const val KEY_TRUSTED = "trusted_macs"
    private const val KEY_LAST_SEEN = "last_seen_macs"
    private const val KEY_MONITOR = "monitor_enabled"
  }

  var host: String
    get() = getString(KEY_HOST, DEFAULT_HOST) ?: DEFAULT_HOST
    set(value) = putString(KEY_HOST, value.trim())

  var username: String
    get() = getString(KEY_USER, "admin") ?: "admin"
    set(value) = putString(KEY_USER, value.trim())

  var password: String?
    get() = getString(KEY_PASSWORD)?.let(KeystoreSecretBox::decrypt)
    set(value) = putString(KEY_PASSWORD, value?.let(KeystoreSecretBox::encrypt))

  var monitoringEnabled: Boolean
    get() = getBoolean(KEY_MONITOR, false)
    set(value) = putBoolean(KEY_MONITOR, value)

  var trustedMacs: Set<String>
    get() = getStringSet(KEY_TRUSTED, emptySet()).orEmpty()
    set(value) = putStringSet(KEY_TRUSTED, value)

  /** MACs of the previous scan; null until the first scan has established a baseline. */
  var lastSeenMacs: Set<String>?
    get() = getStringSet(KEY_LAST_SEEN, null)
    set(value) = putStringSet(KEY_LAST_SEEN, value.orEmpty())

  fun isConfigured() = !password.isNullOrEmpty() && host.isNotBlank()

  fun setTrusted(mac: String, trusted: Boolean) {
    trustedMacs = if (trusted) trustedMacs + mac else trustedMacs - mac
  }
}
