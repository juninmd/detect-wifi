package com.example.presencedetector.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.presencedetector.services.TelegramService
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class CameraHelper(private val context: Context) {

    private val telegramService = TelegramService(context)
    private val mainHandler = Handler(Looper.getMainLooper())

    fun captureSelfie(surfaceTexture: SurfaceTexture? = null, onComplete: (() -> Unit)? = null) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.w("CameraHelper", "Camera permission not granted")
            onComplete?.invoke()
            return
        }

        val session = SelfieCaptureSession(
            context = context,
            providedSurfaceTexture = surfaceTexture,
            mainHandler = mainHandler,
            onImageCaptured = { bytes -> saveAndSendImage(bytes) },
            onComplete = onComplete
        )
        session.start()
    }

    private fun saveAndSendImage(bytes: ByteArray) {
        Thread {
            try {
                val filename = "EVENT_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".jpg"
                val file = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), filename)
                FileOutputStream(file).use { it.write(bytes) }

                val prefs = PreferencesUtil(context)
                prefs.logSystemEvent("📸 Photo Captured: $filename")

                telegramService.sendPhoto(file, "📸 Security Event Captured")

                // Show Notification
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                NotificationUtil.sendIntruderAlert(context, bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}
