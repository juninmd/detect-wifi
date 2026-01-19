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
        color = Color.parseColor("#4D00FF00") // dragon_radar_grid
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3300FF00") // Faint green
        style = Paint.Style.STROKE
        strokeWidth = 1f
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(15f, 15f), 0f)
    }

    private val sweepPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val devicePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00FF00") // dragon_radar_green
        textSize = 28f
        typeface = android.graphics.Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
        setShadowLayer(5f, 0f, 0f, Color.parseColor("#8000FF00")) // Glow effect
    }

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CC001100") // dragon_radar_bg
        style = Paint.Style.FILL
    }

    // State
    private var devices: List<WiFiDevice> = emptyList()
    private var scanAngle = 0f
    private val animationSpeed = 2.5f
    private var isAnimating = true
    private var pulsePhase = 0f

    // Signal smoothing
    private val smoothedLevels = mutableMapOf<String, Float>()
    private val SMOOTHING_FACTOR = 0.25f 

    // Animation Loop
    private val animator = object : Runnable {
        override fun run() {
            if (isAnimating) {
                scanAngle = (scanAngle + animationSpeed) % 360f
                pulsePhase = (pulsePhase + 0.15f) % (2f * Math.PI.toFloat())
                invalidate()
                postDelayed(this, 16) 
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

    private fun getSmoothedLevel(bssid: String, currentLevel: Int): Float {
        val previous = smoothedLevels[bssid] ?: currentLevel.toFloat()
        val smoothed = previous + SMOOTHING_FACTOR * (currentLevel - previous)
        smoothedLevels[bssid] = smoothed
        return smoothed
    }

    private fun getConsistentAngle(bssid: String): Float {
        return (abs(bssid.hashCode()) % 360).toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val maxRadius = min(centerX, centerY) * 0.9f

        // 0. Dragon Radar Background
        canvas.drawCircle(centerX, centerY, maxRadius, backgroundPaint)

        // 1. Draw Radar Grid
        // Outer rim
        circlePaint.strokeWidth = 5f
        circlePaint.color = Color.parseColor("#00FF00")
        circlePaint.setShadowLayer(10f, 0f, 0f, Color.parseColor("#8000FF00")) // Neon Glow
        canvas.drawCircle(centerX, centerY, maxRadius, circlePaint)
        circlePaint.clearShadowLayer()
        
        // Inner circles
        canvas.drawCircle(centerX, centerY, maxRadius * 0.66f, gridPaint)
        canvas.drawCircle(centerX, centerY, maxRadius * 0.33f, gridPaint)
        
        // Crosshairs
        canvas.drawLine(centerX - maxRadius, centerY, centerX + maxRadius, centerY, gridPaint)
        canvas.drawLine(centerX, centerY - maxRadius, centerX, centerY + maxRadius, gridPaint)

        // 2. Draw Scanning Sweep (Green Gradient)
        val sweepGradient = SweepGradient(centerX, centerY,
            intArrayOf(Color.TRANSPARENT, Color.parseColor("#003300"), Color.parseColor("#00FF00")),
            floatArrayOf(0f, 0.5f, 1f)
        )
        sweepPaint.shader = sweepGradient

        canvas.save()
        canvas.rotate(scanAngle, centerX, centerY)
        canvas.drawCircle(centerX, centerY, maxRadius, sweepPaint)
        canvas.restore()

        // 3. Draw Devices
        devices.forEach { device ->
            val angleDeg = getConsistentAngle(device.bssid)
            val angleRad = Math.toRadians(angleDeg.toDouble())

            val smoothedLevel = getSmoothedLevel(device.bssid, device.level)
            val clampedLevel = smoothedLevel.coerceIn(-90f, -30f)
            val normalizedDist = 1f - ((clampedLevel + 90f) / 60f)
            val radius = normalizedDist * maxRadius

            val dx = centerX + (radius * cos(angleRad)).toFloat()
            val dy = centerY + (radius * sin(angleRad)).toFloat()

            val isKnown = device.nickname != null
            // Dragon Radar Colors: Gold for DragonBalls (unknowns?), Cyan for Allies (known)
            val color = when {
                 isKnown -> Color.parseColor("#00FFFF") 
                 else -> Color.parseColor("#FFD700") 
            }

            val pulseScale = 1f + 0.2f * sin(pulsePhase + angleDeg / 20f)
            
            // Glow
            devicePaint.color = color
            devicePaint.alpha = 80
            val glowRadius = if (isKnown) 20f * pulseScale else 14f * pulseScale
            canvas.drawCircle(dx, dy, glowRadius, devicePaint)

            // Core
            devicePaint.alpha = 255
            val dotRadius = if (isKnown) 10f else 7f
            canvas.drawCircle(dx, dy, dotRadius, devicePaint)

            // Label
            val label = device.nickname ?: "" // Only show nickname on map to reduce clutter
            if (label.isNotEmpty()) {
                canvas.drawText(label, dx, dy + 35f, textPaint)
            }
        }

        // Center User Arrow
        val arrowPath = android.graphics.Path().apply {
            moveTo(centerX, centerY - 15f)
            lineTo(centerX - 10f, centerY + 10f)
            lineTo(centerX, centerY + 5f) // Indent
            lineTo(centerX + 10f, centerY + 10f)
            close()
        }
        devicePaint.color = Color.RED
        canvas.drawPath(arrowPath, devicePaint)
    }
}

