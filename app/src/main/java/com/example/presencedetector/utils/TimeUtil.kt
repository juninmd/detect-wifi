package com.example.presencedetector.utils

import java.text.SimpleDateFormat
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
    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val now = Date()
    val currentStr = dateFormat.format(now)

    return try {
      val start = dateFormat.parse(startStr)
      val end = dateFormat.parse(endStr)
      val current = dateFormat.parse(currentStr)

      if (start != null && end != null && current != null) {
        if (start.before(end)) {
          (current.after(start) || current == start) && (current.before(end) || current == end)
        } else {
          // Spans over midnight (e.g., 22:00 to 06:00)
          (current.after(start) || current == start) || (current.before(end) || current == end)
        }
      } else {
        false
      }
    } catch (e: Exception) {
      false
    }
  }

  fun getCurrentTimestamp(): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
  }
}
