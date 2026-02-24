package com.example.presencedetector.data.preferences

import android.content.Context
import com.example.presencedetector.model.DeviceCategory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class DeviceInfoPreferences(context: Context) : BasePreferences(context, PREF_NAME) {

    companion object {
        const val PREF_NAME = "presence_detector_prefs"
        private const val KEY_ALL_BSSIDS = "all_bssids"
        private const val PREFIX_NICKNAME = "nickname_"
        private const val PREFIX_CATEGORY = "category_"
        private const val PREFIX_HISTORY = "history_"
    }

    // Cache for tracked BSSIDs (Last detected date) to avoid hitting SharedPreferences repeatedly
    // Key: BSSID, Value: Last Tracked Date (yyyy-MM-dd)
    private val trackedCache = ConcurrentHashMap<String, String>()

    // Cache for all known BSSIDs to avoid reading the large set from disk repeatedly
    @Volatile
    private var allBssidsCache: MutableSet<String>? = null
    private val allBssidsLock = Any()

    fun saveNickname(bssid: String, nickname: String) =
        putString(PREFIX_NICKNAME + bssid, nickname)

    fun getNickname(bssid: String) = getString(PREFIX_NICKNAME + bssid)

    fun saveManualCategory(bssid: String, category: DeviceCategory) =
        putString(PREFIX_CATEGORY + bssid, category.name)

    fun getManualCategory(bssid: String): DeviceCategory? {
        val name = getString(PREFIX_CATEGORY + bssid) ?: return null
        return try {
            DeviceCategory.valueOf(name)
        } catch (e: Exception) {
            null
        }
    }

    fun trackDetection(bssid: String) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Optimization: Check in-memory cache first (most frequent path)
        if (trackedCache[bssid] == today) {
            return
        }

        // Ensure BSSID is in the master list
        val allBssids = getOrLoadAllBssids()
        // Fast check before lock
        if (!allBssids.contains(bssid)) {
            synchronized(allBssidsLock) {
                // Double check and add atomically
                if (allBssids.add(bssid)) {
                     // Write to disk asynchronously. Create snapshot to be safe.
                     putStringSet(KEY_ALL_BSSIDS, HashSet(allBssids))
                }
            }
        }

        // Update history
        val historyKey = PREFIX_HISTORY + bssid
        val history = getStringSet(historyKey, mutableSetOf()) ?: mutableSetOf()
        if (!history.contains(today)) {
            val newHistory = history.toMutableSet()
            newHistory.add(today)
            putStringSet(historyKey, newHistory)
        }

        // Update cache so subsequent checks today are fast
        trackedCache[bssid] = today
    }

    private fun getOrLoadAllBssids(): MutableSet<String> {
        var cache = allBssidsCache
        if (cache == null) {
            synchronized(allBssidsLock) {
                cache = allBssidsCache
                if (cache == null) {
                    val loaded = getStringSet(KEY_ALL_BSSIDS, mutableSetOf()) ?: mutableSetOf()
                    // Use a concurrent set
                    val concurrentSet = ConcurrentHashMap.newKeySet<String>()
                    concurrentSet.addAll(loaded)
                    cache = concurrentSet
                    allBssidsCache = cache
                }
            }
        }
        return cache!!
    }

    fun getDetectionHistoryCount(bssid: String): Int {
        return getStringSet(PREFIX_HISTORY + bssid, emptySet())?.size ?: 0
    }

    fun getAllTrackedBssids(): List<String> {
        return getOrLoadAllBssids().toList()
    }
}
