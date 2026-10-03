package com.example.presencedetector.lock

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent

/**
 * Watches which app comes to the foreground and covers protected apps with [AppLockActivity]. It
 * only reads the package name of the window; it never reads screen content.
 */
class AppLockAccessibilityService : AccessibilityService() {
  private val screenOff =
    object : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) {
        AppLockModule.controller(context).clearGrace()
      }
    }

  override fun onServiceConnected() {
    super.onServiceConnected()
    registerReceiver(screenOff, IntentFilter(Intent.ACTION_SCREEN_OFF), RECEIVER_NOT_EXPORTED)
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
    val pkg = event.packageName?.toString() ?: return
    if (pkg == packageName) return
    if (AppLockModule.controller(this).needsLock(pkg)) {
      startActivity(
        Intent(this, AppLockActivity::class.java)
          .putExtra(AppLockActivity.EXTRA_PACKAGE, pkg)
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
      )
    }
  }

  override fun onInterrupt() = Unit

  override fun onDestroy() {
    runCatching { unregisterReceiver(screenOff) }
    super.onDestroy()
  }
}
