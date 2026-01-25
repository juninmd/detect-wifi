package com.example.presencedetector.services

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import com.example.presencedetector.utils.PreferencesUtil
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowSensorManager
import org.mockito.kotlin.whenever
import org.mockito.kotlin.check
import org.mockito.kotlin.any

@RunWith(RobolectricTestRunner::class)
class AntiTheftServiceTest {

    @Mock private lateinit var mockPreferences: PreferencesUtil
    @Mock private lateinit var mockTelegramService: TelegramService

    private lateinit var service: AntiTheftService
    private lateinit var shadowSensorManager: ShadowSensorManager
    private lateinit var context: Context

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        shadowSensorManager = Shadows.shadowOf(sensorManager)

        // Setup Mocks defaults
        whenever(mockPreferences.getAntiTheftSensitivity()).thenReturn(1.0f)
        whenever(mockPreferences.isPocketModeEnabled()).thenReturn(false)
        whenever(mockPreferences.isChargerModeEnabled()).thenReturn(false)

        val controller = Robolectric.buildService(AntiTheftService::class.java)
        service = controller.get()
        service.preferences = mockPreferences
        service.telegramService = mockTelegramService
        controller.create()
    }

    @Test
    fun `startMonitoring should arm the service`() {
        val intent = Intent(context, AntiTheftService::class.java).apply {
            action = AntiTheftService.ACTION_START
        }

        service.onStartCommand(intent, 0, 0)

        verify(mockPreferences).setAntiTheftArmed(true)
        verify(mockPreferences).isPocketModeEnabled()
    }

    @Test
    fun `stopMonitoring should disarm the service`() {
        // First arm
        service.onStartCommand(Intent(context, AntiTheftService::class.java).apply { action = AntiTheftService.ACTION_START }, 0, 0)

        // Then disarm
        val intent = Intent(context, AntiTheftService::class.java).apply {
            action = AntiTheftService.ACTION_STOP
        }
        service.onStartCommand(intent, 0, 0)

        verify(mockPreferences).setAntiTheftArmed(false)
    }

    // Helper to create SensorEvent via reflection
    private fun createSensorEvent(values: FloatArray, type: Int): SensorEvent {
        val sensorEventClass = SensorEvent::class.java
        val constructor = sensorEventClass.getDeclaredConstructor(Int::class.javaPrimitiveType)
        constructor.isAccessible = true
        val event = constructor.newInstance(values.size)

        val valuesField = sensorEventClass.getField("values")
        valuesField.isAccessible = true
        val eventValues = valuesField.get(event) as FloatArray
        System.arraycopy(values, 0, eventValues, 0, values.size)

        val sensorField = sensorEventClass.getField("sensor")
        sensorField.isAccessible = true
        val sensor = mock(Sensor::class.java)
        whenever(sensor.type).thenReturn(type)
        whenever(sensor.maximumRange).thenReturn(100f)
        sensorField.set(event, sensor)

        return event
    }

    @Test
    fun `onSensorChanged should trigger alarm on significant motion after grace period`() {
        // Arm
        val intent = Intent(context, AntiTheftService::class.java).apply { action = AntiTheftService.ACTION_START }
        service.onStartCommand(intent, 0, 0)

        // Advance time > GRACE_PERIOD (5000ms)
        SystemClock.sleep(6000)

        // First reading (initializes lastX, lastY, lastZ)
        val event1 = createSensorEvent(floatArrayOf(0f, 0f, 9.8f), Sensor.TYPE_ACCELEROMETER)
        service.onSensorChanged(event1)

        // Second reading (Significant change)
        val event2 = createSensorEvent(floatArrayOf(5f, 5f, 5f), Sensor.TYPE_ACCELEROMETER)
        service.onSensorChanged(event2)

        verify(mockTelegramService).sendMessage(check {
            // Check for Portuguese string "Movimento Detectado" or generic "ALARM"
            assert(it.contains("Movimento Detectado") || it.contains("ALARM"))
        })
    }
}
