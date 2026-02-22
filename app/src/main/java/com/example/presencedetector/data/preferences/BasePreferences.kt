package com.example.presencedetector.data.preferences

import android.content.Context
import android.content.SharedPreferences

abstract class BasePreferences(context: Context, prefName: String) {
    protected val preferences: SharedPreferences =
        context.getSharedPreferences(prefName, Context.MODE_PRIVATE)

    protected fun putBoolean(key: String, value: Boolean) =
        preferences.edit().putBoolean(key, value).apply()

    protected fun getBoolean(key: String, default: Boolean) = preferences.getBoolean(key, default)

    protected fun putString(key: String, value: String?) =
        preferences.edit().putString(key, value).apply()

    protected fun getString(key: String, default: String? = null) = preferences.getString(key, default)

    protected fun putFloat(key: String, value: Float) = preferences.edit().putFloat(key, value).apply()

    protected fun getFloat(key: String, default: Float) = preferences.getFloat(key, default)

    protected fun putStringSet(key: String, value: Set<String>) =
        preferences.edit().putStringSet(key, value).apply()

    protected fun getStringSet(key: String, default: Set<String>? = null) =
        preferences.getStringSet(key, default)

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
