package com.example.presencedetector.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.example.presencedetector.model.WiFiDevice
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Custom view that simulates a Radar GPS.
 * Places devices on circles based on signal strength.
 */
class RadarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2C2C2E") // Darker circles for dark theme
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val devicePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 30f
        textAlign = Paint.Align.CENTER
    }

    private var devices: List<WiFiDevice> = emptyList()

    fun updateDevices(newDevices: List<WiFiDevice>) {
        this.devices = newDevices
        invalidate()
    }

    fun setDevices(newDevices: List<WiFiDevice>) {
        updateDevices(newDevices)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val maxRadius = min(centerX, centerY) * 0.8f

        // Draw radar circles
        canvas.drawCircle(centerX, centerY, maxRadius, circlePaint)
        canvas.drawCircle(centerX, centerY, maxRadius * 0.66f, circlePaint)
        canvas.drawCircle(centerX, centerY, maxRadius * 0.33f, circlePaint)

        // Draw devices
        devices.forEachIndexed { index, device ->
            val nickname = device.nickname
            
            val level = device.level.coerceIn(-90, -30)
            val normalizedDist = 1f - (level + 90) / 60f
            val radius = normalizedDist * maxRadius
            
            val angle = (index * 137.5f) * (Math.PI / 180f)
            val dx = centerX + radius * cos(angle).toFloat()
            val dy = centerY + radius * sin(angle).toFloat()

            devicePaint.color = if (nickname != null) Color.parseColor("#4D7FFE") else Color.parseColor("#8E8E93")
            
            // Draw a glow effect for known devices
            if (nickname != null) {
                val glowPaint = Paint(devicePaint).apply { alpha = 50 }
                canvas.drawCircle(dx, dy, 25f, glowPaint)
            }
            
            canvas.drawCircle(dx, dy, 15f, devicePaint)
            
            val label = nickname ?: device.ssid.take(8)
            canvas.drawText(label, dx, dy + 45f, textPaint)
        }
    }
}
