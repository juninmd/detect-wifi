package com.example.presencedetector.services

import android.Manifest
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowApplication

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DetectionBackgroundServiceTest {

  @Test
  fun `service should start and return START_STICKY`() {
    val app = ShadowApplication.getInstance()
    app.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)

    val controller = Robolectric.buildService(DetectionBackgroundService::class.java)
    val service = controller.create().get()

    val result =
      service.onStartCommand(
        Intent(ApplicationProvider.getApplicationContext(), DetectionBackgroundService::class.java),
        0,
        0
      )

    assert(result == android.app.Service.START_STICKY)
  }

  @Test
  fun `service should startForeground with notification`() {
    val app = ShadowApplication.getInstance()
    app.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)

    val controller = Robolectric.buildService(DetectionBackgroundService::class.java)
    val service = controller.create().get()

    service.onStartCommand(
      Intent(ApplicationProvider.getApplicationContext(), DetectionBackgroundService::class.java),
      0,
      0
    )

    val notification = org.robolectric.Shadows.shadowOf(service).lastForegroundNotification
    assert(notification != null)
    assert(
      notification.channelId ==
        com.example.presencedetector.utils.NotificationUtil.HOME_SECURITY_CHANNEL_ID
    )
  }
}
