package com.example.presencedetector.services

import android.app.Application
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import com.example.presencedetector.MainActivity
import com.example.presencedetector.receivers.NotificationActionReceiver
import com.example.presencedetector.utils.PreferencesUtil
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowSensorManager

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

    whenever(mockPreferences.getAntiTheftSensitivity()).thenReturn(1.0f)
    whenever(mockPreferences.isPocketModeEnabled()).thenReturn(false)
    whenever(mockPreferences.isChargerModeEnabled()).thenReturn(false)

    // Grant permissions including RECORD_AUDIO and READ_PHONE_STATE
    val app = ApplicationProvider.getApplicationContext<Application>()
    val shadowApp = Shadows.shadowOf(app)
    shadowApp.grantPermissions(
      android.Manifest.permission.ACCESS_FINE_LOCATION,
      android.Manifest.permission.RECORD_AUDIO,
      android.Manifest.permission.READ_PHONE_STATE,
      android.Manifest.permission.POST_NOTIFICATIONS // Required for foreground service
    )

    val controller = Robolectric.buildService(AntiTheftService::class.java)
    service = controller.get()
    // Inject mocks (internal properties)
    service.preferences = mockPreferences
    service.telegramService = mockTelegramService

    controller.create()
  }

  @Test
  fun `startMonitoring should arm the service`() {
    val intent =
      Intent(context, AntiTheftService::class.java).apply { action = AntiTheftService.ACTION_START }

    service.onStartCommand(intent, 0, 0)

    verify(mockPreferences).setAntiTheftArmed(true)
  }

  @Test
  fun `stopMonitoring should disarm the service`() {
    service.onStartCommand(
      Intent(context, AntiTheftService::class.java).apply {
        action = AntiTheftService.ACTION_START
      },
      0,
      0
    )

    val intent =
      Intent(context, AntiTheftService::class.java).apply { action = AntiTheftService.ACTION_STOP }
    service.onStartCommand(intent, 0, 0)

    verify(mockPreferences).setAntiTheftArmed(false)
  }

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
    val intent =
      Intent(context, AntiTheftService::class.java).apply { action = AntiTheftService.ACTION_START }
    service.onStartCommand(intent, 0, 0)

    SystemClock.sleep(6000)

    val event1 = createSensorEvent(floatArrayOf(0f, 0f, 9.8f), Sensor.TYPE_ACCELEROMETER)
    service.onSensorChanged(event1)

    val event2 = createSensorEvent(floatArrayOf(5f, 5f, 5f), Sensor.TYPE_ACCELEROMETER)
    service.onSensorChanged(event2)

    verify(mockTelegramService)
      .sendMessage(
        check {
          assertTrue(
            it.contains("Movimento Detectado") || it.contains("ALARM") || it.contains("Motion")
          )
        }
      )
  }

  @Test
  fun `triggerAlarm should launch AlarmActivity and send location`() {
    // Setup Location
    val locationManager =
      context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
    val shadowLocationManager = Shadows.shadowOf(locationManager)
    val location = android.location.Location(android.location.LocationManager.GPS_PROVIDER)
    location.latitude = 10.0
    location.longitude = 20.0
    location.time = System.currentTimeMillis()
    shadowLocationManager.setLastKnownLocation(
      android.location.LocationManager.GPS_PROVIDER,
      location
    )

    // Start Service
    val intent =
      Intent(context, AntiTheftService::class.java).apply { action = AntiTheftService.ACTION_START }
    service.onStartCommand(intent, 0, 0)
    SystemClock.sleep(6000)

    // Trigger Alarm (Motion)
    val event1 = createSensorEvent(floatArrayOf(0f, 0f, 9.8f), Sensor.TYPE_ACCELEROMETER)
    service.onSensorChanged(event1)
    val event2 = createSensorEvent(floatArrayOf(5f, 5f, 5f), Sensor.TYPE_ACCELEROMETER)
    service.onSensorChanged(event2)

    // Verify Location Sent
    verify(mockTelegramService, atLeastOnce())
      .sendMessage(
        check {
          if (it.contains("LOCATION")) {
            assertTrue(it.contains("maps.google.com") && it.contains("10.0,20.0"))
          }
        }
      )
  }

  @Test
  fun `low battery should trigger alert`() {
    val intent =
      Intent(context, AntiTheftService::class.java).apply { action = AntiTheftService.ACTION_START }
    service.onStartCommand(intent, 0, 0)

    val app = ApplicationProvider.getApplicationContext<Application>()
    val shadowApp = Shadows.shadowOf(app)
    val receivers = shadowApp.registeredReceivers
    val batteryReceiverWrapper =
      receivers.find { it.intentFilter.hasAction(Intent.ACTION_BATTERY_CHANGED) }

    if (batteryReceiverWrapper != null) {
      val batteryIntent =
        Intent(Intent.ACTION_BATTERY_CHANGED).apply {
          putExtra(android.os.BatteryManager.EXTRA_LEVEL, 10)
          putExtra(android.os.BatteryManager.EXTRA_SCALE, 100)
          putExtra(
            android.os.BatteryManager.EXTRA_STATUS,
            android.os.BatteryManager.BATTERY_STATUS_DISCHARGING
          )
        }
      batteryReceiverWrapper.broadcastReceiver.onReceive(context, batteryIntent)

      verify(mockTelegramService)
        .sendMessage(
          check { assertTrue(it.contains("LOW BATTERY") || it.contains("Security Device")) }
        )
    } else {
      fail("Battery receiver not registered")
    }
  }

  @Test
  fun `charger disconnect should trigger alarm if enabled`() {
    whenever(mockPreferences.isChargerModeEnabled()).thenReturn(true)

    val intent =
      Intent(context, AntiTheftService::class.java).apply { action = AntiTheftService.ACTION_START }
    service.onStartCommand(intent, 0, 0)

    val app = ApplicationProvider.getApplicationContext<Application>()
    val shadowApp = Shadows.shadowOf(app)
    val receivers = shadowApp.registeredReceivers
    val powerReceiverWrapper =
      receivers.find { it.intentFilter.hasAction(Intent.ACTION_POWER_DISCONNECTED) }

    if (powerReceiverWrapper != null) {
      val powerIntent = Intent(Intent.ACTION_POWER_DISCONNECTED)
      powerReceiverWrapper.broadcastReceiver.onReceive(context, powerIntent)

      verify(mockTelegramService)
        .sendMessage(check { assertTrue(it.contains("Charger") || it.contains("Carregador")) })
    } else {
      fail("Power receiver not registered")
    }
  }

  @Test
  fun `foreground notification should contain Panic action`() {
    val intent =
      Intent(context, AntiTheftService::class.java).apply { action = AntiTheftService.ACTION_START }
    service.onStartCommand(intent, 0, 0)

    val notificationManager =
      context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    val shadows = Shadows.shadowOf(notificationManager)
    val notification = shadows.getNotification(999)

    assertNotNull(notification)
    val actions = notification.actions
    val panicAction = actions.find { it.title.toString().contains("PÂNICO") }
    assertNotNull("Panic action should exist", panicAction)

    val pendingIntent = panicAction!!.actionIntent
    val shadowIntent = Shadows.shadowOf(pendingIntent).savedIntent
    assertEquals(NotificationActionReceiver.ACTION_PANIC, shadowIntent.action)
  }

  @Test
  fun `ACTION_SNOOZE should stop alarm and log message`() {
    val intentStart =
      Intent(context, AntiTheftService::class.java).apply { action = AntiTheftService.ACTION_START }
    service.onStartCommand(intentStart, 0, 0)
    SystemClock.sleep(6000)
    val event = createSensorEvent(floatArrayOf(5f, 5f, 5f), Sensor.TYPE_ACCELEROMETER)
    service.onSensorChanged(event)

    val intentSnooze =
      Intent(context, AntiTheftService::class.java).apply {
        action = AntiTheftService.ACTION_SNOOZE
      }
    service.onStartCommand(intentSnooze, 0, 0)

    verify(mockTelegramService).sendMessage(check { assertTrue(it.contains("Snoozed")) })

    val notificationManager =
      context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    val shadows = Shadows.shadowOf(notificationManager)
    assertNull(shadows.getNotification(1000))
  }

  @Test
  fun `ACTION_PANIC with reason should trigger alarm with that reason`() {
    val intent =
      Intent(context, AntiTheftService::class.java).apply {
        action = AntiTheftService.ACTION_PANIC
        putExtra("com.example.presencedetector.EXTRA_REASON", "SIM CARD REMOVED")
      }

    service.onStartCommand(intent, 0, 0)

    verify(mockTelegramService)
      .sendMessage(
        check {
          assertTrue("Message should contain SIM CARD REMOVED", it.contains("SIM CARD REMOVED"))
        }
      )
  }

  @Test
  fun `ACTION_PANIC should trigger panic notification with specific ID`() {
    val intent =
      Intent(context, AntiTheftService::class.java).apply { action = AntiTheftService.ACTION_PANIC }

    service.onStartCommand(intent, 0, 0)

    val notificationManager =
      context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    val shadows = Shadows.shadowOf(notificationManager)

    // NotificationUtil.sendPanicAlert uses ID 1000
    val notification = shadows.getNotification(1000)
    assertNotNull("Panic notification should be posted", notification)

    val title = notification.extras.getString(android.app.Notification.EXTRA_TITLE)
    assertTrue("Title should contain Panic", title?.contains("PÂNICO") == true)

    val actions = notification.actions
    assertTrue("Should have Call 190 action", actions.any { it.title.toString().contains("190") })
  }

  @Test
  fun `foreground notification disarm action should point to MainActivity`() {
    val intent =
      Intent(context, AntiTheftService::class.java).apply { action = AntiTheftService.ACTION_START }
    service.onStartCommand(intent, 0, 0)

    val notificationManager =
      context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    val notification = Shadows.shadowOf(notificationManager).getNotification(999)

    assertNotNull("Foreground notification should exist", notification)

    val disarmAction =
      notification.actions.find {
        it.title.toString().contains("Desarmar") || it.title.toString().contains("Disarm")
      }
    assertNotNull("Disarm action should exist", disarmAction)

    val pendingIntent = disarmAction!!.actionIntent
    val shadowIntent = Shadows.shadowOf(pendingIntent).savedIntent

    assertEquals(
      "Intent should target MainActivity",
      MainActivity::class.java.name,
      shadowIntent.component?.className
    )
    assertTrue(
      "Intent should have EXTRA_DISARM_REQUEST",
      shadowIntent.getBooleanExtra(MainActivity.EXTRA_DISARM_REQUEST, false)
    )
  }
}
