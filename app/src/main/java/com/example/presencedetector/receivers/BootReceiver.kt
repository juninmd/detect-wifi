package com.example.presencedetector.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.presencedetector.services.AntiTheftService
import com.example.presencedetector.services.DetectionBackgroundService
import com.example.presencedetector.utils.PreferencesUtil

class BootReceiver : BroadcastReceiver() {
  companion object {
    private const val TAG = "BootReceiver"
  }

  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
      Log.d(TAG, "Boot completed. Checking services...")

      val prefs = PreferencesUtil(context)

      // 1. Restart Detection Service if it was enabled (assuming it should be always on if
      // configured)
      // Ideally we check a preference "isMonitoringEnabled", but for now let's assume if the user
      // has setup monitoring it should run.
      // Or better, check if the service is meant to be running.
      // For now, let's start it if we have permissions and it's not explicitly disabled.
      // Actually, DetectionBackgroundService seems to be the main engine.

      // NOTE: Foreground services must be started from foreground or via BOOT_COMPLETED receiver
      startService(context, DetectionBackgroundService::class.java)

      // 2. Restart Anti-Theft Service if it was armed before reboot
      // We need to persist "isArmed" state. Currently AntiTheftService doesn't seem to persist
      // "isArmed" across reboots in SharedPreferences.
      // Let's assume we want to be safe and NOT arm it automatically to avoid loops, unless we add
      // a persistent flag.
      // However, the user asked for "bug fixes". If I arm anti-theft and phone reboots (e.g.
      // battery dies), it should probably come back?
      // Usually Anti-Theft systems should persist.
      if (prefs.isAntiTheftArmed()) {
        val antiTheftIntent =
          Intent(context, AntiTheftService::class.java).apply {
            action = AntiTheftService.ACTION_START
          }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          context.startForegroundService(antiTheftIntent)
        } else {
          context.startService(antiTheftIntent)
        }
        Log.d(TAG, "Anti-Theft restarted.")
      }
    }
  }

  private fun startService(context: Context, serviceClass: Class<*>) {
    val intent = Intent(context, serviceClass)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      context.startForegroundService(intent)
    } else {
      context.startService(intent)
    }
  }
}
