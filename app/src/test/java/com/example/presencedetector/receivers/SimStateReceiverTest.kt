package com.example.presencedetector.receivers

import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import androidx.test.core.app.ApplicationProvider
import com.example.presencedetector.services.AntiTheftService
import com.example.presencedetector.utils.PreferencesUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowTelephonyManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SimStateReceiverTest {

    private lateinit var context: Context
    private lateinit var receiver: SimStateReceiver
    private lateinit var telephonyManager: TelephonyManager
    private lateinit var shadowTelephonyManager: ShadowTelephonyManager
    private lateinit var prefs: PreferencesUtil

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        receiver = SimStateReceiver()
        telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        shadowTelephonyManager = shadowOf(telephonyManager)
        prefs = PreferencesUtil(context)
        prefs.clear()
    }

    @Test
    fun `onReceive triggers alarm if armed and SIM absent`() {
        // Arm
        prefs.setAntiTheftArmed(true)

        // Set SIM absent
        shadowTelephonyManager.setSimState(TelephonyManager.SIM_STATE_ABSENT)

        val intent = Intent("android.intent.action.SIM_STATE_CHANGED")
        intent.putExtra("ss", "ABSENT") // Extra "ss" is often used in this broadcast

        receiver.onReceive(context, intent)

        // Verify service started
        val startedIntent = shadowOf(context as android.app.Application).nextStartedService
        assertEquals(AntiTheftService::class.java.name, startedIntent.component?.className)
        assertEquals(AntiTheftService.ACTION_PANIC, startedIntent.action)
        assertEquals("SIM CARD REMOVED", startedIntent.getStringExtra(SimStateReceiver.EXTRA_REASON))
    }

    @Test
    fun `onReceive ignores if not armed`() {
        prefs.setAntiTheftArmed(false)
        shadowTelephonyManager.setSimState(TelephonyManager.SIM_STATE_ABSENT)

        val intent = Intent("android.intent.action.SIM_STATE_CHANGED")
        receiver.onReceive(context, intent)

        val startedIntent = shadowOf(context as android.app.Application).nextStartedService
        assertNull(startedIntent)
    }
}
