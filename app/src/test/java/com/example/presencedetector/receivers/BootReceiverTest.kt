package com.example.presencedetector.receivers

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.presencedetector.services.AntiTheftService
import com.example.presencedetector.services.DetectionBackgroundService
import com.example.presencedetector.utils.PreferencesUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BootReceiverTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `onReceive should start DetectionBackgroundService`() {
        val receiver = BootReceiver()
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)

        receiver.onReceive(context, intent)

        val shadowContext = Shadows.shadowOf(context as android.app.Application)
        val nextStartedService = shadowContext.nextStartedService
        assertNotNull("Should start a service", nextStartedService)
        // Note: The order depends on implementation.
        // If AntiTheft is not armed, only Detection should start.
        assertEquals(DetectionBackgroundService::class.java.name, nextStartedService.component?.className)
    }

    @Test
    fun `onReceive should start AntiTheftService if armed`() {
        // Set armed state
        val prefs = PreferencesUtil(context)
        prefs.setAntiTheftArmed(true)

        val receiver = BootReceiver()
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)

        receiver.onReceive(context, intent)

        val shadowContext = Shadows.shadowOf(context as android.app.Application)

        var foundAntiTheft = false
        var foundDetection = false

        // Capture all started services
        val startedIntents = mutableListOf<Intent>()
        while (true) {
            val intent = shadowContext.nextStartedService ?: break
            startedIntents.add(intent)
        }

        for (intent in startedIntents) {
            if (intent.component?.className == AntiTheftService::class.java.name) {
                foundAntiTheft = true
                assertEquals(AntiTheftService.ACTION_START, intent.action)
            } else if (intent.component?.className == DetectionBackgroundService::class.java.name) {
                foundDetection = true
            }
        }

        assert(foundAntiTheft) { "AntiTheftService should be started" }
        assert(foundDetection) { "DetectionBackgroundService should be started" }
    }
}
