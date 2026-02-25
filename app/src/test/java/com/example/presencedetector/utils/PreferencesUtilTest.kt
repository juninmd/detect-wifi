package com.example.presencedetector.utils

import android.content.Context
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
    assertFalse(
      preferencesUtil.isSecurityAlertEnabled()
    ) // Default false? Or true? Let's check impl.
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
}
