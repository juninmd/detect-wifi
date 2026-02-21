package com.example.presencedetector.data.preferences

import android.content.Context
import com.example.presencedetector.model.DeviceCategory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DeviceInfoPreferences(context: Context) : BasePreferences(context, PREF_NAME) {

    companion object {
        const val PREF_NAME = "presence_detector_prefs"
        private const val KEY_ALL_BSSIDS = "all_bssids"
        private const val PREFIX_NICKNAME = "nickname_"
        private const val PREFIX_CATEGORY = "category_"
        private const val PREFIX_HISTORY = "history_"
    }

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
        // Ensure BSSID is in the master list
        val allBssids = getStringSet(KEY_ALL_BSSIDS, mutableSetOf()) ?: mutableSetOf()
        if (!allBssids.contains(bssid)) {
            val newAllBssids = allBssids.toMutableSet()
            newAllBssids.add(bssid)
            putStringSet(KEY_ALL_BSSIDS, newAllBssids)
        }

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val historyKey = PREFIX_HISTORY + bssid
        val history = getStringSet(historyKey, mutableSetOf()) ?: mutableSetOf()
        if (!history.contains(today)) {
            val newHistory = history.toMutableSet()
            newHistory.add(today)
            putStringSet(historyKey, newHistory)
        }
    }

    fun getDetectionHistoryCount(bssid: String): Int {
        return getStringSet(PREFIX_HISTORY + bssid, emptySet())?.size ?: 0
    }

    fun getAllTrackedBssids(): List<String> {
        return getStringSet(KEY_ALL_BSSIDS, emptySet())?.toList() ?: emptyList()
    }
}
