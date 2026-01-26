package com.example.presencedetector.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.presencedetector.model.DeviceCategory
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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
    fun `test detection enabled flag`() {
        assertFalse(preferencesUtil.isDetectionEnabled())
        preferencesUtil.setDetectionEnabled(true)
        assertTrue(preferencesUtil.isDetectionEnabled())
    }

    @Test
    fun `test telegram settings`() {
        assertFalse(preferencesUtil.isTelegramEnabled())
        preferencesUtil.setTelegramEnabled(true)
        assertTrue(preferencesUtil.isTelegramEnabled())

        preferencesUtil.setTelegramToken("token123")
        assertEquals("token123", preferencesUtil.getTelegramToken())

        preferencesUtil.setTelegramChatId("chat123")
        assertEquals("chat123", preferencesUtil.getTelegramChatId())
    }

    @Test
    fun `test anti theft settings`() {
        assertFalse(preferencesUtil.isAntiTheftArmed())
        preferencesUtil.setAntiTheftArmed(true)
        assertTrue(preferencesUtil.isAntiTheftArmed())

        preferencesUtil.setAntiTheftSensitivity(2.5f)
        assertEquals(2.5f, preferencesUtil.getAntiTheftSensitivity(), 0.01f)
    }

    @Test
    fun `test nickname storage`() {
        val bssid = "00:11:22:33:44:55"
        assertNull(preferencesUtil.getNickname(bssid))

        preferencesUtil.saveNickname(bssid, "My Phone")
        assertEquals("My Phone", preferencesUtil.getNickname(bssid))
    }

    @Test
    fun `test manual category`() {
        val bssid = "AA:BB:CC:DD:EE:FF"
        assertNull(preferencesUtil.getManualCategory(bssid))

        preferencesUtil.saveManualCategory(bssid, DeviceCategory.SMART_TV)
        assertEquals(DeviceCategory.SMART_TV, preferencesUtil.getManualCategory(bssid))
    }

    @Test
    fun `test log event`() {
        val bssid = "11:22:33:44:55:66"
        preferencesUtil.logEvent(bssid, "Arrived")

        val logs = preferencesUtil.getEventLogs(bssid)
        assertEquals(1, logs.size)
        assertTrue(logs[0].contains("Arrived"))

        // Test system logs
        preferencesUtil.logSystemEvent("System Start")
        val sysLogs = preferencesUtil.getSystemLogs()
        assertEquals(1, sysLogs.size)
        assertTrue(sysLogs[0].contains("System Start"))
    }

    @Test
    fun `test security schedule`() {
        preferencesUtil.setSecuritySchedule("23:00", "07:00")
        val (start, end) = preferencesUtil.getSecuritySchedule()
        assertEquals("23:00", start)
        assertEquals("07:00", end)
    }
}
