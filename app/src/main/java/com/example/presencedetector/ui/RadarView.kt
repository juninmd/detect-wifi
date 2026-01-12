package com.example.presencedetector.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import com.example.presencedetector.model.DeviceSource
import com.example.presencedetector.model.WiFiDevice
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Modern Radar View with scanning animation, device blips, and signal smoothing.
 */
class RadarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Paints
    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3A3A3C")
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3A3A3C")
        style = Paint.Style.STROKE
        strokeWidth = 1f
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    private val sweepPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val devicePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8E8E93")
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    // State
    private var devices: List<WiFiDevice> = emptyList()
    private var scanAngle = 0f
    private val animationSpeed = 2f
    private var isAnimating = true
    private var pulsePhase = 0f

    // Signal smoothing: store last known levels per BSSID for interpolation
    private val smoothedLevels = mutableMapOf<String, Float>()
    private val SMOOTHING_FACTOR = 0.3f // Lower = smoother, higher = more responsive

    // Animation Loop
    private val animator = object : Runnable {
        override fun run() {
            if (isAnimating) {
                scanAngle = (scanAngle + animationSpeed) % 360f
                pulsePhase = (pulsePhase + 0.1f) % (2f * Math.PI.toFloat())
                invalidate()
                postDelayed(this, 16) // ~60 FPS
            }
        }
    }

    init {
        post(animator)
    }

    fun updateDevices(newDevices: List<WiFiDevice>) {
        this.devices = newDevices
        invalidate()
    }

    // Alias for compatibility
    fun setDevices(newDevices: List<WiFiDevice>) {
        updateDevices(newDevices)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(animator)
        isAnimating = false
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isAnimating = true
        post(animator)
    }

    /**
     * Smooth signal level using exponential moving average.
     * Prevents devices from jumping around erratically.
     */
    private fun getSmoothedLevel(bssid: String, currentLevel: Int): Float {
        val previous = smoothedLevels[bssid] ?: currentLevel.toFloat()
        val smoothed = previous + SMOOTHING_FACTOR * (currentLevel - previous)
        smoothedLevels[bssid] = smoothed
        return smoothed
    }

    /**
     * Get consistent angle based on BSSID hash.
     * Device will always appear at the same position around the radar.
     */
    private fun getConsistentAngle(bssid: String): Float {
        return (abs(bssid.hashCode()) % 360).toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val maxRadius = min(centerX, centerY) * 0.85f

        // 1. Draw Radar Grid
        // Outer circle
        canvas.drawCircle(centerX, centerY, maxRadius, circlePaint)
        // Inner circles with distance labels
        canvas.drawCircle(centerX, centerY, maxRadius * 0.66f, gridPaint)
        canvas.drawCircle(centerX, centerY, maxRadius * 0.33f, gridPaint)
        // Crosshairs
        canvas.drawLine(centerX - maxRadius, centerY, centerX + maxRadius, centerY, gridPaint)
        canvas.drawLine(centerX, centerY - maxRadius, centerX, centerY + maxRadius, gridPaint)

        // 2. Draw Scanning Sweep
        val sweepGradient = SweepGradient(centerX, centerY,
            intArrayOf(Color.TRANSPARENT, Color.parseColor("#4D7FFE00"), Color.parseColor("#4D7FFE")),
            floatArrayOf(0f, 0.75f, 1f)
        )
        sweepPaint.shader = sweepGradient

        canvas.save()
        canvas.rotate(scanAngle, centerX, centerY)
        canvas.drawCircle(centerX, centerY, maxRadius, sweepPaint)
        canvas.restore()

        // 3. Draw Devices with improved positioning
        devices.forEach { device ->
            // Get consistent angle based on BSSID (device always in same position)
            val angleDeg = getConsistentAngle(device.bssid)
            val angleRad = Math.toRadians(angleDeg.toDouble())

            // Smooth signal level to prevent jumping
            val smoothedLevel = getSmoothedLevel(device.bssid, device.level)
            val clampedLevel = smoothedLevel.coerceIn(-90f, -30f)
            val normalizedDist = 1f - ((clampedLevel + 90f) / 60f)
            val radius = normalizedDist * maxRadius

            val dx = centerX + (radius * cos(angleRad)).toFloat()
            val dy = centerY + (radius * sin(angleRad)).toFloat()

            // Device Color based on source type and known status
            val isKnown = device.nickname != null
            val color = when {
                device.source == DeviceSource.BLUETOOTH -> if (isKnown) Color.parseColor("#007AFF") else Color.parseColor("#5856D6")
                isKnown -> Color.parseColor("#34C759") // Green for known WiFi
                else -> Color.parseColor("#FF9500") // Orange for unknown WiFi
            }

            // Pulsing glow effect for known devices (simulates active detection)
            val pulseScale = 1f + 0.3f * sin(pulsePhase + angleDeg / 30f)
            
            // Draw Glow (larger for known devices, pulsing)
            devicePaint.color = color
            devicePaint.alpha = if (isKnown) 60 else 30
            val glowRadius = if (isKnown) 25f * pulseScale else 18f
            canvas.drawCircle(dx, dy, glowRadius, devicePaint)

            // Draw Dot
            devicePaint.color = color
            devicePaint.alpha = 255
            val dotRadius = if (isKnown) 12f else 8f
            canvas.drawCircle(dx, dy, dotRadius, devicePaint)

            // Draw Label
            val label = device.nickname ?: device.ssid.take(8)
            canvas.drawText(label, dx, dy + 40f, textPaint)
        }

        // Center User Dot
        devicePaint.color = Color.parseColor("#007AFF")
        canvas.drawCircle(centerX, centerY, 8f, devicePaint)
    }
}

