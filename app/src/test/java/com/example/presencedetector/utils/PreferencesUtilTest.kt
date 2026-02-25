package com.example.presencedetector.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
        // Clear shared prefs
        val prefs = context.getSharedPreferences("presence_detector_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        preferencesUtil = PreferencesUtil(context)
    }

    @Test
    fun testSecurityAlertEnabled() {
        assertFalse(preferencesUtil.isSecurityAlertEnabled()) // Default false? Or true? Let's check impl.
        // Logic says SecurityPreferences defaults to false usually unless specified.

        preferencesUtil.setSecurityAlertEnabled(true)
        assertTrue(preferencesUtil.isSecurityAlertEnabled())
    }

    @Test
    fun testPocketModeEnabled() {
        assertFalse(preferencesUtil.isPocketModeEnabled())

        preferencesUtil.setPocketModeEnabled(true)
        assertTrue(preferencesUtil.isPocketModeEnabled())
    }

    @Test
    fun testChargerModeEnabled() {
        assertFalse(preferencesUtil.isChargerModeEnabled())

        preferencesUtil.setChargerModeEnabled(true)
        assertTrue(preferencesUtil.isChargerModeEnabled())
    }

    @Test
    fun testTrustedWifi() {
        // Default might be null or empty depending on impl
        val default = preferencesUtil.getTrustedWifiSsid()
        assertTrue(default.isNullOrEmpty())

        preferencesUtil.setTrustedWifiSsid("MyHomeWiFi")
        assertEquals("MyHomeWiFi", preferencesUtil.getTrustedWifiSsid())
    }

    @Test
    fun testNicknameCaching() {
        val bssid = "00:11:22:33:44:55"
        val nickname = "My Phone"

        // Initially null
        assertEquals(null, preferencesUtil.getNickname(bssid))

        // Save
        preferencesUtil.saveNickname(bssid, nickname)

        // Should return saved value
        assertEquals(nickname, preferencesUtil.getNickname(bssid))

        // Verify persistence (simulate new instance or checking underlying prefs)
        // Since PreferencesUtil uses DeviceInfoPreferences which uses its own cache instance,
        // a new PreferencesUtil(context) will create a new DeviceInfoPreferences(context),
        // which will have an empty cache. This verifies that fallback to SharedPreferences works.
        val newPrefsUtil = PreferencesUtil(context)
        assertEquals(nickname, newPrefsUtil.getNickname(bssid))
    }

    @Test
    fun testManualCategoryCaching() {
        val bssid = "AA:BB:CC:DD:EE:FF"
        val category = com.example.presencedetector.model.DeviceCategory.SMARTPHONE

        // Initially null
        assertEquals(null, preferencesUtil.getManualCategory(bssid))

        // Save
        preferencesUtil.saveManualCategory(bssid, category)

        // Should return saved value
        assertEquals(category, preferencesUtil.getManualCategory(bssid))

        // Verify persistence
        val newPrefsUtil = PreferencesUtil(context)
        assertEquals(category, newPrefsUtil.getManualCategory(bssid))
    }
}
