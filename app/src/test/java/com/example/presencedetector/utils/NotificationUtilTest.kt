package com.example.presencedetector.utils

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotificationUtilTest {

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @Test
    fun testChannelCreation() {
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
    }

    @Test
    fun testCreateForegroundNotification() {
        val notification = NotificationUtil.createForegroundNotification(context, "Title", "Subtitle")
        assertNotNull(notification)
    }

    @Test
    fun testSendPresenceNotification() {
        // Just verify it doesn't crash and potentially check if a notification was posted via Shadows in a fuller test suite
        NotificationUtil.sendPresenceNotification(
            context,
            "Title",
            "Message",
            isImportantEvent = false
        )
        // With Robolectric, we can inspect ShadowNotificationManager to see if notify was called,
        // but for now we assume no exception means code paths were executed.
    }
}
