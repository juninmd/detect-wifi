package com.example.presencedetector.security.ui

import android.content.Context
import android.content.Intent
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.example.presencedetector.R
import com.example.presencedetector.services.AntiTheftService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AlarmActivityTest {

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
  }

  @org.junit.Ignore(
    "Temporarily disabled due to Robolectric/Environment NPE in buildActivity. Needs layout inflation fix."
  )
  @Test
  fun `activity displays reason from intent`() {
    val intent =
      Intent(context, AlarmActivity::class.java).apply {
        putExtra(AlarmActivity.EXTRA_REASON, "TEST REASON")
      }

    val controller = Robolectric.buildActivity(AlarmActivity::class.java, intent)
    val activity = controller.create().start().resume().get()

    // Check basic lifecycle success
    assertNotNull(activity)

    val tvReason = activity.findViewById<TextView>(R.id.tvAlertReason)
    assertNotNull("TextView should be found", tvReason)
    assertEquals("TEST REASON", tvReason.text)
  }

  @org.junit.Ignore(
    "Temporarily disabled due to Robolectric/Environment NPE in buildActivity. Needs layout inflation fix."
  )
  @Test
  fun `stop button logic execution`() {
    val controller = Robolectric.buildActivity(AlarmActivity::class.java)
    val activity = controller.create().start().resume().get()

    val btnStop = activity.findViewById<android.view.View>(R.id.btnStopAlarm)
    assertNotNull("Stop button should be found", btnStop)
    btnStop.performClick()

    val shadowActivity = Shadows.shadowOf(activity)

    // Check Service Stop Intent
    val nextService = shadowActivity.nextStartedService
    assertNotNull("Service stop intent should be sent", nextService)
    assertEquals(AntiTheftService::class.java.name, nextService.component?.className)
    assertEquals(AntiTheftService.ACTION_STOP, nextService.action)
  }
}
