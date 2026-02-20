package com.example.presencedetector.security.repository

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Repository for handling file-based logging of system and detection events.
 * Replaces LoggerUtil and SharedPreferences-based logging.
 */
object LogRepository {
  private const val TAG = "LogRepository"
  private const val LOG_DIR = "presence_detector_logs"
  private const val SYSTEM_LOG_FILE = "system_events.log"

  /**
   * Logs a general system event (e.g., service start, errors, security alerts).
   */
  fun logSystemEvent(context: Context, message: String) {
    val timestamp = getTimestamp()
    val logEntry = "[$timestamp] $message"
    appendLog(context, SYSTEM_LOG_FILE, logEntry)
    Log.d(TAG, "System Event: $message")
  }

  /**
   * Logs a detection event for a specific device (BSSID).
   */
  fun logDetectionEvent(context: Context, bssid: String, message: String) {
    val timestamp = getTimestamp()
    val logEntry = "[$timestamp] $message"
    // Sanitize BSSID for filename
    val safeBssid = bssid.replace(":", "")
    val filename = "device_${safeBssid}.log"
    appendLog(context, filename, logEntry)
  }

  /**
   * Retrieves the last N system logs.
   */
  fun getSystemLogs(context: Context, limit: Int = 100): List<String> {
    return readLogs(context, SYSTEM_LOG_FILE, limit)
  }

  /**
   * Retrieves the last N detection logs for a specific device.
   */
  fun getDetectionLogs(context: Context, bssid: String, limit: Int = 100): List<String> {
    val safeBssid = bssid.replace(":", "")
    val filename = "device_${safeBssid}.log"
    return readLogs(context, filename, limit)
  }

  private fun appendLog(context: Context, filename: String, entry: String) {
    try {
      val logsDir = getLogsDir(context)
      val file = File(logsDir, filename)
      file.appendText("$entry\n")
    } catch (e: Exception) {
      Log.e(TAG, "Error writing to log file: $filename", e)
    }
  }

  private fun readLogs(context: Context, filename: String, limit: Int): List<String> {
    return try {
      val logsDir = getLogsDir(context)
      val file = File(logsDir, filename)
      if (!file.exists()) return emptyList()

      // Read lines and return the last 'limit' lines in reverse chronological order (newest first)
      file.readLines().takeLast(limit).reversed()
    } catch (e: Exception) {
      Log.e(TAG, "Error reading log file: $filename", e)
      emptyList()
    }
  }

  private fun getLogsDir(context: Context): File {
    val dir = File(context.getExternalFilesDir(null), LOG_DIR)
    if (!dir.exists()) {
      dir.mkdirs()
    }
    return dir
  }

  private fun getTimestamp(): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
  }

  /**
   * Clears all logs.
   */
  fun clearLogs(context: Context) {
      try {
          val logsDir = getLogsDir(context)
          logsDir.listFiles()?.forEach { it.delete() }
      } catch (e: Exception) {
          Log.e(TAG, "Error clearing logs", e)
      }
  }
}
