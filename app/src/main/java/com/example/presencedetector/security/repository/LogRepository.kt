package com.example.presencedetector.security.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Repository for handling file-based logging of system and detection events.
 * Replaces LoggerUtil and SharedPreferences-based logging.
 * Optimized for performance using Coroutines and efficient file I/O.
 */
object LogRepository {
  private const val TAG = "LogRepository"
  private const val LOG_DIR = "presence_detector_logs"
  private const val SYSTEM_LOG_FILE = "system_events.log"

  // Use a single-threaded dispatcher to ensure log order and prevent file locking issues
  private val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
  private val scope = CoroutineScope(dispatcher + SupervisorJob())

  /**
   * Logs a general system event (e.g., service start, errors, security alerts).
   * Asynchronous operation.
   */
  fun logSystemEvent(context: Context, message: String) {
    val appContext = context.applicationContext
    scope.launch {
      try {
        val timestamp = getTimestamp()
        val logEntry = "[$timestamp] $message"
        appendLog(appContext, SYSTEM_LOG_FILE, logEntry)
        Log.d(TAG, "System Event: $message")
      } catch (e: Exception) {
        Log.e(TAG, "Error logging system event", e)
      }
    }
  }

  /**
   * Logs a detection event for a specific device (BSSID).
   * Asynchronous operation.
   */
  fun logDetectionEvent(context: Context, bssid: String, message: String) {
    val appContext = context.applicationContext
    scope.launch {
      try {
        val timestamp = getTimestamp()
        val logEntry = "[$timestamp] $message"
        // Sanitize BSSID for filename
        val safeBssid = bssid.replace(":", "")
        val filename = "device_${safeBssid}.log"
        appendLog(appContext, filename, logEntry)
      } catch (e: Exception) {
        Log.e(TAG, "Error logging detection event", e)
      }
    }
  }

  /**
   * Retrieves the last N system logs.
   * This remains synchronous for compatibility but utilizes efficient streaming.
   */
  fun getSystemLogs(context: Context, limit: Int = 100): List<String> {
    return readLogsReverse(context, SYSTEM_LOG_FILE, limit)
  }

  /**
   * Retrieves the last N detection logs for a specific device.
   */
  fun getDetectionLogs(context: Context, bssid: String, limit: Int = 100): List<String> {
    val safeBssid = bssid.replace(":", "")
    val filename = "device_${safeBssid}.log"
    return readLogsReverse(context, filename, limit)
  }

  private fun appendLog(context: Context, filename: String, entry: String) {
    try {
      val logsDir = getLogsDir(context)
      val file = File(logsDir, filename)
      // Use appendText which handles opening/closing
      file.appendText("$entry\n")
    } catch (e: Exception) {
      Log.e(TAG, "Error writing to log file: $filename", e)
    }
  }

  /**
   * Efficiently reads the last N lines from a file using a rolling buffer.
   * This avoids loading the entire file into memory.
   */
  private fun readLogsReverse(context: Context, filename: String, limit: Int): List<String> {
    val logsDir = getLogsDir(context)
    val file = File(logsDir, filename)
    if (!file.exists()) return emptyList()

    val buffer = ArrayDeque<String>(limit)
    try {
        file.useLines { lines ->
            lines.forEach { line ->
                if (buffer.size >= limit) {
                    buffer.removeFirst()
                }
                buffer.addLast(line)
            }
        }
    } catch (e: Exception) {
      Log.e(TAG, "Error reading log file: $filename", e)
      return emptyList()
    }

    // Return reversed (newest first)
    return buffer.toList().reversed()
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
    val appContext = context.applicationContext
    scope.launch {
      try {
          val logsDir = getLogsDir(appContext)
          logsDir.listFiles()?.forEach { it.delete() }
      } catch (e: Exception) {
          Log.e(TAG, "Error clearing logs", e)
      }
    }
  }
}
