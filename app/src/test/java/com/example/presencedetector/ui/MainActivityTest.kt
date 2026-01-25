package com.example.presencedetector.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.example.presencedetector.MainActivity
import com.example.presencedetector.R
import com.example.presencedetector.services.AntiTheftService
import com.example.presencedetector.services.DetectionBackgroundService
import com.example.presencedetector.utils.PreferencesUtil
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
@Config(sdk = [33])
class MainActivityTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val prefs = context.getSharedPreferences("com.example.presencedetector_preferences", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
    }

    @Test
    fun `activity should launch and initialize views`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java)
        val activity = controller.create().start().resume().get()

        // Use R.id names based on source code provided
        // btnAntiTheft, startButton, etc.
        assertNotNull(activity.findViewById(R.id.btnAntiTheft))
        assertNotNull(activity.findViewById(R.id.startButton))
    }

    @Test
    fun `UI should show armed status when preference is set`() {
        // Set armed in prefs
        val util = PreferencesUtil(context)
        util.setAntiTheftArmed(true)

        val controller = Robolectric.buildActivity(MainActivity::class.java)
        val activity = controller.create().start().resume().get()

        val statusText = activity.findViewById<TextView>(R.id.tvAntiTheftStatus)
        // Check for Portuguese "Armado"
        assert(statusText.text.toString().contains("Armado"))
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
    fun `Start button should start detection service`() {
        // Grant permissions
        val app = org.robolectric.shadows.ShadowApplication.getInstance()
        app.grantPermissions(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_WIFI_STATE,
            android.Manifest.permission.CHANGE_WIFI_STATE,
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.POST_NOTIFICATIONS,
            android.Manifest.permission.NEARBY_WIFI_DEVICES,
            android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )

        val controller = Robolectric.buildActivity(MainActivity::class.java)
        val activity = controller.create().start().resume().get()

        val btn = activity.findViewById<Button>(R.id.startButton)
        btn.performClick()

        val shadowActivity = Shadows.shadowOf(activity)
        val startedIntent = shadowActivity.nextStartedService
        assertNotNull("Detection Service should start", startedIntent)
        assertEquals(DetectionBackgroundService::class.java.name, startedIntent.component?.className)
    }
}
