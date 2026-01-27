package com.example.presencedetector.security.model

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DetectionSettingsTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Clear prefs
        val prefs = context.getSharedPreferences("security_detection_settings", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
    }

    @Test
    fun `test default values`() {
        val settings = DetectionSettings()
        assertEquals(5, settings.detectionThresholdSeconds)
        assertEquals(1, settings.gracePeriodSeconds)
        assertEquals(60, settings.notificationCooldownSeconds)
        assertEquals(5000L, settings.detectionThresholdMs)
        assertEquals(1000L, settings.gracePeriodMs)
        assertEquals(60000L, settings.notificationCooldownMs)
    }

    @Test
    fun `test save and load`() {
        val channel = CameraChannel(1, "Test Cam", "192.168.1.1")
        val settings = DetectionSettings(
            dvrHost = "10.0.0.1",
            detectionThresholdSeconds = 10,
            enabledChannelIds = setOf(1),
            channels = listOf(channel)
        )

        DetectionSettings.save(context, settings)

        val loaded = DetectionSettings.load(context)
        assertEquals("10.0.0.1", loaded.dvrHost)
        assertEquals(10, loaded.detectionThresholdSeconds)
        assertEquals(1, loaded.enabledChannelIds.size)
        assertTrue(loaded.enabledChannelIds.contains(1))
        assertEquals(1, loaded.channels.size)
        assertEquals("Test Cam", loaded.channels[0].name)
    }

    @Test
    fun `test channel management`() {
        val settings = DetectionSettings(
            dvrHost = "host",
            dvrPort = 123,
            username = "user",
            password = "pass"
        )

        // Add channel
        val s1 = settings.addChannel("Cam 1", 1)
        assertEquals(1, s1.channels.size)
        assertEquals("Cam 1", s1.channels[0].name)
        assertEquals("host", s1.channels[0].host) // inherit from global
        assertTrue(s1.enabledChannelIds.contains(s1.channels[0].id))

        // Add another
        val s2 = s1.addChannel("Cam 2", 2)
        assertEquals(2, s2.channels.size)

        // Remove
        val s3 = s2.removeChannel(s1.channels[0].id)
        assertEquals(1, s3.channels.size)
        assertEquals("Cam 2", s3.channels[0].name)

        // Ensure enabled ID is also removed
        assertEquals(1, s3.enabledChannelIds.size)
    }

    @Test
    fun `test update connections`() {
        val channel = CameraChannel(1, "Cam 1", "old_host")
        val settings = DetectionSettings(
            dvrHost = "new_host",
            channels = listOf(channel)
        )

        val updated = settings.withUpdatedChannelConnections()
        assertEquals("new_host", updated.channels[0].host)
    }

    @Test
    fun `test load with bad json`() {
        val prefs = context.getSharedPreferences("security_detection_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("channels_json", "{invalid_json").commit()

        val loaded = DetectionSettings.load(context)
        assertTrue(loaded.channels.isEmpty())
    }
}
