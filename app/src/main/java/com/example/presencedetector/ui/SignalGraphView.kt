package com.example.presencedetector.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import com.example.presencedetector.services.SignalHistoryManager

class SignalGraphView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4CAF50") // Green
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = 30f
        textAlign = Paint.Align.RIGHT
    }

    private val path = Path()
    private var bssid: String = ""

    fun setDevice(bssid: String) {
        this.bssid = bssid
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (bssid.isEmpty()) return
        
        val history = SignalHistoryManager.getHistory(bssid)
        if (history.isEmpty()) return // Add "No Data" text later if needed
        
        val width = width.toFloat()
        val height = height.toFloat()
        val padding = 40f
        
        val contentWidth = width - 2 * padding
        val contentHeight = height - 2 * padding
        
        // Y-Axis: Signal from -100 to -30
        val minSignal = -100f
        val maxSignal = -30f
        val signalRange = maxSignal - minSignal
        
        // Draw Grid Lines (-40, -60, -80, -100)
        listOf(-40, -60, -80, -100).forEach { dbm ->
             val normalizedY = 1f - ((dbm - minSignal) / signalRange)
             val y = padding + (normalizedY * contentHeight)
             canvas.drawLine(padding, y, width - padding, y, gridPaint)
             canvas.drawText("${dbm}dBm", width - 10f, y, textPaint)
        }
        
        // Draw Graph
        path.reset()
        val points = history.takeLast(60) // Ensure we don't overdraw
        if (points.isNotEmpty()) {
             val stepX = contentWidth / (points.size.coerceAtLeast(10)) // scale X based on count (min 10 slots)
             
             points.forEachIndexed { index, pair ->
                 val signal = pair.second.coerceIn(-100, -30).toFloat()
                 val normalizedY = 1f - ((signal - minSignal) / signalRange)
                 val x = padding + (index * stepX)
                 val y = padding + (normalizedY * contentHeight)
                 
                 if (index == 0) {
                     path.moveTo(x, y)
                 } else {
                     path.lineTo(x, y)
                 }
                 
                 // Draw small circle for point
                 canvas.drawCircle(x, y, 4f, linePaint)
             }
             canvas.drawPath(path, linePaint)
        }
    }
}
