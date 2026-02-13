package com.example.presencedetector.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LogRepositoryTest {

  private lateinit var context: Context
  private lateinit var logRepository: LogRepository

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    logRepository = LogRepository(context)
    // Clear prefs if possible, or just rely on fresh context from Robolectric
    val prefs = context.getSharedPreferences("presence_detector_prefs", Context.MODE_PRIVATE)
    prefs.edit().clear().apply()
  }

  @Test
  fun `test log event`() {
    val bssid = "11:22:33:44:55:66"
    logRepository.logEvent(bssid, "Arrived")

    val logs = logRepository.getEventLogs(bssid)
    assertEquals(1, logs.size)
    assertTrue(logs[0].contains("Arrived"))

    // Verify BSSID is tracked
    val allBssids = logRepository.getAllTrackedBssids()
    assertTrue(allBssids.contains(bssid))
  }

  @Test
  fun `test system logs`() {
    logRepository.logSystemEvent("System Start")
    val sysLogs = logRepository.getSystemLogs()
    assertEquals(1, sysLogs.size)
    assertTrue(sysLogs[0].contains("System Start"))
  }

  @Test
  fun `test detection history count`() {
    val bssid = "HISTORY:BSSID"
    assertEquals(0, logRepository.getDetectionHistoryCount(bssid))

    logRepository.trackDetection(bssid)
    assertEquals(1, logRepository.getDetectionHistoryCount(bssid))

    // Same day shouldn't increase count
    logRepository.trackDetection(bssid)
    assertEquals(1, logRepository.getDetectionHistoryCount(bssid))
  }
}
