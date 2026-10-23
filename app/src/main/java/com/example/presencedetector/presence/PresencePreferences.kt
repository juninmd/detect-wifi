package com.example.presencedetector.presence

import android.content.Context
import com.example.presencedetector.data.preferences.BasePreferences
import com.example.presencedetector.router.KeystoreSecretBox

/** Backend address, API key (encrypted with the Android Keystore) and the last poll baseline. */
class PresencePreferences(context: Context) : BasePreferences(context, PREF_NAME) {
  companion object {
    const val PREF_NAME = "presence_prefs"
    const val DEFAULT_URL = "http://192.168.1.10:3000"
    const val WINDOW_SECONDS = 300
    private const val KEY_URL = "base_url"
    private const val KEY_API_KEY = "api_key_enc"
    private const val KEY_MONITOR = "monitor_enabled"
    private const val KEY_LAST_UNKNOWN = "last_unknown_macs"
  }

  var baseUrl: String
    get() = getString(KEY_URL, DEFAULT_URL) ?: DEFAULT_URL
    set(value) = putString(KEY_URL, value.trim())

  var apiKey: String?
    get() = getString(KEY_API_KEY)?.let(KeystoreSecretBox::decrypt)
    set(value) = putString(KEY_API_KEY, value?.let(KeystoreSecretBox::encrypt))

  var monitoringEnabled: Boolean
    get() = getBoolean(KEY_MONITOR, false)
    set(value) = putBoolean(KEY_MONITOR, value)

  /** Unknown MACs of the previous poll; null until the first poll has set a baseline. */
  var lastUnknownMacs: Set<String>?
    get() = getStringSet(KEY_LAST_UNKNOWN, null)
    set(value) = putStringSet(KEY_LAST_UNKNOWN, value.orEmpty())

  fun isConfigured() = !apiKey.isNullOrEmpty() && baseUrl.isNotBlank()
}
