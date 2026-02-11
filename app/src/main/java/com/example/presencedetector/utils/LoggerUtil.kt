package com.example.presencedetector.utils

import android.content.Context
import android.util.Log
import java.io.BufferedWriter
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/** Utility class for logging detection events to file. */
object LoggerUtil {
  private const val TAG = "LoggerUtil"
  private const val LOG_DIR = "presence_detector_logs"

  fun logEvent(context: Context, event: String) {
    try {
      // Use scoped storage directory (Android 10+)
      val logsDir = File(context.getExternalFilesDir(null), LOG_DIR)
      if (!logsDir.exists()) {
        logsDir.mkdirs()
      }

      val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
      val filename = "detection_${dateFormat.format(Date())}.log"
      val logFile = File(logsDir, filename)

      BufferedWriter(java.io.FileWriter(logFile, true)).use { writer ->
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timestamp = timeFormat.format(Date())

        writer.write("[$timestamp] $event")
        writer.newLine()
      }

      Log.d(TAG, "Event logged: $event")
    } catch (e: Exception) {
      Log.e(TAG, "Error logging event", e)
    }
  }
}
