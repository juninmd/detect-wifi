package com.example.presencedetector.security.notification

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.presencedetector.security.model.CameraChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class SecurityNotificationManagerTest {

    private lateinit var context: Context
    private lateinit var securityNotificationManager: SecurityNotificationManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        securityNotificationManager = SecurityNotificationManager(context)
    }

    @Test
    fun `test channel creation`() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = notificationManager.getNotificationChannel("security_alerts")

        assertNotNull(channel)
        assertEquals("Alertas de Segurança", channel.name)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
    }

    @Test
    fun `test show detection notification`() {
        val cameraChannel = CameraChannel(
            id = 1,
            name = "Front Door",
            host = "192.168.1.100"
        )

        securityNotificationManager.showDetectionNotification(cameraChannel)

        val notificationManager = Shadows.shadowOf(context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        val notifications = notificationManager.allNotifications

        assertEquals(1, notifications.size)

        val notification = notifications[0]
        assertEquals("🚨 Movimento detectado", notification.extras.getString(android.app.Notification.EXTRA_TITLE))
        assertEquals("Front Door", notification.extras.getString(android.app.Notification.EXTRA_TEXT))
        assertEquals("security_alerts", notification.channelId)
    }

    @Test
    fun `test cancel notification`() {
        val cameraChannel = CameraChannel(
            id = 1,
            name = "Front Door",
            host = "192.168.1.100"
        )

        securityNotificationManager.showDetectionNotification(cameraChannel)
        securityNotificationManager.cancelNotification(1)

        val notificationManager = Shadows.shadowOf(context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        assertEquals(0, notificationManager.allNotifications.size)
    }

    @Test
    fun `test cancel all notifications`() {
        val cameraChannel1 = CameraChannel(id = 1, name = "Cam 1", host = "192.168.1.100")
        val cameraChannel2 = CameraChannel(id = 2, name = "Cam 2", host = "192.168.1.101")

        securityNotificationManager.showDetectionNotification(cameraChannel1)
        securityNotificationManager.showDetectionNotification(cameraChannel2)

        val notificationManager = Shadows.shadowOf(context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        assertEquals(2, notificationManager.allNotifications.size)

        securityNotificationManager.cancelAllNotifications()
        assertEquals(0, notificationManager.allNotifications.size)
    }
}
