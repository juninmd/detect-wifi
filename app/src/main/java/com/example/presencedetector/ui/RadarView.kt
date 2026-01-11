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
import com.example.presencedetector.model.WiFiDevice
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Modern Radar View with scanning animation and device blips.
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

    // Animation Loop
    private val animator = object : Runnable {
        override fun run() {
            if (isAnimating) {
                scanAngle = (scanAngle + animationSpeed) % 360f
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

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val maxRadius = min(centerX, centerY) * 0.85f

        // 1. Draw Radar Grid
        // Outer circle
        canvas.drawCircle(centerX, centerY, maxRadius, circlePaint)
        // Inner circles
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

        // 3. Draw Devices
        devices.forEachIndexed { index, device ->
            // Map signal level (-90 to -30) to distance (1.0 to 0.0)
            // Stronger signal = Closer to center
            val level = device.level.coerceIn(-90, -30)
            val normalizedDist = 1f - ((level + 90) / 60f)
            val radius = normalizedDist * maxRadius
            
            // Distribute devices around the circle to avoid overlap visual
            // Using a pseudo-random but deterministic angle based on hashcode or index
            val angleDeg = (index * 137.5f) % 360f
            val angleRad = Math.toRadians(angleDeg.toDouble())

            val dx = centerX + (radius * cos(angleRad)).toFloat()
            val dy = centerY + (radius * sin(angleRad)).toFloat()

            // Device Color
            val isKnown = device.nickname != null
            val color = if (isKnown) Color.parseColor("#34C759") else Color.parseColor("#FF9500")
            
            // Draw Glow
            if (isKnown) {
                devicePaint.color = color
                devicePaint.alpha = 50
                canvas.drawCircle(dx, dy, 25f, devicePaint)
            }
            
            // Draw Dot
            devicePaint.color = color
            devicePaint.alpha = 255
            canvas.drawCircle(dx, dy, 12f, devicePaint)
            
            // Draw Label
            val label = device.nickname ?: device.ssid.take(8)
            canvas.drawText(label, dx, dy + 40f, textPaint)
        }

        // Center User Dot
        devicePaint.color = Color.parseColor("#007AFF")
        canvas.drawCircle(centerX, centerY, 8f, devicePaint)
    }
}
