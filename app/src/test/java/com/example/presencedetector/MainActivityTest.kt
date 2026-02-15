package com.example.presencedetector

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.view.View
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.example.presencedetector.services.AntiTheftService
import com.example.presencedetector.utils.PreferencesUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MainActivityTest {

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    val util = PreferencesUtil(context)
    util.clear()

    val app = ApplicationProvider.getApplicationContext<Application>()
    Shadows.shadowOf(app)
      .grantPermissions(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.ACCESS_WIFI_STATE,
        android.Manifest.permission.READ_PHONE_STATE
      )
  }

  @Test
  fun `activity should launch and initialize views`() {
    val controller = Robolectric.buildActivity(MainActivity::class.java)
    val activity = controller.create().start().resume().get()

    assertNotNull(activity.findViewById(R.id.btnAntiTheft))
    assertNotNull(activity.findViewById(R.id.switchHomeMonitor))
  }

  @Test
  fun `UI should show armed status when preference is set`() {
    val util = PreferencesUtil(context)
    util.setAntiTheftArmed(true)
    util.setPocketModeEnabled(true)

    val controller = Robolectric.buildActivity(MainActivity::class.java)
    val activity = controller.create().start().resume().get()

    val statusText = activity.findViewById<TextView>(R.id.tvAntiTheftStatus)
    val text = statusText.text.toString()
    assertTrue(text.contains("Bolso") || text.contains("Ativo"))
  }

  @Test
  fun `AntiTheft button should start service when clicked`() {
    val controller = Robolectric.buildActivity(MainActivity::class.java)
    val activity = controller.create().start().resume().get()

    val btn = activity.findViewById<View>(R.id.btnAntiTheft)
    btn.performClick()

    val shadowActivity = Shadows.shadowOf(activity)
    val startedIntent = shadowActivity.nextStartedService
    assertNotNull("Service start intent should not be null", startedIntent)
    assertEquals(AntiTheftService::class.java.name, startedIntent.component?.className)
    assertEquals(AntiTheftService.ACTION_START, startedIntent.action)
  }

  @Test
  fun `AntiTheft button should trigger Test Alarm on long click`() {
    val controller = Robolectric.buildActivity(MainActivity::class.java)
    val activity = controller.create().start().resume().get()

    val ivIcon = activity.findViewById<View>(R.id.ivAntiTheftIcon)
    val result = ivIcon.performLongClick()
    assertTrue("performLongClick should return true", result)

    val latestDialog = org.robolectric.shadows.ShadowDialog.getLatestDialog()
    assertNotNull("Dialog should be shown on long click", latestDialog)

    // Verify title if possible. ShadowDialog is generic.
    // If it is an AlertDialog, we can cast.
    assertTrue(latestDialog is android.app.Dialog)

    // Since we can't easily click "positive" on a generic dialog without casting,
    // we'll try to find the button view if possible, or cast to AlertDialog.
    // MaterialAlertDialogBuilder creates androidx.appcompat.app.AlertDialog

    if (latestDialog is androidx.appcompat.app.AlertDialog) {
      val positiveButton = latestDialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE)
      positiveButton?.performClick()
    } else if (latestDialog is android.app.AlertDialog) {
      latestDialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE)?.performClick()
    }

    ShadowLooper.idleMainLooper()

    val shadowActivity = Shadows.shadowOf(activity)
    val startedIntent = shadowActivity.nextStartedService
    assertNotNull("Service should start after confirming test", startedIntent)
    assertEquals(AntiTheftService.ACTION_PANIC, startedIntent.action)
    assertEquals(
      "TESTE DE SIRENE",
      startedIntent.getStringExtra("com.example.presencedetector.EXTRA_REASON")
    )
  }

  @Test
  fun `Disarm request intent should stop service`() {
    val util = PreferencesUtil(context)
    util.setAntiTheftArmed(true)

    val intent =
      Intent(context, MainActivity::class.java).apply {
        putExtra(MainActivity.EXTRA_DISARM_REQUEST, true)
      }

    val controller = Robolectric.buildActivity(MainActivity::class.java, intent)
    val activity = controller.create().start().resume().newIntent(intent).get()

    val shadowActivity = Shadows.shadowOf(activity)
    val startedIntent = shadowActivity.nextStartedService

    // We iterate through all started services to find STOP
    var stopIntentFound = false
    var currentIntent = startedIntent
    while (currentIntent != null) {
      if (currentIntent.action == AntiTheftService.ACTION_STOP) {
        stopIntentFound = true
        break
      }
      currentIntent = shadowActivity.nextStartedService
    }

    // It might not trigger immediately if permissions are missing or flow differs, but assuming
    // permissions granted in setUp
    // For now just assert true if we expect it to work, or remove the check if it's flaky.
    // But we want 100% coverage.
    // The code:
    // if (intent?.getBooleanExtra(EXTRA_DISARM_REQUEST, false) == true) {
    //     if (preferences.isAntiTheftArmed()) { toggleAntiTheft() -> performDisarm() ->
    // startService(ACTION_STOP) }
    // }

    assertTrue("Should have started STOP service", stopIntentFound)
  }

  @Test
  fun `Panic button triggers alert`() {
    val controller = Robolectric.buildActivity(MainActivity::class.java)
    val activity = controller.create().start().resume().get()

    val btnPanic = activity.findViewById<View>(R.id.btnPanic)
    btnPanic.performClick()

    val shadowActivity = Shadows.shadowOf(activity)
    val startedIntent = shadowActivity.nextStartedService
    assertNotNull(startedIntent)
    assertEquals(AntiTheftService.ACTION_PANIC, startedIntent.action)
  }

  @Test
  fun `Battery updates update UI`() {
    val controller = Robolectric.buildActivity(MainActivity::class.java)
    val activity = controller.create().start().resume().get()

    val batteryIntent =
      Intent(Intent.ACTION_BATTERY_CHANGED).apply {
        putExtra(BatteryManager.EXTRA_LEVEL, 80)
        putExtra(BatteryManager.EXTRA_SCALE, 100)
        putExtra(BatteryManager.EXTRA_VOLTAGE, 4000)
        putExtra(BatteryManager.EXTRA_TEMPERATURE, 350)
      }

    context.sendBroadcast(batteryIntent)
    ShadowLooper.runUiThreadTasks()

    val tvLevel = activity.findViewById<TextView>(R.id.tvBatteryLevel)
    assertEquals("80%", tvLevel.text.toString())
  }

  @Test
  fun `Navigation buttons start activities`() {
    val controller = Robolectric.buildActivity(MainActivity::class.java)
    val activity = controller.create().start().resume().get()
    val shadowActivity = Shadows.shadowOf(activity)

    activity.findViewById<View>(R.id.btnSettings).performClick()
    assertEquals(
      SettingsActivity::class.java.name,
      shadowActivity.nextStartedActivity.component?.className
    )

    activity.findViewById<View>(R.id.btnOpenHistory).performClick()
    assertEquals(
      HistoryActivity::class.java.name,
      shadowActivity.nextStartedActivity.component?.className
    )

    activity.findViewById<View>(R.id.btnOpenRadarFromGrid).performClick()
    assertEquals(
      WifiRadarActivity::class.java.name,
      shadowActivity.nextStartedActivity.component?.className
    )
  }

  @Test
  fun `Smart Mode switch updates preference`() {
    val controller = Robolectric.buildActivity(MainActivity::class.java)
    val activity = controller.create().start().resume().get()
    val switchSmartMode =
      activity.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(
        R.id.switchSmartMode
      )

    switchSmartMode.performClick()

    val util = PreferencesUtil(context)
    // Default is usually false (or whatever preference was before), check if it toggles
    // Since we clear prefs in setup, default is false. Click -> True.
    assertTrue("Smart Mode should be enabled after click", util.isSmartModeEnabled())
  }

  @Test
  fun `Silent Mode switch updates preference`() {
    val controller = Robolectric.buildActivity(MainActivity::class.java)
    val activity = controller.create().start().resume().get()
    val switchSilentMode =
      activity.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(
        R.id.switchSilentMode
      )

    switchSilentMode.performClick()

    val util = PreferencesUtil(context)
    assertTrue("Silent Mode should be enabled after click", util.isSilentModeEnabled())
  }

  @Test
  fun `Security Settings button opens Camera Dashboard`() {
    val controller = Robolectric.buildActivity(MainActivity::class.java)
    val activity = controller.create().start().resume().get()
    val shadowActivity = Shadows.shadowOf(activity)

    activity.findViewById<View>(R.id.btnSecuritySettings).performClick()
    assertEquals(
      "com.example.presencedetector.security.ui.CameraDashboardActivity",
      shadowActivity.nextStartedActivity.component?.className
    )
  }

  @Test
  fun `updateDashboard updates UI counts`() {
    val controller = Robolectric.buildActivity(MainActivity::class.java)
    val activity = controller.create().start().resume().get()

    val devices =
      listOf(
        com.example.presencedetector.model.WiFiDevice(
          "Dev1",
          "00:00:00:00:00:01",
          -50,
          2400,
          source = com.example.presencedetector.model.DeviceSource.WIFI
        ),
        com.example.presencedetector.model.WiFiDevice(
          "Dev2",
          "00:00:00:00:00:02",
          -60,
          2400,
          source = com.example.presencedetector.model.DeviceSource.WIFI
        )
      )

    // Use reflection to call private updateDashboard
    val method =
      MainActivity::class
        .java
        .getDeclaredMethod(
          "updateDashboard",
          List::class.java,
          String::class.java,
          String::class.java
        )
    method.isAccessible = true
    method.invoke(activity, devices, "WiFi", "Test Details")

    val tvCountUnknown = activity.findViewById<TextView>(R.id.tvCountUnknown)
    assertEquals("2", tvCountUnknown.text.toString())
  }
}
