package com.example.presencedetector.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeUtilTest {

  @Test
  fun `isCurrentTimeInSchedule should work for simple range`() {
    val now = Date()
    val format = SimpleDateFormat("HH:mm", Locale.getDefault())
    val currentStr = format.format(now)
    val currentHour = currentStr.split(":")[0].toInt()

    val startHour = (currentHour - 1 + 24) % 24
    val endHour = (currentHour + 1) % 24
    val startStr = String.format("%02d:00", startHour)
    val endStr = String.format("%02d:00", endHour)

    assertTrue(TimeUtil.isCurrentTimeInSchedule(startStr, endStr))
  }

  @Test
  fun `isCurrentTimeInSchedule should work for overnight range`() {
    val now = Date()
    val format = SimpleDateFormat("HH:mm", Locale.getDefault())
    val currentStr = format.format(now)
    val currentHour = currentStr.split(":")[0].toInt()

    val startHour = (currentHour + 1) % 24
    val endHour = (currentHour - 1 + 24) % 24
    val startStr = String.format("%02d:00", startHour)
    val endStr = String.format("%02d:00", endHour)

    assertFalse(
      "Should be outside of inverted schedule",
      TimeUtil.isCurrentTimeInSchedule(startStr, endStr),
    )
  }

  @Test
  fun `isCurrentTimeInSchedule invalid format`() {
    assertFalse(TimeUtil.isCurrentTimeInSchedule("invalid", "invalid"))
  }
}
