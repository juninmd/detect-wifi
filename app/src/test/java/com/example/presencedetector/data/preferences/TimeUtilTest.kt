package com.example.presencedetector.data.preferences

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TimeUtilTest {

    @Test
    fun `isCurrentTimeInSchedule should work for simple range`() {
        val now = Date()
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currentStr = format.format(now)
        val currentHour = currentStr.split(":")[0].toInt()

        // Schedule covers current time (Current-1h to Current+1h)
        val startHour = (currentHour - 1 + 24) % 24
        val endHour = (currentHour + 1) % 24
        val startStr = String.format("%02d:00", startHour)
        val endStr = String.format("%02d:00", endHour)

        assertTrue(TimeUtil.isCurrentTimeInSchedule(startStr, endStr))
    }

    @Test
    fun `isCurrentTimeInSchedule should work for overnight range`() {
        // Test overnight logic: e.g. 23:00 to 06:00
        // We need to pick a current time and mock it, but TimeUtil uses 'new Date()' internally.
        // Since we can't easily mock Date() without dependency injection or MockK static mocking (which is complex),
        // we test relative to current time.

        val now = Date()
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currentStr = format.format(now)
        val currentHour = currentStr.split(":")[0].toInt()

        // Case 1: Current time is inside overnight range (e.g. now=02:00, range=23:00-06:00)
        // Let's construct a range that DEFINITELY includes now, crossing midnight if needed or not.

        // Let's use the logic: Start = Now - 1h, End = Now + 1h. Even if crossing midnight, it works.
        // But specifically for overnight crossing midnight test:
        // If we set Start = Now + 1h, End = Now - 1h. This covers everything EXCEPT the 2 hours around now.
        // So it should be FALSE.

        val startHour = (currentHour + 1) % 24
        val endHour = (currentHour - 1 + 24) % 24
        val startStr = String.format("%02d:00", startHour)
        val endStr = String.format("%02d:00", endHour)

        assertFalse("Should be outside of inverted schedule", TimeUtil.isCurrentTimeInSchedule(startStr, endStr))
    }
}
