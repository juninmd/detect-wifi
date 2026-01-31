package com.example.presencedetector.utils

import android.os.Environment
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LoggerUtilTest {

    @Test
    fun `logEvent writes to file`() {
        // Ensure external storage is available
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED)

        val message = "Test Event"
        LoggerUtil.logEvent(message)

        // Verify file exists
        val logDir = File(Environment.getExternalStorageDirectory(), "presence_detector_logs")

        assertTrue("Log directory should exist", logDir.exists())

        val files = logDir.listFiles()
        assertNotNull("Files list should not be null", files)
        assertTrue("Should have at least one log file", files!!.isNotEmpty())

        val content = files[0].readText()
        assertTrue("Log content should contain the message", content.contains(message))
    }
}
