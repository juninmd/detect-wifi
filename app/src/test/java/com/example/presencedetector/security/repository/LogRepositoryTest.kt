package com.example.presencedetector.security.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class LogRepositoryTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        LogRepository.clearLogs(context)
        // Give time for clearLogs async operation
        Thread.sleep(100)
    }

    private fun waitForCondition(condition: () -> Boolean, timeoutMs: Long = 2000) {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (condition()) return
            Thread.sleep(50)
        }
    }

    @Test
    fun `logSystemEvent should create file and append log`() {
        val message = "System Started"
        LogRepository.logSystemEvent(context, message)

        waitForCondition({ LogRepository.getSystemLogs(context).isNotEmpty() })

        val logs = LogRepository.getSystemLogs(context)
        assertEquals(1, logs.size)
        assertTrue(logs[0].contains(message))

        // Check file existence
        val logFile = File(context.getExternalFilesDir(null), "presence_detector_logs/system_events.log")
        assertTrue(logFile.exists())
    }

    @Test
    fun `logDetectionEvent should create specific file`() {
        val bssid = "AA:BB:CC:DD:EE:FF"
        val message = "Device Arrived"
        LogRepository.logDetectionEvent(context, bssid, message)

        waitForCondition({ LogRepository.getDetectionLogs(context, bssid).isNotEmpty() })

        val logs = LogRepository.getDetectionLogs(context, bssid)
        assertEquals(1, logs.size)
        assertTrue(logs[0].contains(message))

        // Check filename sanitization
        val safeBssid = bssid.replace(":", "")
        val logFile = File(context.getExternalFilesDir(null), "presence_detector_logs/device_${safeBssid}.log")
        assertTrue(logFile.exists())
    }

    @Test
    fun `getLogs should return limited number of logs`() {
        for (i in 1..10) {
            LogRepository.logSystemEvent(context, "Log $i")
        }

        waitForCondition({ LogRepository.getSystemLogs(context).size >= 10 })

        val logs = LogRepository.getSystemLogs(context, limit = 5)
        assertEquals(5, logs.size)
        // Newest first. Log 10 should be first.
        assertTrue("Expected 'Log 10' but got '${logs[0]}'", logs[0].contains("Log 10"))
        assertTrue("Expected 'Log 6' but got '${logs[4]}'", logs[4].contains("Log 6"))
    }
}
