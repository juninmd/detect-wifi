package com.example.presencedetector.router

import android.content.Context
import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/** Periodically scans the router while the background service runs and monitoring is enabled. */
class RouterPoller(
  context: Context,
  private val intervalSec: Long = DEFAULT_INTERVAL_SEC,
  private val prefs: RouterPreferences = RouterPreferences(context),
  private val scanner: RouterScanner = RouterScanner(context, prefs),
) {
  companion object {
    private const val TAG = "RouterPoller"
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
      .onFailure { Log.w(TAG, "Router scan failed: ${it.message}") }
  }
}
