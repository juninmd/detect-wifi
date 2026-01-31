package com.example.presencedetector.services

import android.content.Context
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowWifiManager
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class WiFiHotspotDetectionServiceTest {

    private lateinit var context: Context
    private lateinit var service: WiFiHotspotDetectionService
    private lateinit var wifiManager: WifiManager
    private lateinit var shadowWifiManager: ShadowWifiManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        shadowWifiManager = shadowOf(wifiManager)
        service = WiFiHotspotDetectionService(context)
    }

    @After
    fun tearDown() {
        service.destroy()
    }

    @Test
    fun `startDetection starts scanning and detects hotspot`() {
        var detected = false
        var count = 0

        val listener = object : WiFiHotspotDetectionService.HotspotListener {
            override fun onHotspotDetected(ssid: String, bssid: String, signal: Int) {
                if (ssid == "iPhone 12") detected = true
            }

            override fun onHotspotsUpdated(c: Int) {
                count = c
            }
        }

        service.setHotspotListener(listener)

        val result1 = ScanResult().apply {
            SSID = "iPhone 12"
            BSSID = "00:11:22:33:44:55"
            level = -50
        }
        val result2 = ScanResult().apply {
            SSID = "Home Wifi"
            BSSID = "AA:BB:CC:DD:EE:FF"
            level = -40
        }

        shadowWifiManager.setScanResults(listOf(result1, result2))

        service.startDetection()
        assertTrue(service.isScanning())

        // Advance the looper to handle the coroutine delay and execution
        shadowOf(Looper.getMainLooper()).idleFor(6000, TimeUnit.MILLISECONDS)

        assertTrue("Should detect iPhone", detected)
        assertEquals("Should have 1 detected hotspot", 1, service.getDetectedHotspotCount())

        service.stopDetection()
        assertFalse(service.isScanning())
    }

    @Test
    fun `does not detect regular wifi`() {
        var detected = false
        val listener = object : WiFiHotspotDetectionService.HotspotListener {
            override fun onHotspotDetected(ssid: String, bssid: String, signal: Int) {
                detected = true
            }

            override fun onHotspotsUpdated(count: Int) {}
        }
        service.setHotspotListener(listener)

        val result1 = ScanResult().apply {
            SSID = "Netgear"
            BSSID = "00:11:22:33:44:55"
            level = -50
        }
        shadowWifiManager.setScanResults(listOf(result1))

        service.startDetection()

        shadowOf(Looper.getMainLooper()).idleFor(6000, TimeUnit.MILLISECONDS)

        assertFalse("Should not detect Netgear", detected)
    }
}
