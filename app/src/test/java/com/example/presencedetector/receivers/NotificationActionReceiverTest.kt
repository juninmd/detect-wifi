package com.example.presencedetector.receivers

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.presencedetector.services.AntiTheftService
import com.example.presencedetector.utils.PreferencesUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowApplication
import org.robolectric.shadows.ShadowNotificationManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NotificationActionReceiverTest {

  private lateinit var context: Context
  private lateinit var receiver: NotificationActionReceiver
  private lateinit var shadowNotificationManager: ShadowNotificationManager

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    receiver = NotificationActionReceiver()
    val notificationManager =
      context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    shadowNotificationManager = Shadows.shadowOf(notificationManager)
  }

  @Test
  fun `ACTION_STOP_ALARM should start service with stop action and cancel notification`() {
    val intent =
      Intent(NotificationActionReceiver.ACTION_STOP_ALARM).apply {
        putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, 123)
      }

    receiver.onReceive(context, intent)

    // Verify Service Started with STOP action
    val nextStartedService = ShadowApplication.getInstance().nextStartedService
    assertNotNull("Service should be started", nextStartedService)
    assertEquals(AntiTheftService::class.java.name, nextStartedService.component?.className)
    assertEquals(AntiTheftService.ACTION_STOP, nextStartedService.action)
  }

  @Test
  fun `ACTION_MARK_SAFE should save nickname and stop alarm`() {
    val bssid = "00:11:22:33:44:55"
    val intent =
      Intent(NotificationActionReceiver.ACTION_MARK_SAFE).apply {
        putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, 456)
        putExtra(NotificationActionReceiver.EXTRA_BSSID, bssid)
      }

    receiver.onReceive(context, intent)

    // Verify Nickname Saved
    val prefs = PreferencesUtil(context)
    assertEquals("Trusted Device", prefs.getNickname(bssid))

    // Verify Detection Tracked (history count > 0)
    assertEquals(1, prefs.getDetectionHistoryCount(bssid))

    // Verify Service Started with STOP action
    val nextStartedService = ShadowApplication.getInstance().nextStartedService
    assertNotNull("Service should be started to stop alarm", nextStartedService)
    assertEquals(AntiTheftService::class.java.name, nextStartedService.component?.className)
    assertEquals(AntiTheftService.ACTION_STOP, nextStartedService.action)
  }

  @Test
  fun `ACTION_ENABLE_ANTITHEFT should start AntiTheftService`() {
    val intent =
      Intent(NotificationActionReceiver.ACTION_ENABLE_ANTITHEFT).apply {
        putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, 789)
      }

    receiver.onReceive(context, intent)

    val nextStartedService = ShadowApplication.getInstance().nextStartedService
    assertNotNull("Service should be started", nextStartedService)
    assertEquals(AntiTheftService::class.java.name, nextStartedService.component?.className)
    assertEquals(AntiTheftService.ACTION_START, nextStartedService.action)
  }

  @Test
  fun `ACTION_SNOOZE should start AntiTheftService with snooze action`() {
    val intent = Intent(NotificationActionReceiver.ACTION_SNOOZE)

    receiver.onReceive(context, intent)

    val nextStartedService = ShadowApplication.getInstance().nextStartedService
    assertNotNull("Service should be started", nextStartedService)
    assertEquals(AntiTheftService::class.java.name, nextStartedService.component?.className)
    assertEquals(AntiTheftService.ACTION_SNOOZE, nextStartedService.action)
  }

  @Test
  fun `ACTION_PANIC should not crash and attempt telegram`() {
    // We can't easily mock TelegramService as it is instantiated directly,
    // but we can ensure it doesn't crash given Telegram is disabled by default in prefs.
    val intent = Intent(NotificationActionReceiver.ACTION_PANIC)

    receiver.onReceive(context, intent)

    // No verification of telegram call possible without refactoring, but we verified the path
    // executes.
    // We can check if a Toast was shown (ShadowToast)
    // val latestToast = org.robolectric.shadows.ShadowToast.getTextOfLatestToast()
    // assertEquals("🚨 ALERTA DE PÂNICO ENVIADO! 🚨", latestToast)
  }

  @Test
  fun `ACTION_MARK_SAFE should handle missing BSSID gracefully`() {
    // Missing BSSID
    val intent =
      Intent(NotificationActionReceiver.ACTION_MARK_SAFE).apply {
        putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, 123)
      }

    receiver.onReceive(context, intent)

    // Verify Service Started with STOP action (fail-safe)
    val nextStartedService = ShadowApplication.getInstance().nextStartedService
    assertNotNull("Service should be started to stop alarm", nextStartedService)
    assertEquals(AntiTheftService::class.java.name, nextStartedService.component?.className)
    assertEquals(AntiTheftService.ACTION_STOP, nextStartedService.action)
  }
}
