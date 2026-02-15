package com.example.presencedetector.utils

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NotificationUtilTest {

  private val context: Context = ApplicationProvider.getApplicationContext()
  private val notificationManager =
    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

  @org.junit.Before
  fun setUp() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val shadowApp = Shadows.shadowOf(app)
    shadowApp.grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
  }

  @Test
  fun testCreateNotificationChannels() {
    NotificationUtil.createNotificationChannels(context)

    val channels = notificationManager.notificationChannels
    assertNotNull(channels)
    assertTrue(channels.isNotEmpty())

    val securityChannel =
      notificationManager.getNotificationChannel(NotificationUtil.SECURITY_CHANNEL_ID)
    assertNotNull(securityChannel)
    assertEquals(NotificationManager.IMPORTANCE_HIGH, securityChannel.importance)

    val homeChannel =
      notificationManager.getNotificationChannel(NotificationUtil.HOME_SECURITY_CHANNEL_ID)
    assertNotNull(homeChannel)

    val mobileChannel =
      notificationManager.getNotificationChannel(NotificationUtil.MOBILE_SECURITY_CHANNEL_ID)
    assertNotNull(mobileChannel)
  }

  @Test
  fun testSendPresenceNotification_Important() {
    NotificationUtil.sendPresenceNotification(context, "Title", "Message", isImportantEvent = true)

    val shadows = Shadows.shadowOf(notificationManager)
    assertEquals(1, shadows.allNotifications.size)

    val notification = shadows.allNotifications[0]
    assertEquals(NotificationUtil.INFO_CHANNEL_ID, notification.channelId)
  }

  @Test
  fun testSendPresenceNotification_Silent() {
    NotificationUtil.sendPresenceNotification(context, "Title", "Message", isImportantEvent = false)

    val shadows = Shadows.shadowOf(notificationManager)
    assertEquals(1, shadows.allNotifications.size)

    val notification = shadows.allNotifications[0]
    assertEquals(NotificationUtil.SILENT_CHANNEL_ID, notification.channelId)
  }

  @Test
  fun testSendCriticalAlert() {
    NotificationUtil.sendCriticalAlert(context, "Alert", "Intruder", 123)

    val shadows = Shadows.shadowOf(notificationManager)
    val notification = shadows.getNotification(123)
    assertNotNull(notification)
    assertEquals(NotificationUtil.SECURITY_CHANNEL_ID, notification.channelId)

    // Verify Mark as Safe action
    val actions = notification.actions
    assertTrue(
      "Should have Mark as Safe action",
      actions.any { it.title.toString().contains("Mark") || it.title.toString().contains("Seguro") }
    )
  }

  @Test
  fun testSendPanicAlert() {
    NotificationUtil.sendPanicAlert(context)

    val shadows = Shadows.shadowOf(notificationManager)
    // Panic alert uses ID 1000 in Util
    val notification = shadows.getNotification(1000)
    assertNotNull(notification)

    val actions = notification.actions
    assertTrue(
      "Should have Mark as Safe action",
      actions.any { it.title.toString().contains("Seguro") }
    )
  }

  @Test
  fun testSendBatteryAlert() {
    NotificationUtil.sendBatteryAlert(context, 10)

    val shadows = Shadows.shadowOf(notificationManager)
    val notification = shadows.getNotification(2001) // Fixed ID in Util
    assertNotNull(notification)
    assertEquals(NotificationUtil.BATTERY_CHANNEL_ID, notification.channelId)
  }

  @Test
  fun testCreateForegroundNotification() {
    val notification =
      NotificationUtil.createForegroundNotification(
        context,
        "Title",
        "Message",
        NotificationUtil.CHANNEL_ID
      )
    assertNotNull(notification)
    assertEquals(NotificationUtil.CHANNEL_ID, notification.channelId)
  }
}
