package com.example.presencedetector

import com.example.presencedetector.security.model.CameraChannel
import com.example.presencedetector.security.model.DetectionSettings
import org.junit.Assert.*
import org.junit.Test

class DetectionSettingsTest {

    @Test
    fun testConversionSecondsToMs() {
        val settings = DetectionSettings(
            detectionThresholdSeconds = 5,
            gracePeriodSeconds = 1,
            notificationCooldownSeconds = 60
        )

        assertEquals(5000L, settings.detectionThresholdMs)
        assertEquals(1000L, settings.gracePeriodMs)
        assertEquals(60000L, settings.notificationCooldownMs)
    }

    @Test
    fun testWithUpdatedChannelConnections() {
        // Arrange
        val channel1 = CameraChannel(id = 1, name = "Cam 1", host = "old_host", port = 111, username = "u", password = "p")
        val initialSettings = DetectionSettings(
            dvrHost = "new_host",
            dvrPort = 554,
            username = "admin",
            password = "123",
            channels = listOf(channel1)
        )

        // Act
        val updatedSettings = initialSettings.withUpdatedChannelConnections()
        val updatedChannel = updatedSettings.channels[0]

        // Assert
        assertEquals("new_host", updatedChannel.host)
        assertEquals(554, updatedChannel.port)
        assertEquals("admin", updatedChannel.username)
        assertEquals("123", updatedChannel.password)
        assertEquals("Cam 1", updatedChannel.name) // Should retain original properties
    }

    @Test
    fun testAddChannel() {
        val settings = DetectionSettings()
        val settingsWithChannel = settings.addChannel("Test Cam", 1)

        assertEquals(1, settingsWithChannel.channels.size)
        assertEquals("Test Cam", settingsWithChannel.channels[0].name)
        assertTrue(settingsWithChannel.enabledChannelIds.contains(settingsWithChannel.channels[0].id))
    }
}
