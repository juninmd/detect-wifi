package com.example.presencedetector.services

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.util.Log

class AppUsageMonitor(private val context: Context) {
  companion object {
    private const val TAG = "AppUsageMonitor"

    // Sensitive Apps List (Brazil specific + Global)
    val SENSITIVE_APPS =
      setOf(
        "com.nu.production", // Nubank
        "com.itau", // Itau
        "br.com.bb.android", // Banco do Brasil
        "com.bradesco", // Bradesco
        "com.santander.app", // Santander
        "com.neoneiros", // Neon
        "com.mercadolibre", // Mercado Pago/Livre
        "com.picpay", // PicPay
        "com.instagram.android", // Instagram
        "com.facebook.katana", // Facebook
        "com.twitter.android", // Twitter
        "com.zhiliaoapp.musically", // TikTok
        "com.whatsapp", // WhatsApp
        "com.google.android.apps.photos" // Photos
      )
  }

  private val usageStatsManager =
    context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
  private var lastForegroundPackage: String? = null
  private val lastTriggerTime = mutableMapOf<String, Long>()

  fun checkForegroundApp(onSensitiveAppDetected: (String) -> Unit) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return

    val time = System.currentTimeMillis()
    val usageEvents = usageStatsManager.queryEvents(time - 5000, time) // Check last 5 seconds
    val event = UsageEvents.Event()

    // If usageEvents is null or empty, it might mean permission is missing or no events.
    if (usageEvents == null) return

    var latestEventTime = 0L
    var currentPkg: String? = null

    // Iterate to find the latest MOVE_TO_FOREGROUND event
    while (usageEvents.hasNextEvent()) {
      usageEvents.getNextEvent(event)
      if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
        if (event.timeStamp > latestEventTime) {
          latestEventTime = event.timeStamp
          currentPkg = event.packageName
        }
      }
    }

    // Only trigger if package CHANGED or it's a new detection cycle where we care
    // Actually, for "Every time a banking app is opened", we care about the transition.
    if (currentPkg != null) {
      if (currentPkg != lastForegroundPackage) {
        lastForegroundPackage = currentPkg

        if (SENSITIVE_APPS.contains(currentPkg)) {
          // Rate limit: Only trigger once every 60 seconds for the same app
          val lastTime = lastTriggerTime[currentPkg] ?: 0L
          if (time - lastTime > 60000) {
            Log.w(TAG, "Sensitive App Detected: $currentPkg")
            lastTriggerTime[currentPkg] = time
            onSensitiveAppDetected(currentPkg)
          }
        }
      }
    }
  }
}
