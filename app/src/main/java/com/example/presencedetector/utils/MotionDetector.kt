package com.example.presencedetector.utils

import kotlin.math.sqrt

class MotionDetector(private val threshold: Float) {

  fun isMotionDetected(
    x: Float,
    y: Float,
    z: Float,
    lastX: Float,
    lastY: Float,
    lastZ: Float
  ): Boolean {
    val deltaX = kotlin.math.abs(x - lastX)
    val deltaY = kotlin.math.abs(y - lastY)
    val deltaZ = kotlin.math.abs(z - lastZ)

    val totalDelta = sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)

    return totalDelta > threshold
  }
}
