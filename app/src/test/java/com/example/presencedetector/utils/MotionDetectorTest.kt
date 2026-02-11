package com.example.presencedetector.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionDetectorTest {

  @Test
  fun `isMotionDetected returns true when movement exceeds threshold`() {
    // Threshold = 1.0
    val detector = MotionDetector(1.0f)

    // Movement: DeltaX = 2.0 (Total > 1.0)
    assertTrue(detector.isMotionDetected(2.0f, 0f, 0f, 0f, 0f, 0f))

    // Movement: Combined (sqrt(1+1+1) = 1.73 > 1.0)
    assertTrue(detector.isMotionDetected(1.0f, 1.0f, 1.0f, 0f, 0f, 0f))
  }

  @Test
  fun `isMotionDetected returns false when movement is below threshold`() {
    val detector = MotionDetector(2.0f)

    // Movement: DeltaX = 1.0 (Total < 2.0)
    assertFalse(detector.isMotionDetected(1.0f, 0f, 0f, 0f, 0f, 0f))

    // No movement
    assertFalse(detector.isMotionDetected(0f, 0f, 0f, 0f, 0f, 0f))
  }
}
