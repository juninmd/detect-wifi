package com.example.presencedetector.utils

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.example.presencedetector.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNotificationManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NotificationUtilTest {

  private lateinit var context: Context
  private lateinit var notificationManager: NotificationManager
  private lateinit var shadowNotificationManager: ShadowNotificationManager

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    shadowNotificationManager = Shadows.shadowOf(notificationManager)
  }

  @Test
  fun testCreateNotificationChannelsAPI25() {
    // Should do nothing gracefully for APIs < 26
    val oldVersion = android.os.Build.VERSION.SDK_INT
    try {
      org.robolectric.util.ReflectionHelpers.setStaticField(android.os.Build.VERSION::class.java, "SDK_INT", 25)

      // Before execution
      val previousSize = notificationManager.notificationChannels.size

      NotificationUtil.createNotificationChannels(context)

      // Verify size did not change
      assertEquals(previousSize, notificationManager.notificationChannels.size)
    } finally {
      org.robolectric.util.ReflectionHelpers.setStaticField(android.os.Build.VERSION::class.java, "SDK_INT", oldVersion)
    }
  }

  @Test
  fun testCreateNotificationChannels() {
    NotificationUtil.createNotificationChannels(context)

    val channelId = NotificationUtil.CHANNEL_ID
    val channel = notificationManager.getNotificationChannel(channelId)

    assertNotNull("Channel should be created", channel)
    assertEquals(context.getString(R.string.channel_service_name), channel?.name)
    assertEquals(NotificationManager.IMPORTANCE_LOW, channel?.importance)
    assertEquals(context.getString(R.string.channel_service_desc), channel?.description)

    val infoChannel = notificationManager.getNotificationChannel(NotificationUtil.INFO_CHANNEL_ID)
    assertNotNull(infoChannel)
    assertEquals(context.getString(R.string.channel_info_name), infoChannel?.name)
    assertEquals(context.getString(R.string.channel_info_desc), infoChannel?.description)
    assertEquals(true, infoChannel?.shouldShowLights())
    assertEquals(android.graphics.Color.BLUE, infoChannel?.lightColor)

    val silentChannel = notificationManager.getNotificationChannel(NotificationUtil.SILENT_CHANNEL_ID)
    assertNotNull(silentChannel)
    assertEquals("Eventos Silenciosos", silentChannel?.name)
    assertEquals("Notificações de rotina sem som", silentChannel?.description)
    assertEquals(false, silentChannel?.canShowBadge())

    val securityChannel = notificationManager.getNotificationChannel(NotificationUtil.SECURITY_CHANNEL_ID)
    assertNotNull(securityChannel)
    assertEquals("Alerta de Segurança Crítico", securityChannel?.name)
    assertEquals("Alertas de intrusão e roubo. Toca mesmo em modo não perturbe.", securityChannel?.description)
    assertEquals(true, securityChannel?.shouldVibrate())
    assertEquals(android.graphics.Color.RED, securityChannel?.lightColor)
    // AudioAttributes not accessible directly but bypass dnd is
    assertEquals(true, securityChannel?.canBypassDnd())
    assertEquals(Notification.VISIBILITY_PUBLIC, securityChannel?.lockscreenVisibility)

    val batteryChannel = notificationManager.getNotificationChannel(NotificationUtil.BATTERY_CHANNEL_ID)
    assertNotNull(batteryChannel)
    assertEquals(context.getString(R.string.channel_battery_name), batteryChannel?.name)
    assertEquals(context.getString(R.string.channel_battery_desc), batteryChannel?.description)
    assertEquals(true, batteryChannel?.shouldVibrate())
    assertEquals(android.graphics.Color.YELLOW, batteryChannel?.lightColor)

    val homeChannel = notificationManager.getNotificationChannel(NotificationUtil.HOME_SECURITY_CHANNEL_ID)
    assertNotNull(homeChannel)
    assertEquals("Segurança Residencial", homeChannel?.name)
    assertEquals("Notificações de monitoramento WiFi e presença em casa", homeChannel?.description)
    assertEquals(false, homeChannel?.canShowBadge())

    val mobileChannel = notificationManager.getNotificationChannel(NotificationUtil.MOBILE_SECURITY_CHANNEL_ID)
    assertNotNull(mobileChannel)
    assertEquals("Segurança do Celular", mobileChannel?.name)
    assertEquals("Notificações de monitoramento anti-furto (bolso, movimento)", mobileChannel?.description)
    assertEquals(true, mobileChannel?.canShowBadge())

    val alertsChannel = notificationManager.getNotificationChannel(NotificationUtil.SECURITY_ALERTS_CHANNEL_ID)
    assertNotNull(alertsChannel)
    assertEquals("Alertas de Segurança", alertsChannel?.name)
    assertEquals("Notificações quando uma pessoa é detectada nas câmeras", alertsChannel?.description)
    assertEquals(true, alertsChannel?.shouldVibrate())
    assertEquals(true, alertsChannel?.shouldShowLights())
  }

  @Test
  fun testSendPresenceNotification() {
    val shadowApp = Shadows.shadowOf(context as android.app.Application)
    shadowApp.grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)

    NotificationUtil.sendPresenceNotification(
      context = context,
      title = "Test Title",
      message = "Test Message",
      isImportantEvent = true,
      notificationId = 123
    )

    val notifications = shadowNotificationManager.allNotifications
    assertEquals("One notification should be shown", 1, notifications.size)

    val notification = notifications[0]
    val title = notification.extras.getString(Notification.EXTRA_TITLE)
    val message = notification.extras.getString(Notification.EXTRA_TEXT)

    assertEquals("Test Title", title)
    assertEquals("Test Message", message)
    assertEquals(NotificationUtil.INFO_CHANNEL_ID, notification.channelId)
  }

  @Test
  fun testSendCriticalAlert() {
    val shadowApp = Shadows.shadowOf(context as android.app.Application)
    shadowApp.grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)

    NotificationUtil.sendCriticalAlert(
      context = context,
      title = "Critical Title",
      message = "Critical Message",
      notificationId = 456
    )

    val notifications = shadowNotificationManager.allNotifications
    assertEquals("One notification should be shown", 1, notifications.size)

    val notification = notifications[0]
    val title = notification.extras.getString(Notification.EXTRA_TITLE)
    val message = notification.extras.getString(Notification.EXTRA_TEXT)

    assertEquals("Critical Title", title)
    assertEquals("Critical Message", message)
    assertEquals(NotificationUtil.SECURITY_CHANNEL_ID, notification.channelId)
    // Verify Mark as Safe action exists
    val actions = notification.actions
    assertNotNull("Actions should exist", actions)
    assertEquals(true, actions.any { it.title == "Marcar Seguro" })
  }

  @Test
  fun testSendBatteryAlert() {
    val shadowApp = Shadows.shadowOf(context as android.app.Application)
    shadowApp.grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)

    NotificationUtil.sendBatteryAlert(context, 15)

    val notifications = shadowNotificationManager.allNotifications
    assertEquals("One notification should be shown", 1, notifications.size)

    val notification = notifications[0]
    val title = notification.extras.getString(Notification.EXTRA_TITLE)
    val message = notification.extras.getString(Notification.EXTRA_TEXT)

    assertEquals(context.getString(R.string.notif_battery_warning), title)
    assertEquals(context.getString(R.string.notif_battery_desc, 15), message)
    assertEquals(NotificationUtil.BATTERY_CHANNEL_ID, notification.channelId)
  }

  @Test
  fun testSendPanicAlert() {
    val shadowApp = Shadows.shadowOf(context as android.app.Application)
    shadowApp.grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)

    NotificationUtil.sendPanicAlert(context)

    val notifications = shadowNotificationManager.allNotifications
    assertEquals("One notification should be shown", 1, notifications.size)

    val notification = notifications[0]
    val title = notification.extras.getString(Notification.EXTRA_TITLE)
    val message = notification.extras.getString(Notification.EXTRA_TEXT)

    assertEquals("🆘 ALARME DE PÂNICO ATIVO", title)
    assertEquals("Sua segurança está em risco? A ajuda está a um toque.", message)
    assertEquals(NotificationUtil.SECURITY_CHANNEL_ID, notification.channelId)
    // Has 3 actions
    assertEquals(3, notification.actions?.size)
  }

  @Test
  fun testCreateForegroundNotification() {
    val notification = NotificationUtil.createForegroundNotification(
      context,
      "Foreground Title",
      "Foreground Message",
      NotificationUtil.CHANNEL_ID
    )

    assertNotNull("Notification should be created", notification)
    val title = notification.extras.getString(Notification.EXTRA_TITLE)
    val message = notification.extras.getString(Notification.EXTRA_TEXT)

    assertEquals("Foreground Title", title)
    assertEquals("Foreground Message", message)
    // Ongoing flag should be set for foreground notifications
    assertEquals(true, notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
  }

  @Test
  fun testCheckPermission() {
    val shadowApp = Shadows.shadowOf(context as android.app.Application)

    // Without permission
    shadowApp.denyPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
    assertEquals(false, NotificationUtil.checkPermission(context))

    // With permission
    shadowApp.grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
    assertEquals(true, NotificationUtil.checkPermission(context))
  }

  @Test
  fun testSendIntruderAlert() {
    val shadowApp = Shadows.shadowOf(context as android.app.Application)
    shadowApp.grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)

    val bitmap = android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888)

    NotificationUtil.sendIntruderAlert(context, bitmap)

    val notifications = shadowNotificationManager.allNotifications
    assertEquals("One notification should be shown", 1, notifications.size)

    val notification = notifications[0]
    val title = notification.extras.getString(Notification.EXTRA_TITLE)
    val message = notification.extras.getString(Notification.EXTRA_TEXT)

    assertEquals("🚨 INTRUSO DETECTADO!", title)
    assertEquals("Uma foto foi capturada durante o alerta de segurança.", message)
    assertEquals(NotificationUtil.SECURITY_CHANNEL_ID, notification.channelId)
    // Has 2 actions: Ligar 190 and Marcar Seguro
    assertEquals(2, notification.actions?.size)
  }
}
