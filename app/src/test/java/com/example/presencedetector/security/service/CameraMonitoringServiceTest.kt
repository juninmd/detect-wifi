package com.example.presencedetector.security.service

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.Robolectric
import org.mockito.Mockito.mockConstruction
import org.videolan.libvlc.LibVLC

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CameraMonitoringServiceTest {
  @Test
  fun testInit() {
    mockConstruction(LibVLC::class.java).use { mocked ->
        val service = Robolectric.buildService(CameraMonitoringService::class.java).create().get()
        assertNotNull(service)
    }
  }
}
