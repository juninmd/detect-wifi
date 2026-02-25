package com.example.presencedetector.data.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeviceInfoPreferencesTest {

  private lateinit var context: Context
  private lateinit var preferences: DeviceInfoPreferences

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    // Clear prefs
    context
      .getSharedPreferences(DeviceInfoPreferences.PREF_NAME, Context.MODE_PRIVATE)
      .edit()
      .clear()
      .commit()
    preferences = DeviceInfoPreferences(context)
  }

  @Test
  fun `trackDetection adds to history and all bssids`() {
    val bssid = "00:11:22:33:44:55"
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    preferences.trackDetection(bssid)

    // Verify history count
    assertEquals(1, preferences.getDetectionHistoryCount(bssid))

    // Verify all bssids
    val all = preferences.getAllTrackedBssids()
    assertTrue(all.contains(bssid))
  }

  @Test
  fun `trackDetection handles duplicates efficiently`() {
    val bssid = "AA:BB:CC:DD:EE:FF"

    // First track
    preferences.trackDetection(bssid)
    assertEquals(1, preferences.getDetectionHistoryCount(bssid))

    // Second track (same day)
    preferences.trackDetection(bssid)
    assertEquals(1, preferences.getDetectionHistoryCount(bssid))

    // Check all bssids size
    val all = preferences.getAllTrackedBssids()
    assertEquals(1, all.size)
  }

  @Test
  fun `cache persists across instances if underlying prefs are same`() {
    // This test checks if cache logic correctly loads from disk on new instance
    val bssid = "11:22:33:44:55:66"

    preferences.trackDetection(bssid)

    // New instance
    val newPreferences = DeviceInfoPreferences(context)

    // Should load from disk
    val all = newPreferences.getAllTrackedBssids()
    assertTrue(all.contains(bssid))

    assertEquals(1, newPreferences.getDetectionHistoryCount(bssid))
  }
}
