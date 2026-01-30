package com.example.presencedetector.services

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AppUsageMonitorTest {

    @Mock private lateinit var mockUsageStatsManager: UsageStatsManager
    @Mock private lateinit var mockUsageEvents: UsageEvents
    private lateinit var context: Context
    private lateinit var appUsageMonitor: AppUsageMonitor

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        // Avoid using spy on ApplicationProvider.getApplicationContext() directly if it causes casting issues.
        // Instead, just pass a mock context or use the real one and mock getSystemService

        context = mock(Context::class.java)

        // Mock getSystemService behavior
        `when`(context.getSystemService(Context.USAGE_STATS_SERVICE)).thenReturn(mockUsageStatsManager)

        appUsageMonitor = AppUsageMonitor(context)
    }

    @Test
    fun `checkForegroundApp should attempt to query events`() {
        // Just verify that we are calling the system service
        `when`(mockUsageStatsManager.queryEvents(anyLong(), anyLong())).thenReturn(mockUsageEvents)

        appUsageMonitor.checkForegroundApp { }

        verify(mockUsageStatsManager).queryEvents(anyLong(), anyLong())
    }
}
