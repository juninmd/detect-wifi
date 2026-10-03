package com.example.presencedetector.lock

/** Persistence contract for the app lock, so the logic stays free of Android classes. */
interface AppLockStore {
  var enabled: Boolean
  var pin: PinHasher.Hashed?
  var protectedApps: Set<String>
  var failedAttempts: Int
  var lockedUntilMs: Long
}
