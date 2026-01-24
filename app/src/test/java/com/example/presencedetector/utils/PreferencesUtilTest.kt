package com.example.presencedetector.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.presencedetector.model.DeviceCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
class PreferencesUtilTest {

    private lateinit var context: Context
    private lateinit var preferencesUtil: PreferencesUtil

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        preferencesUtil = PreferencesUtil(context)
        preferencesUtil.clear()
    }

    @Test
    fun testDetectionEnabled() {
        assertFalse(preferencesUtil.isDetectionEnabled()) // Default false
        preferencesUtil.setDetectionEnabled(true)
        assertTrue(preferencesUtil.isDetectionEnabled())
    }

    @Test
    fun testTelegramSettings() {
        assertFalse(preferencesUtil.isTelegramEnabled())
        preferencesUtil.setTelegramEnabled(true)
        assertTrue(preferencesUtil.isTelegramEnabled())

        assertNull(preferencesUtil.getTelegramToken())
        preferencesUtil.setTelegramToken("token123")
        assertEquals("token123", preferencesUtil.getTelegramToken())

        assertNull(preferencesUtil.getTelegramChatId())
        preferencesUtil.setTelegramChatId("chat123")
        assertEquals("chat123", preferencesUtil.getTelegramChatId())
    }

    @Test
    fun testSecuritySchedule() {
        preferencesUtil.setSecuritySchedule("22:00", "06:00")
        val (start, end) = preferencesUtil.getSecuritySchedule()
        assertEquals("22:00", start)
        assertEquals("06:00", end)
    }

    // Testing logic for isCurrentTimeInSecuritySchedule is tricky with real time,
    // but we can test the parsing logic if we could mock the time.
    // For now we trust the setter/getter.

    @Test
    fun testAntiTheftSettings() {
        assertFalse(preferencesUtil.isAntiTheftArmed())
        preferencesUtil.setAntiTheftArmed(true)
        assertTrue(preferencesUtil.isAntiTheftArmed())

        assertEquals(1.5f, preferencesUtil.getAntiTheftSensitivity(), 0.01f)
        preferencesUtil.setAntiTheftSensitivity(2.0f)
        assertEquals(2.0f, preferencesUtil.getAntiTheftSensitivity(), 0.01f)
    }

    @Test
    fun testPocketAndChargerMode() {
        assertFalse(preferencesUtil.isPocketModeEnabled())
        preferencesUtil.setPocketModeEnabled(true)
        assertTrue(preferencesUtil.isPocketModeEnabled())

        assertFalse(preferencesUtil.isChargerModeEnabled())
        preferencesUtil.setChargerModeEnabled(true)
        assertTrue(preferencesUtil.isChargerModeEnabled())
    }

    @Test
    fun testNicknameAndCategory() {
        val bssid = "00:11:22:33:44:55"
        assertNull(preferencesUtil.getNickname(bssid))
        preferencesUtil.saveNickname(bssid, "My Device")
        assertEquals("My Device", preferencesUtil.getNickname(bssid))

        assertNull(preferencesUtil.getManualCategory(bssid))
        preferencesUtil.saveManualCategory(bssid, DeviceCategory.LAPTOP)
        assertEquals(DeviceCategory.LAPTOP, preferencesUtil.getManualCategory(bssid))
    }

    @Test
    fun testLogEventAndHistory() {
        val bssid = "AA:BB:CC:DD:EE:FF"
        preferencesUtil.logEvent(bssid, "Arrived")

        val logs = preferencesUtil.getEventLogs(bssid)
        assertEquals(1, logs.size)
        assertTrue(logs[0].contains("Arrived"))

        // Check if BSSID was added to master list
        val allBssids = preferencesUtil.getAllTrackedBssids()
        assertTrue(allBssids.contains(bssid))

        // Check history count logic
        assertEquals(1, preferencesUtil.getDetectionHistoryCount(bssid))
        // Logging again same day shouldn't increase history count (set logic)
        preferencesUtil.trackDetection(bssid)
        assertEquals(1, preferencesUtil.getDetectionHistoryCount(bssid))
    }

    @Test
    fun testTrustedWifi() {
        assertNull(preferencesUtil.getTrustedWifiSsid())
        preferencesUtil.setTrustedWifiSsid("MyHomeWifi")
        assertEquals("MyHomeWifi", preferencesUtil.getTrustedWifiSsid())
    }

    @Test
    fun testSystemLogs() {
        preferencesUtil.logSystemEvent("Panic Button Pressed")
        val logs = preferencesUtil.getSystemLogs()
        assertEquals(1, logs.size)
        assertTrue(logs[0].contains("Panic Button Pressed"))
    }
}
