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
    fun `sendBatteryAlert should post high priority notification`() {
        NotificationUtil.sendBatteryAlert(context, 10)

        val shadowNotificationManager = Shadows.shadowOf(notificationManager)
        assertTrue(shadowNotificationManager.allNotifications.isNotEmpty())
        assertNotNull(shadowNotificationManager.getNotification(2001))
    }
}
