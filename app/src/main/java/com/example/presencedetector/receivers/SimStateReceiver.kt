package com.example.presencedetector.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.example.presencedetector.security.repository.LogRepository
import com.example.presencedetector.services.AntiTheftService
import com.example.presencedetector.utils.PreferencesUtil

class SimStateReceiver : BroadcastReceiver() {
  companion object {
    const val EXTRA_REASON = "com.example.presencedetector.EXTRA_REASON"
  }

  override fun onReceive(context: Context, intent: Intent) {
    // Handle standard intent action
    if (intent.action == "android.intent.action.SIM_STATE_CHANGED") {
      val stateExtra = intent.getStringExtra("ss")
      LogRepository.logSystemEvent(context, "SIM State Broadcast Received. State: $stateExtra")

      // reliable check via TelephonyManager
      val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
      val simState = telephonyManager?.simState ?: TelephonyManager.SIM_STATE_UNKNOWN

      if (simState == TelephonyManager.SIM_STATE_ABSENT) {
        LogRepository.logSystemEvent(
          context,
          "SIM Card detected as ABSENT (Removed). Checking security status."
        )
        checkAndTriggerAlarm(context)
      }
    }
  }

  private fun checkAndTriggerAlarm(context: Context) {
    try {
      val prefs = PreferencesUtil(context)
      if (prefs.isAntiTheftArmed()) {
        LogRepository.logSystemEvent(context, "System Armed! Triggering Panic due to SIM removal.")

        val serviceIntent =
          Intent(context, AntiTheftService::class.java).apply {
            action = AntiTheftService.ACTION_PANIC
            putExtra(EXTRA_REASON, "SIM CARD REMOVED")
          }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
          context.startForegroundService(serviceIntent)
        } else {
          context.startService(serviceIntent)
        }
      } else {
        LogRepository.logSystemEvent(context, "System not armed. Ignoring SIM removal.")
      }
    } catch (e: Exception) {
      LogRepository.logSystemEvent(
        context,
        "Error checking preferences or starting service: ${e.message}"
      )
    }
  }
}
