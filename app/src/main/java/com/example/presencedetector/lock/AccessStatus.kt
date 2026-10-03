package com.example.presencedetector.lock

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import com.example.presencedetector.intruder.IntruderDeviceAdminReceiver

/** Reads the state of the two system grants the lock and the intruder photo depend on. */
object AccessStatus {
  fun adminComponent(context: Context) =
    ComponentName(context, IntruderDeviceAdminReceiver::class.java)

  fun isDeviceAdminActive(context: Context): Boolean =
    context.getSystemService(DevicePolicyManager::class.java).isAdminActive(adminComponent(context))

  fun isAccessibilityEnabled(context: Context): Boolean {
    val enabled =
      Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
      ) ?: return false
    val me = ComponentName(context, AppLockAccessibilityService::class.java).flattenToString()
    return enabled.split(':').any { it.equals(me, ignoreCase = true) }
  }
}
