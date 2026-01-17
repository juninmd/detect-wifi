package com.example.presencedetector.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionDetectorTest {

    @Test
    fun testMotionDetectedAboveThreshold() {
        val detector = MotionDetector(1.5f)
        // Change of 2.0 on X axis
        assertTrue(detector.isMotionDetected(2.0f, 0f, 0f, 0f, 0f, 0f))
    }

    @Test
    fun testMotionNotDetectedBelowThreshold() {
        val detector = MotionDetector(1.5f)
        // Change of 1.0 on X axis
        assertFalse(detector.isMotionDetected(1.0f, 0f, 0f, 0f, 0f, 0f))
    }
}
