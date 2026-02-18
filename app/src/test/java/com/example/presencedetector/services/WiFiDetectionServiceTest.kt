package com.example.presencedetector.services

import android.Manifest
import android.content.Context
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import androidx.test.core.app.ApplicationProvider
import com.example.presencedetector.model.WiFiDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowApplication
import org.robolectric.shadows.ShadowWifiManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class WiFiDetectionServiceTest {

  private lateinit var context: Context
  private lateinit var service: WiFiDetectionService
  private lateinit var shadowWifiManager: ShadowWifiManager

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    shadowWifiManager = Shadows.shadowOf(wifiManager)

    service = WiFiDetectionService(context)
  }

  @Test
  fun `performScan handles permissions correctly`() {
    // No permissions by default
    Shadows.shadowOf(ApplicationProvider.getApplicationContext<android.app.Application>()).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION)

    var callbackCalled = false
    service.setPresenceListener { _, _, details ->
      if (details == "Permission Denied") callbackCalled = true
    }

    service.startScanning()
    // Wait for coroutine loop? `launch` is used.
    // We can force run main loop.
    org.robolectric.shadows.ShadowLooper.idleMainLooper(100)

    assertTrue("Should report permission denied", callbackCalled)
    service.stopScanning()
  }

  @Test
  fun `performScan parses scan results and detects hotspots`() {
    Shadows.shadowOf(ApplicationProvider.getApplicationContext<android.app.Application>()).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)

    // Mock ScanResult (API is tricky, need to create via reflection or empty constr if available)
    // ScanResult() is accessible in recent Android versions/Robolectric
    val result1 =
      ScanResult().apply {
        SSID = "MyWiFi"
        BSSID = "00:11:22:33:44:55"
        level = -50
        frequency = 2400
        capabilities = "[WPA2]"
      }

    val result2 =
      ScanResult().apply {
        SSID = "iPhone 13" // Hotspot pattern
        BSSID = "AA:BB:CC:DD:EE:FF"
        level = -60
        frequency = 5000
      }

    shadowWifiManager.setScanResults(listOf(result1, result2))

    var devicesFound: List<WiFiDevice> = emptyList()
    val latch = java.util.concurrent.CountDownLatch(1)

    service.setPresenceListener { _, devices, _ ->
      devicesFound = devices
      latch.countDown()
    }

    service.startScanning()
    org.robolectric.shadows.ShadowLooper.idleMainLooper(100)

    assertEquals(2, devicesFound.size)

    val wifi = devicesFound.find { it.ssid == "MyWiFi" }
    val hotspot = devicesFound.find { it.ssid == "iPhone 13" }

    assertEquals("MyWiFi", wifi?.ssid)
    // Check hotspot detection logic (nickname should contain Hotspot)
    assertTrue(hotspot?.nickname?.contains("Hotspot") == true)

    service.stopScanning()
  }
}
