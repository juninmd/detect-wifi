package com.example.presencedetector.services

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.presencedetector.model.DeviceCategory
import com.example.presencedetector.model.DeviceSource
import com.example.presencedetector.model.WiFiDevice
import com.example.presencedetector.utils.PreferencesUtil
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
class PresenceDetectionManagerTest {

    @Mock private lateinit var mockWifiService: WiFiDetectionService
    @Mock private lateinit var mockBluetoothService: BluetoothDetectionService
    @Mock private lateinit var mockTelegramService: TelegramService

    private lateinit var context: Context
    private lateinit var manager: PresenceDetectionManager
    private lateinit var preferences: PreferencesUtil

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        preferences = PreferencesUtil(context)
        preferences.clear()

        // Setup default mocks
        // Mocks return null/void by default, so explicit stubbing for void methods is not needed
        // unless we want to trigger a callback immediately, which we verify later instead.

        manager = PresenceDetectionManager(
            context,
            true,
            mockWifiService,
            mockBluetoothService,
            mockTelegramService
        )
    }

    @Test
    fun `startDetection starts services`() {
        manager.startDetection()
        verify(mockWifiService).startScanning()
        verify(mockBluetoothService).startScanning()
    }

    @Test
    fun `stopDetection stops services`() {
        manager.stopDetection()
        verify(mockWifiService).stopScanning()
        verify(mockBluetoothService).stopScanning()
    }

    @Test
    fun `WiFi detection triggers presence`() {
        // Capture listener
        val captor = argumentCaptor<WiFiDetectionService.PresenceListener>()
        verify(mockWifiService).setPresenceListener(captor.capture())
        val listener = captor.firstValue

        // Simulate detection
        val device = WiFiDevice(
            ssid = "TestSSID",
            bssid = "00:11:22:33:44:55",
            level = -50,
            frequency = 2400,
            source = DeviceSource.WIFI
        )
        listener.onPresenceDetected(true, listOf(device), "Found 1 device")

        ShadowLooper.runUiThreadTasks()

        assertTrue(manager.getDetectionStatus().contains("Present: YES"))

        // Check logs
        val logs = preferences.getEventLogs("00:11:22:33:44:55")
        assertTrue(logs.isNotEmpty())
        assertTrue(logs[0].contains("Arrived"))
    }

    @Test
    fun `Bluetooth detection triggers presence and notification`() {
        preferences.setNotifyOnPresence(true)

        val captor = argumentCaptor<BluetoothDetectionService.PresenceListener>()
        verify(mockBluetoothService).setPresenceListener(captor.capture())
        val listener = captor.firstValue

        val device = WiFiDevice(
            ssid = "TestBT",
            bssid = "AA:BB:CC:DD:EE:FF",
            level = -50,
            frequency = 0,
            source = DeviceSource.BLUETOOTH
        )
        listener.onPresenceDetected(true, listOf(device), "Found BT")

        ShadowLooper.runUiThreadTasks()

        assertTrue(manager.getDetectionStatus().contains("Present: YES"))
    }

    @Test
    fun `External (Camera) detection triggers presence`() {
        // Enable telegram for this test to verify interaction
        preferences.setTelegramEnabled(true)

        manager.handleExternalPresence("Camera1", "Person")

        ShadowLooper.runUiThreadTasks()

        assertTrue(manager.getDetectionStatus().contains("Camera: Active"))
        assertTrue(manager.getDetectionStatus().contains("Present: YES"))

        // Check Telegram call
        verify(mockTelegramService, atLeastOnce()).sendMessage(any())
    }
}
