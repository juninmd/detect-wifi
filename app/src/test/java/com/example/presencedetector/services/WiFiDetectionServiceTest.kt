package com.example.presencedetector.services

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
import org.robolectric.shadows.ShadowApplication
import org.robolectric.shadows.ShadowWifiManager
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config
import android.Manifest

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
        ShadowApplication.getInstance().denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION)

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
        ShadowApplication.getInstance().grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)

        // Mock ScanResult (API is tricky, need to create via reflection or empty constr if available)
        // ScanResult() is accessible in recent Android versions/Robolectric
        val result1 = ScanResult().apply {
            SSID = "MyWiFi"
            BSSID = "00:11:22:33:44:55"
            level = -50
            frequency = 2400
            capabilities = "[WPA2]"
        }

        val result2 = ScanResult().apply {
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

    @Test
    fun `isLikelyMobileHotspot detects various patterns`() {
        val method = WiFiDetectionService::class.java.getDeclaredMethod("isLikelyMobileHotspot", String::class.java)
        method.isAccessible = true

        assertTrue(method.invoke(service, "iPhone of User") as Boolean)
        assertTrue(method.invoke(service, "AndroidAP") as Boolean)
        assertTrue(method.invoke(service, "Galaxy S21") as Boolean)
        assertTrue(method.invoke(service, "MyHotspot") as Boolean) // "hotspot" pattern

        // Short alphanumeric
        assertTrue(method.invoke(service, "Abc12") as Boolean)

        // Normal WiFi
        org.junit.Assert.assertFalse(method.invoke(service, "Home_WiFi_5G") as Boolean)
        org.junit.Assert.assertFalse(method.invoke(service, "TP-Link_1234") as Boolean)
    }
}
