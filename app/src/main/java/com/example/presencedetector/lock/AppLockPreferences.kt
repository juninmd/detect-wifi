package com.example.presencedetector.lock

import android.content.Context
import com.example.presencedetector.data.preferences.BasePreferences

/** SharedPreferences-backed [AppLockStore]. */
class AppLockPreferences(context: Context) : BasePreferences(context, PREF_NAME), AppLockStore {
  companion object {
    const val PREF_NAME = "app_lock_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_SALT = "pin_salt"
    private const val KEY_HASH = "pin_hash"
    private const val KEY_APPS = "protected_apps"
    private const val KEY_FAILED = "failed_attempts"
    private const val KEY_LOCKED_UNTIL = "locked_until"
  }

  override var enabled: Boolean
    get() = getBoolean(KEY_ENABLED, false)
    set(value) = putBoolean(KEY_ENABLED, value)

  override var pin: PinHasher.Hashed?
    get() {
      val salt = getString(KEY_SALT) ?: return null
      val hash = getString(KEY_HASH) ?: return null
      return PinHasher.Hashed(salt, hash)
    }
    set(value) {
      putString(KEY_SALT, value?.salt)
      putString(KEY_HASH, value?.hash)
    }

  override var protectedApps: Set<String>
    get() = getStringSet(KEY_APPS, BankingApps.DEFAULTS).orEmpty()
    set(value) = putStringSet(KEY_APPS, value)

  override var failedAttempts: Int
    get() = preferences.getInt(KEY_FAILED, 0)
    set(value) = preferences.edit().putInt(KEY_FAILED, value).apply()

  override var lockedUntilMs: Long
    get() = preferences.getLong(KEY_LOCKED_UNTIL, 0L)
    set(value) = preferences.edit().putLong(KEY_LOCKED_UNTIL, value).apply()
}
