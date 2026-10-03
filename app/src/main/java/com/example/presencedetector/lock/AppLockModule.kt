package com.example.presencedetector.lock

import android.content.Context

/** Process-wide holder so the accessibility service and the PIN screen share one controller. */
object AppLockModule {
  @Volatile private var instance: AppLockController? = null

  fun controller(context: Context): AppLockController =
    instance
      ?: synchronized(this) {
        instance
          ?: AppLockController(AppLockPreferences(context.applicationContext)).also {
            instance = it
          }
      }
}
