package com.example.presencedetector.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Utility object for time-related operations. */
object TimeUtil {

  /**
   * Checks if the current time falls within the specified start and end time schedule.
   *
   * @param startStr Start time in "HH:mm" format.
   * @param endStr End time in "HH:mm" format.
   * @return True if current time is within the schedule, false otherwise.
   */
  fun isCurrentTimeInSchedule(startStr: String, endStr: String): Boolean {
    return try {
      val calendar = Calendar.getInstance()
      val currentMinutes =
        calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

      val (startHour, startMinute) = parseTime(startStr)
      val (endHour, endMinute) = parseTime(endStr)

      val startMinutes = startHour * 60 + startMinute
      val endMinutes = endHour * 60 + endMinute

      if (startMinutes <= endMinutes) {
        currentMinutes in startMinutes..endMinutes
      } else {
        // Spans midnight (e.g., 22:00 to 06:00)
        currentMinutes >= startMinutes || currentMinutes <= endMinutes
      }
    } catch (e: Exception) {
      false
    }
  }

  private fun parseTime(timeStr: String): Pair<Int, Int> {
    val parts = timeStr.split(":")
    return Pair(parts[0].toInt(), parts[1].toInt())
  }

  fun getCurrentTimestamp(): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
  }
}
