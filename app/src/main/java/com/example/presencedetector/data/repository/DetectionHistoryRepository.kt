package com.example.presencedetector.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.LruCache
import com.example.presencedetector.utils.PreferencesUtil
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale

/**
 * Repository for managing detection history and tracked BSSIDs.
 * Extracted from PreferencesUtil to separate concerns.
 */
class DetectionHistoryRepository(context: Context) {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "presence_detector_prefs"
        private val cacheLock = Any()

        @Volatile
        private var trackedBssidsCache: MutableSet<String>? = null

        // Limit history cache to 500 recently detected devices to prevent OOM
        private val historyCache = LruCache<String, MutableSet<String>>(500)
    }

    private fun getStringSet(key: String, default: Set<String>): Set<String>? {
        return preferences.getStringSet(key, default)
    }

    private fun putStringSet(key: String, value: Set<String>) {
        preferences.edit().putStringSet(key, value).apply()
    }

    private fun getOrLoadTrackedBssids(): MutableSet<String> {
        var cache = trackedBssidsCache
        if (cache == null) {
            synchronized(cacheLock) {
                cache = trackedBssidsCache
                if (cache == null) {
                    val loaded = getStringSet(PreferencesUtil.Companion.Keys.ALL_BSSIDS, mutableSetOf()) ?: mutableSetOf()
                    cache = Collections.synchronizedSet(loaded.toMutableSet())
                    trackedBssidsCache = cache
                }
            }
        }
        return cache!!
    }

    fun trackDetection(bssid: String) {
        // 1. Get or Load BSSID Cache (Thread-safe initialization)
        val allBssids = getOrLoadTrackedBssids()

        // 2. Add to BSSID Cache
        if (!allBssids.contains(bssid)) {
            allBssids.add(bssid)
            // Persist asynchronously
            putStringSet(PreferencesUtil.Companion.Keys.ALL_BSSIDS, allBssids.toMutableSet())
        }

        // 3. Initialize History Cache for this BSSID if needed
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val historyKey = PreferencesUtil.Companion.Prefixes.HISTORY + bssid

        var history = historyCache.get(bssid)
        if (history == null) {
            val loaded = getStringSet(historyKey, mutableSetOf()) ?: mutableSetOf()
            history = Collections.synchronizedSet(loaded.toMutableSet())
            historyCache.put(bssid, history)
        }

        // 4. Add to History Cache
        if (!history.contains(today)) {
            history.add(today)
            putStringSet(historyKey, history.toMutableSet())
        }
    }

    fun getDetectionHistoryCount(bssid: String): Int {
        // Check cache first
        val history = historyCache.get(bssid)
        if (history != null) {
            return history.size
        }
        return getStringSet(PreferencesUtil.Companion.Prefixes.HISTORY + bssid, emptySet())?.size ?: 0
    }

    fun getAllTrackedBssids(): List<String> {
        // Use the helper to ensure cache is populated if accessed for the first time
        return getOrLoadTrackedBssids().toList()
    }
}
