package com.example.presencedetector.lock

import com.example.presencedetector.lock.AppLockController.Result
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class StubStore : AppLockStore {
  override var enabled = true
  override var pin: PinHasher.Hashed? = null
  override var protectedApps: Set<String> = setOf(BANK)
  override var failedAttempts = 0
  override var lockedUntilMs = 0L
}

private const val BANK = "com.nu.production"

class AppLockControllerTest {
  private lateinit var store: StubStore
  private lateinit var controller: AppLockController
  private var now = 1_000_000L

  @Before
  fun setUp() {
    store = StubStore()
    controller = AppLockController(store, clock = { now })
    controller.setPin("1234")
  }

  @Test
  fun `only protected apps need the lock`() {
    assertTrue(controller.needsLock(BANK))
    assertFalse(controller.needsLock("com.android.chrome"))
  }

  @Test
  fun `nothing is locked while disabled or without a pin`() {
    store.enabled = false
    assertFalse(controller.needsLock(BANK))
    store.enabled = true
    store.pin = null
    assertFalse(controller.needsLock(BANK))
  }

  @Test
  fun `setPin only accepts 4 to 8 digits`() {
    assertFalse(controller.setPin("123"))
    assertFalse(controller.setPin("123456789"))
    assertFalse(controller.setPin("12a4"))
    assertTrue(controller.setPin("12345678"))
  }

  @Test
  fun `correct pin unlocks and starts the grace period`() {
    assertEquals(Result.Unlocked, controller.submit(BANK, "1234"))
    assertFalse(controller.needsLock(BANK))
    now += AppLockController.GRACE_MS + 1
    assertTrue(controller.needsLock(BANK))
  }

  @Test
  fun `clearGrace locks again immediately`() {
    controller.submit(BANK, "1234")
    controller.clearGrace()
    assertTrue(controller.needsLock(BANK))
  }

  @Test
  fun `wrong pin counts down and reports from the second attempt`() {
    assertEquals(Result.Wrong(1, 2, report = false), controller.submit(BANK, "0000"))
    assertEquals(Result.Wrong(2, 1, report = true), controller.submit(BANK, "0000"))
  }

  @Test
  fun `third wrong pin locks the pad and reports`() {
    repeat(2) { controller.submit(BANK, "0000") }
    val result = controller.submit(BANK, "0000") as Result.Locked
    assertTrue(result.report)
    assertEquals(30_000L, result.remainingMs)
    assertTrue(controller.submit(BANK, "1234") is Result.Locked)
  }

  @Test
  fun `lockout ends with time and doubles after the next batch`() {
    repeat(3) { controller.submit(BANK, "0000") }
    now += 30_001
    assertEquals(0L, controller.lockoutRemainingMs())
    repeat(2) { controller.submit(BANK, "0000") }
    assertEquals(60_000L, (controller.submit(BANK, "0000") as Result.Locked).remainingMs)
  }

  @Test
  fun `success resets the failure counter`() {
    controller.submit(BANK, "0000")
    controller.submit(BANK, "1234")
    assertEquals(0, store.failedAttempts)
  }

  @Test
  fun `submit without a stored pin reports NoPin`() {
    store.pin = null
    assertEquals(Result.NoPin, controller.submit(BANK, "1234"))
  }
}
