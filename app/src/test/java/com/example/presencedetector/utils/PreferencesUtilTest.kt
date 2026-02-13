package com.example.presencedetector.utils

import android.content.Context
import android.content.SharedPreferences
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
  fun `test notification settings`() {
    // notifyOnPresence
    assertTrue(preferencesUtil.shouldNotifyOnPresence()) // Default true
    preferencesUtil.setNotifyOnPresence(false)
    assertFalse(preferencesUtil.shouldNotifyOnPresence())

    // notifyWifiArrival
    assertFalse(preferencesUtil.shouldNotifyWifiArrival()) // Default false
    preferencesUtil.setNotifyWifiArrival(true)
    assertTrue(preferencesUtil.shouldNotifyWifiArrival())
  }

  @Test
  fun `test per bssid notification settings`() {
    val bssid = "00:11:22:33:44:55"

    // Notify Arrival
    assertFalse(preferencesUtil.shouldNotifyArrival(bssid))
    preferencesUtil.setNotifyArrival(bssid, true)
    assertTrue(preferencesUtil.shouldNotifyArrival(bssid))

    // Notify Departure
    assertFalse(preferencesUtil.shouldNotifyDeparture(bssid))
    preferencesUtil.setNotifyDeparture(bssid, true)
    assertTrue(preferencesUtil.shouldNotifyDeparture(bssid))

    // Critical Alert
    assertFalse(preferencesUtil.isCriticalAlertEnabled(bssid))
    preferencesUtil.setCriticalAlertEnabled(bssid, true)
    assertTrue(preferencesUtil.isCriticalAlertEnabled(bssid))

    // Telegram Alert
    assertFalse(preferencesUtil.isTelegramAlertEnabled(bssid))
    preferencesUtil.setTelegramAlertEnabled(bssid, true)
    assertTrue(preferencesUtil.isTelegramAlertEnabled(bssid))
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
  fun `test security settings`() {
    // Security Alert
    assertFalse(preferencesUtil.isSecurityAlertEnabled())
    preferencesUtil.setSecurityAlertEnabled(true)
    assertTrue(preferencesUtil.isSecurityAlertEnabled())

    // Security Sound
    assertFalse(preferencesUtil.isSecuritySoundEnabled())
    preferencesUtil.setSecuritySoundEnabled(true)
    assertTrue(preferencesUtil.isSecuritySoundEnabled())

    // Schedule
    val (defaultStart, defaultEnd) = preferencesUtil.getSecuritySchedule()
    assertEquals("22:00", defaultStart)
    assertEquals("06:00", defaultEnd)

    preferencesUtil.setSecuritySchedule("23:00", "07:00")
    val (start, end) = preferencesUtil.getSecuritySchedule()
    assertEquals("23:00", start)
    assertEquals("07:00", end)
  }

  @Test
  fun `test anti theft settings`() {
    assertFalse(preferencesUtil.isAntiTheftArmed())
    preferencesUtil.setAntiTheftArmed(true)
    assertTrue(preferencesUtil.isAntiTheftArmed())

    assertEquals(1.5f, preferencesUtil.getAntiTheftSensitivity(), 0.0f)
    preferencesUtil.setAntiTheftSensitivity(2.5f)
    assertEquals(2.5f, preferencesUtil.getAntiTheftSensitivity(), 0.01f)
  }

  @Test
  fun `test biometric and lock settings`() {
    assertFalse(preferencesUtil.isBiometricEnabled())
    preferencesUtil.setBiometricEnabled(true)
    assertTrue(preferencesUtil.isBiometricEnabled())

    assertFalse(preferencesUtil.isAppLockEnabled())
    preferencesUtil.setAppLockEnabled(true)
    assertTrue(preferencesUtil.isAppLockEnabled())
  }

  @Test
  fun `test mode settings`() {
    assertFalse(preferencesUtil.isPocketModeEnabled())
    preferencesUtil.setPocketModeEnabled(true)
    assertTrue(preferencesUtil.isPocketModeEnabled())

    assertFalse(preferencesUtil.isChargerModeEnabled())
    preferencesUtil.setChargerModeEnabled(true)
    assertTrue(preferencesUtil.isChargerModeEnabled())
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

    // Test invalid
    val prefs = context.getSharedPreferences("presence_detector_prefs", Context.MODE_PRIVATE)
    prefs.edit().putString("category_" + bssid, "INVALID_CATEGORY").commit()
    assertNull(preferencesUtil.getManualCategory(bssid))
  }


  @Test
  fun `test trusted wifi`() {
    assertNull(preferencesUtil.getTrustedWifiSsid())
    preferencesUtil.setTrustedWifiSsid("MyHomeWifi")
    assertEquals("MyHomeWifi", preferencesUtil.getTrustedWifiSsid())
  }

  @Test
  fun `test listener registration`() {
    // This is tricky to test with real listeners as they are weak references or platform
    // implementations
    // But we can verify no crash
    val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> }
    preferencesUtil.registerListener(listener)
    preferencesUtil.unregisterListener(listener)
  }

  @Test
  fun `isCurrentTimeInSecuritySchedule should work for day schedule`() {
    // Set a day schedule that covers the entire day
    preferencesUtil.setSecuritySchedule("00:00", "23:59")
    assertTrue(
      "Should be in schedule for full day range",
      preferencesUtil.isCurrentTimeInSecuritySchedule()
    )
  }

  @Test
  fun `isCurrentTimeInSecuritySchedule should work for overnight schedule`() {
    // Set an overnight schedule that effectively covers everything
    // 00:01 > 00:00 is true.
    // If now is 12:00. 12:00 >= 00:01 (True).
    preferencesUtil.setSecuritySchedule("00:01", "00:00")
    assertTrue(
      "Should be in schedule for overnight full coverage",
      preferencesUtil.isCurrentTimeInSecuritySchedule()
    )
  }

  @Test
  fun `isCurrentTimeInSecuritySchedule should detect out of schedule`() {
    // Set schedule to a time definitely in the future/past relative to now
    val now =
      java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
    val hour = now.split(":")[0].toInt()

    // Pick a 1-hour window 2 hours from now
    val nextHour = (hour + 2) % 24
    val nextNextHour = (hour + 3) % 24

    val start = String.format("%02d:00", nextHour)
    val end = String.format("%02d:00", nextNextHour)

    preferencesUtil.setSecuritySchedule(start, end)
    assertFalse("Should be out of schedule", preferencesUtil.isCurrentTimeInSecuritySchedule())
  }
}
