package com.example.presencedetector.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Utility class for handling time-related logic. */
object TimeUtil {

    /**
     * Checks if the current time falls within the start and end time range.
     * Handles ranges that span across midnight (e.g., 22:00 to 06:00).
     *
     * @param startStr Start time in "HH:mm" format
     * @param endStr End time in "HH:mm" format
     * @return true if current time is within the range, false otherwise
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
                    // Standard range (e.g., 09:00 to 17:00)
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
}
