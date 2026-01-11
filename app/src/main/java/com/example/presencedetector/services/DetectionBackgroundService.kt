package com.example.presencedetector.services

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.example.presencedetector.utils.NotificationUtil

/**
 * Background service for continuous presence detection.
 * Runs even when the app is not in the foreground.
 */
class DetectionBackgroundService : Service() {
    companion object {
        private const val TAG = "DetectionBackgroundService"
        private const val NOTIFICATION_ID = 1
    }

    private val binder = LocalBinder()
    private var detectionManager: PresenceDetectionManager? = null
    private var isRunning = false

    inner class LocalBinder : Binder() {
        fun getService(): DetectionBackgroundService = this@DetectionBackgroundService
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Background service created")

        detectionManager = PresenceDetectionManager(this)
        detectionManager?.setPresenceListener { peoplePresent, method, _, details ->
            updateForegroundNotification(peoplePresent, method, details)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Background service started")

        if (!isRunning) {
            isRunning = true

            // Criar notificação para foreground
            val notification = NotificationUtil.createForegroundNotification(this)
            startForeground(NOTIFICATION_ID, notification)

            // Iniciar detecção
            detectionManager?.startDetection()
            Log.i(TAG, "Presence detection started in background")
        }

        // START_STICKY: Reinicia o serviço se for morto pelo sistema
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "Background service destroyed")

        isRunning = false
        detectionManager?.stopDetection()
        detectionManager?.destroy()
    }

    private fun updateForegroundNotification(peoplePresent: Boolean, method: String, details: String) {
        try {
            val notification = if (peoplePresent) {
                NotificationUtil.createForegroundNotification(
                    this,
                    "✓ Presença Detectada",
                    "Método: $method"
                )
            } else {
                NotificationUtil.createForegroundNotification(
                    this,
                    "✗ Nenhuma Presença",
                    "Método: $method"
                )
            }

            val notificationManager = getSystemService(android.app.NotificationManager::class.java)
            notificationManager?.notify(NOTIFICATION_ID, notification)

        } catch (e: Exception) {
            Log.e(TAG, "Error updating foreground notification", e)
        }
    }

    fun getDetectionStatus(): String = detectionManager?.getDetectionStatus() ?: "N/A"
}
