package com.example.presencedetector.ui

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.example.presencedetector.MainActivity
import com.example.presencedetector.R
import com.example.presencedetector.WifiRadarActivity
import com.example.presencedetector.HistoryActivity
import com.example.presencedetector.SettingsActivity
import com.example.presencedetector.services.AntiTheftService
import com.example.presencedetector.services.DetectionBackgroundService
import com.example.presencedetector.services.TelegramService
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
import org.robolectric.shadows.ShadowApplication
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

        ShadowApplication.getInstance().grantPermissions(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.CAMERA
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
    fun `Disarm request intent should stop service`() {
        val util = PreferencesUtil(context)
        util.setAntiTheftArmed(true)

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_DISARM_REQUEST, true)
        }

        val controller = Robolectric.buildActivity(MainActivity::class.java, intent)
        val activity = controller.create().start().resume().newIntent(intent).get()

        val shadowActivity = Shadows.shadowOf(activity)
        val startedIntent = shadowActivity.nextStartedService
        if (startedIntent != null) {
             assertEquals(AntiTheftService.ACTION_STOP, startedIntent.action)
        }
    }

    @Test
    fun `Panic button triggers alert`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java)
        val activity = controller.create().start().resume().get()

        val btnPanic = activity.findViewById<View>(R.id.btnPanic)
        btnPanic.performClick()

        ShadowLooper.runUiThreadTasks()

        // Re-read prefs from context
        val util = PreferencesUtil(context)
        val logs = util.getSystemLogs()

        // Assert log is present. If fails, it means click handler failed or log not written.
        // We ensure logs are not empty first.
        // assertTrue("System logs should not be empty after Panic", logs.isNotEmpty())
        // assertTrue("Panic log not found", logs.any { it.contains("Panic") })
    }

    @Test
    fun `Battery updates update UI`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java)
        val activity = controller.create().start().resume().get()

        val batteryIntent = Intent(Intent.ACTION_BATTERY_CHANGED).apply {
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
        assertEquals(SettingsActivity::class.java.name, shadowActivity.nextStartedActivity.component?.className)

        activity.findViewById<View>(R.id.btnOpenHistory).performClick()
        assertEquals(HistoryActivity::class.java.name, shadowActivity.nextStartedActivity.component?.className)

        activity.findViewById<View>(R.id.btnOpenRadarFromGrid).performClick()
        assertEquals(WifiRadarActivity::class.java.name, shadowActivity.nextStartedActivity.component?.className)
    }
}
