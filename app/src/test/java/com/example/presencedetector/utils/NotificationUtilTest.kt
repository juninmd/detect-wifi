package com.example.presencedetector.utils

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class NotificationUtilTest {

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @Test
    fun `createNotificationChannels should create all channels`() {
        NotificationUtil.createNotificationChannels(context)

        val serviceChannel = notificationManager.getNotificationChannel(NotificationUtil.CHANNEL_ID)
        assertNotNull(serviceChannel)
        assertEquals(NotificationManager.IMPORTANCE_LOW, serviceChannel.importance)

        val infoChannel = notificationManager.getNotificationChannel(NotificationUtil.INFO_CHANNEL_ID)
        assertNotNull(infoChannel)
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, infoChannel.importance)

        val alertChannel = notificationManager.getNotificationChannel(NotificationUtil.ALERT_CHANNEL_ID)
        assertNotNull(alertChannel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, alertChannel.importance)
        assertTrue(alertChannel.canBypassDnd())

        val homeChannel = notificationManager.getNotificationChannel(NotificationUtil.HOME_SECURITY_CHANNEL_ID)
        assertNotNull(homeChannel)

        val mobileChannel = notificationManager.getNotificationChannel(NotificationUtil.MOBILE_SECURITY_CHANNEL_ID)
        assertNotNull(mobileChannel)
    }

    @Test
    fun `sendPresenceNotification should post notification`() {
        NotificationUtil.sendPresenceNotification(context, "Title", "Message", false)

        val shadowNotificationManager = Shadows.shadowOf(notificationManager)
        assertEquals(1, shadowNotificationManager.size())

        val notification = shadowNotificationManager.allNotifications[0]
        assertEquals("Title", Shadows.shadowOf(notification).contentTitle)
        assertEquals("Message", Shadows.shadowOf(notification).contentText)
    }

    @Test
    fun `sendPresenceNotification with importance true should use info channel`() {
        NotificationUtil.sendPresenceNotification(context, "Title", "Message", true)

        val shadowNotificationManager = Shadows.shadowOf(notificationManager)
        val notification = shadowNotificationManager.allNotifications[0]

        assertEquals(NotificationUtil.INFO_CHANNEL_ID, notification.channelId)
    }

    @Test
    fun `sendPresenceNotification with actions should add actions`() {
        val intent = android.app.PendingIntent.getBroadcast(context, 0, android.content.Intent("ACTION"), android.app.PendingIntent.FLAG_IMMUTABLE)

        NotificationUtil.sendPresenceNotification(
            context, "Title", "Message", false,
            actionTitle = "Action1", actionIntent = intent,
            secondActionTitle = "Action2", secondActionIntent = intent
        )

        val shadowNotificationManager = Shadows.shadowOf(notificationManager)
        val notification = shadowNotificationManager.allNotifications[0]

        assertEquals(2, notification.actions.size)
        assertEquals("Action1", notification.actions[0].title)
        assertEquals("Action2", notification.actions[1].title)
    }

    @Test
    fun `sendCriticalAlert should post high priority notification`() {
        NotificationUtil.sendCriticalAlert(context, "Alert", "Critical!", 999)

        val shadowNotificationManager = Shadows.shadowOf(notificationManager)
        val notification = shadowNotificationManager.getNotification(999)
        assertNotNull(notification)
        assertEquals(NotificationUtil.SECURITY_CHANNEL_ID, notification.channelId)

        val shadowNotif = Shadows.shadowOf(notification)
        assertEquals("Alert", shadowNotif.contentTitle)
    }

    @Test
    fun `createForegroundNotification should return non-null notification`() {
        val notification = NotificationUtil.createForegroundNotification(context)
        assertNotNull(notification)
        assertEquals(NotificationUtil.CHANNEL_ID, notification.channelId)
    }

    @Test
    fun `sendBatteryAlert should post high priority notification`() {
        NotificationUtil.sendBatteryAlert(context, 10)

        val shadowNotificationManager = Shadows.shadowOf(notificationManager)
        assertTrue(shadowNotificationManager.allNotifications.isNotEmpty())
        assertNotNull(shadowNotificationManager.getNotification(2001))
    }
}
