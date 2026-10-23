package com.example.presencedetector.presence

import android.content.Context
import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/** Polls the presence backend while the background service runs and monitoring is enabled. */
class PresencePoller(
  context: Context,
  private val intervalSec: Long = DEFAULT_INTERVAL_SEC,
  private val prefs: PresencePreferences = PresencePreferences(context),
  private val scanner: PresenceScanner = PresenceScanner(context, prefs),
) {
  companion object {
    private const val TAG = "PresencePoller"
    private const val DEFAULT_INTERVAL_SEC = 60L
    private const val FIRST_DELAY_SEC = 15L
  }

  private var executor: ScheduledExecutorService? = null

  @Synchronized
  fun start() {
    if (executor != null) return
    executor =
      Executors.newSingleThreadScheduledExecutor().also {
        it.scheduleWithFixedDelay(::tick, FIRST_DELAY_SEC, intervalSec, TimeUnit.SECONDS)
      }
  }

  @Synchronized
  fun stop() {
    executor?.shutdownNow()
    executor = null
  }

  private fun tick() {
    if (!prefs.monitoringEnabled || !prefs.isConfigured()) return
    runCatching { scanner.scan(background = true) }
      .onFailure { Log.w(TAG, "Presence scan failed: ${it.message}") }
  }
}
