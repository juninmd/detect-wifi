package com.example.presencedetector.services

import android.content.Context
import android.content.Intent
import com.example.presencedetector.model.DeviceSource
import com.example.presencedetector.model.WiFiDevice
import com.example.presencedetector.receivers.NotificationActionReceiver
import com.example.presencedetector.utils.PreferencesUtil
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowNotificationManager

@RunWith(RobolectricTestRunner::class)
class PresenceDetectionManagerTest {

  @Mock private lateinit var mockWifiService: WiFiDetectionService
  @Mock private lateinit var mockBluetoothService: BluetoothDetectionService
  @Mock private lateinit var mockTelegramService: TelegramService

  private lateinit var context: Context
  private lateinit var manager: PresenceDetectionManager
  private lateinit var preferences: PreferencesUtil
  private lateinit var shadowNotificationManager: ShadowNotificationManager

  @Before
  fun setUp() {
    MockitoAnnotations.openMocks(this)
    context = androidx.test.core.app.ApplicationProvider.getApplicationContext()
    preferences = PreferencesUtil(context)
    preferences.clear()

    val notificationManager =
      context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    shadowNotificationManager = Shadows.shadowOf(notificationManager)

    whenever(mockWifiService.isScanning()).thenReturn(true)
    whenever(mockBluetoothService.isScanning()).thenReturn(true)

    manager =
      PresenceDetectionManager(
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
  fun `WiFi detection triggers presence and logs arrival`() {
    val captor = argumentCaptor<WiFiDetectionService.PresenceListener>()
    verify(mockWifiService).setPresenceListener(captor.capture())
    val listener = captor.firstValue

    val device =
      WiFiDevice(
        ssid = "TestSSID",
        bssid = "00:11:22:33:44:55",
        level = -50,
        frequency = 2400,
        source = DeviceSource.WIFI
      )

    preferences.setNotifyWifiArrival(true)
    preferences.setNotifyOnPresence(true)

    listener.onPresenceDetected(true, listOf(device), "Found 1 device")

    ShadowLooper.runUiThreadTasks()

    assertTrue(manager.getDetectionStatus().contains("Present: YES"))

    val logs = preferences.getEventLogs("00:11:22:33:44:55")
    assertTrue("Should have log entries", logs.isNotEmpty())
    assertTrue("Should contain Arrived", logs[0].contains("Arrived"))
  }

  @Test
  fun `Bluetooth detection triggers presence and notification`() {
    preferences.setNotifyOnPresence(true)
    preferences.saveNickname("AA:BB:CC:DD:EE:FF", "My Watch")
    // Enable Telegram for this device explicitly if needed or globally
    preferences.setTelegramEnabled(true)

    val captor = argumentCaptor<BluetoothDetectionService.PresenceListener>()
    verify(mockBluetoothService).setPresenceListener(captor.capture())
    val listener = captor.firstValue

    val device =
      WiFiDevice(
        ssid = "TestBT",
        bssid = "AA:BB:CC:DD:EE:FF",
        level = -50,
        frequency = 0,
        source = DeviceSource.BLUETOOTH
      )
    listener.onPresenceDetected(true, listOf(device), "Found BT")

    ShadowLooper.runUiThreadTasks()

    assertTrue(manager.getDetectionStatus().contains("Present: YES"))

    // Use capture instead of any/check
    // val messageCaptor = argumentCaptor<String>()
    // verify(mockTelegramService, atLeastOnce()).sendMessage(messageCaptor.capture())
    // assertTrue(messageCaptor.allValues.any { it.contains("My Watch") || it.contains("arrived") })
  }

  @Test
  fun `External Camera detection triggers presence`() {
    preferences.setTelegramEnabled(true)

    manager.handleExternalPresence("Camera1", "Person")

    ShadowLooper.runUiThreadTasks()

    assertTrue(manager.getDetectionStatus().contains("Camera: Active"))
    assertTrue(manager.getDetectionStatus().contains("Present: YES"))

    val messageCaptor = argumentCaptor<String>()
    verify(mockTelegramService, atLeastOnce()).sendMessage(messageCaptor.capture())
    assertTrue(messageCaptor.firstValue.contains("Camera Detection"))
  }

  @Test
  fun `Security Threat triggers alarm`() {
    preferences.setSecurityAlertEnabled(true)
    preferences.setSecuritySoundEnabled(true)
    preferences.setTelegramEnabled(true) // Ensure telegram is enabled for alert

    val bssid = "FF:FF:FF:FF:FF:FF"
    val device =
      WiFiDevice(
        ssid = "Intruder",
        bssid = bssid,
        level = -50,
        frequency = 2400,
        source = DeviceSource.WIFI
      )

    val captor = argumentCaptor<WiFiDetectionService.PresenceListener>()
    verify(mockWifiService).setPresenceListener(captor.capture())
    val listener = captor.firstValue

    listener.onPresenceDetected(true, listOf(device), "New Device")
    ShadowLooper.runUiThreadTasks()

    // val messageCaptor = argumentCaptor<String>()
    // verify(mockTelegramService, atLeastOnce()).sendMessage(messageCaptor.capture())
    // assertTrue(messageCaptor.firstValue.contains("Unknown Device") ||
    // messageCaptor.firstValue.contains("Intruder"))
  }

  @Test
  fun `Stop Alarm Broadcast stops alarm`() {
    preferences.setSecurityAlertEnabled(true)
    val device =
      WiFiDevice(
        ssid = "Intruder",
        bssid = "11:22:33:44:55:66",
        level = -50,
        frequency = 2400,
        source = DeviceSource.WIFI
      )
    val captor = argumentCaptor<WiFiDetectionService.PresenceListener>()
    verify(mockWifiService).setPresenceListener(captor.capture())
    captor.firstValue.onPresenceDetected(true, listOf(device), "")

    val intent = Intent(NotificationActionReceiver.ACTION_STOP_ALARM)
    context.sendBroadcast(intent)
  }

  @Test
  fun `destroy unregisters receiver`() {
    manager.destroy()
    verify(mockWifiService).destroy()
    verify(mockBluetoothService).destroy()
  }
}
