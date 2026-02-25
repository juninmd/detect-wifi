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
    private lateinit var logsDir: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        logsDir = File(context.getExternalFilesDir(null), "presence_detector_logs")
        if (!logsDir.exists()) logsDir.mkdirs()
        // Clear logs
        logsDir.listFiles()?.forEach { it.delete() }
    }

    @Test
    fun testReadLogsReverse() {
        val bssid = "00:11:22"
        val expectedFilename = "device_001122.log"
        val logFile = File(logsDir, expectedFilename)

        // Create a large log file
        val totalLines = 1000
        logFile.bufferedWriter().use { writer ->
            for (i in 1..totalLines) {
                writer.write("Log line $i\n")
            }
        }

        val limit = 50
        val logs = LogRepository.getDetectionLogs(context, bssid, limit)

        assertEquals("Expected $limit logs", limit, logs.size)
        // LogRepository returns newest first. So "Log line 1000" should be first.
        assertEquals("Log line 1000", logs[0])
        assertEquals("Log line 951", logs[49])
    }

    @Test
    fun testReadLogsReverseShortFile() {
        val bssid = "AA:BB:CC"
        val expectedFilename = "device_AABBCC.log"
        val logFile = File(logsDir, expectedFilename)

        logFile.bufferedWriter().use { writer ->
            writer.write("Line 1\nLine 2\n")
        }

        val logs = LogRepository.getDetectionLogs(context, bssid, 100)
        assertEquals(2, logs.size)
        assertEquals("Line 2", logs[0])
        assertEquals("Line 1", logs[1])
    }

    @Test
    fun testReadLogsReverseEmptyFile() {
        val bssid = "DD:EE:FF"
        val expectedFilename = "device_DDEEFF.log"
        val logFile = File(logsDir, expectedFilename)
        logFile.createNewFile()

        val logs = LogRepository.getDetectionLogs(context, bssid, 100)
        assertTrue(logs.isEmpty())
    }
}
