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
   * Efficiently reads the last N lines from a file reading backwards from the end.
   * This avoids loading the entire file into memory (O(limit) instead of O(file_size)).
   */
  private fun readLogsReverse(context: Context, filename: String, limit: Int): List<String> {
    val logsDir = getLogsDir(context)
    val file = File(logsDir, filename)
    if (!file.exists()) return emptyList()

    val result = mutableListOf<String>()
    var raf: java.io.RandomAccessFile? = null
    try {
        raf = java.io.RandomAccessFile(file, "r")
        val length = raf.length()
        var currentPos = length
        val bufferSize = 4096
        var partialLine = ""

        while (currentPos > 0 && result.size < limit) {
            val readSize = if (currentPos >= bufferSize) bufferSize.toLong() else currentPos
            currentPos -= readSize

            raf.seek(currentPos)
            val bytes = ByteArray(readSize.toInt())
            raf.readFully(bytes)

            val chunk = String(bytes, java.nio.charset.StandardCharsets.UTF_8)
            val fullChunk = chunk + partialLine
            val linesInChunk = fullChunk.split('\n')

            if (linesInChunk.isNotEmpty()) {
                if (currentPos > 0) {
                    // The first element is the end of the previous line (which we haven't fully read yet)
                    partialLine = linesInChunk[0]

                    // Process the rest in reverse order
                    for (i in linesInChunk.indices.reversed()) {
                        if (i == 0) continue // Skip the partial part
                        val line = linesInChunk[i]
                        // Ignore empty strings that result from trailing newlines
                        if (line.isNotEmpty() && result.size < limit) {
                             result.add(line)
                        }
                    }
                } else {
                    // We are at the start of the file, all parts are valid
                    for (i in linesInChunk.indices.reversed()) {
                        val line = linesInChunk[i]
                        if (line.isNotEmpty() && result.size < limit) {
                             result.add(line)
                        }
                    }
                }
            }
        }

    } catch (e: Exception) {
      Log.e(TAG, "Error reading log file: $filename", e)
    } finally {
        try { raf?.close() } catch (e: Exception) {}
    }

    return result
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
