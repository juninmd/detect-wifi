package com.example.presencedetector.lock

/**
 * Decides when a protected app must be covered by the PIN screen and validates PIN attempts.
 *
 * Repeated mistakes lock the PIN pad with a growing delay and flag the attempt as an intrusion so
 * the caller can take a photo and alert the owner.
 */
class AppLockController(
  private val store: AppLockStore,
  private val clock: () -> Long = System::currentTimeMillis,
  private val graceMs: Long = GRACE_MS,
  private val maxAttempts: Int = MAX_ATTEMPTS,
) {
  companion object {
    const val GRACE_MS = 30_000L
    const val MAX_ATTEMPTS = 3
    const val REPORT_AFTER = 2
    const val MIN_PIN = 4
    const val MAX_PIN = 8
    private const val BASE_LOCKOUT_MS = 30_000L
    private const val MAX_LOCKOUT_MS = 15 * 60_000L
  }

  sealed interface Result {
    data object Unlocked : Result

    data object NoPin : Result

    data class Wrong(val attempts: Int, val remaining: Int, val report: Boolean) : Result

    data class Locked(val remainingMs: Long, val report: Boolean = false) : Result
  }

  private val unlockedAt = mutableMapOf<String, Long>()

  fun isProtected(pkg: String) = store.enabled && store.pin != null && pkg in store.protectedApps

  fun needsLock(pkg: String): Boolean {
    if (!isProtected(pkg)) return false
    val at = unlockedAt[pkg] ?: return true
    return clock() - at > graceMs
  }

  fun clearGrace() = unlockedAt.clear()

  fun lockoutRemainingMs() = (store.lockedUntilMs - clock()).coerceAtLeast(0)

  fun setPin(pin: String): Boolean {
    if (!isValidPin(pin)) return false
    store.pin = PinHasher.hash(pin)
    resetAttempts()
    return true
  }

  fun submit(pkg: String, pin: String): Result {
    val stored = store.pin ?: return Result.NoPin
    val wait = lockoutRemainingMs()
    if (wait > 0) return Result.Locked(wait)
    if (PinHasher.verify(pin, stored)) {
      resetAttempts()
      unlockedAt[pkg] = clock()
      return Result.Unlocked
    }
    return registerFailure()
  }

  private fun registerFailure(): Result {
    val attempts = store.failedAttempts + 1
    store.failedAttempts = attempts
    val remaining = maxAttempts - attempts % maxAttempts
    if (attempts % maxAttempts == 0) {
      val batches = attempts / maxAttempts
      val delay = (BASE_LOCKOUT_MS shl (batches - 1).coerceAtMost(6)).coerceAtMost(MAX_LOCKOUT_MS)
      store.lockedUntilMs = clock() + delay
      return Result.Locked(delay, report = true)
    }
    return Result.Wrong(attempts, remaining, attempts >= REPORT_AFTER)
  }

  private fun resetAttempts() {
    store.failedAttempts = 0
    store.lockedUntilMs = 0
  }

  private fun isValidPin(pin: String) = pin.length in MIN_PIN..MAX_PIN && pin.all { it.isDigit() }
}
