package com.example.presencedetector.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Surface
import androidx.core.app.ActivityCompat
import com.example.presencedetector.services.TelegramService
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class CameraHelper(private val context: Context) {

    private val telegramService = TelegramService(context)

    fun captureSelfie(surfaceTexture: SurfaceTexture? = null, onComplete: (() -> Unit)? = null) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.w("CameraHelper", "Camera permission not granted")
            onComplete?.invoke()
            return
        }

        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull {
                cameraManager.getCameraCharacteristics(it)
                    .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
            } ?: cameraManager.cameraIdList.firstOrNull()

            if (cameraId == null) {
                onComplete?.invoke()
                return
            }

            // Create a dummy surface texture if not provided
            val texture = surfaceTexture ?: SurfaceTexture(10)
            texture.setDefaultBufferSize(640, 480)

            val imageReader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 1)

            // Cleanup function
            var activeCamera: CameraDevice? = null
            var activeSession: CameraCaptureSession? = null

            val cleanup = {
                try {
                    activeSession?.close()
                    activeCamera?.close()
                    imageReader.close()
                    if (surfaceTexture == null) texture.release() // Only release if we created it
                    onComplete?.invoke()
                } catch (e: Exception) { e.printStackTrace() }
            }

            // Fix for Kotlin compiler issue with Handler
            val mainHandler = Handler(Looper.getMainLooper())

            imageReader.setOnImageAvailableListener({ reader ->
                try {
                    val image = reader.acquireLatestImage()
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    image.close()

                    saveAndSendImage(bytes)

                    mainHandler.post { cleanup() }
                } catch (e: Exception) {
                    e.printStackTrace()
                    cleanup()
                }
            }, mainHandler)

            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    activeCamera = camera
                    try {
                        val previewSurface = Surface(texture)
                        val captureSurface = imageReader.surface

                        camera.createCaptureSession(listOf(previewSurface, captureSurface), object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                activeSession = session
                                try {
                                    // Start Preview to warm up
                                    val previewRequest = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                                    previewRequest.addTarget(previewSurface)
                                    session.setRepeatingRequest(previewRequest.build(), null, null)

                                    // Capture after delay
                                    mainHandler.postDelayed({
                                        try {
                                            val captureRequest = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                                            captureRequest.addTarget(captureSurface)
                                            // Ensure orientation is decent (assuming portrait natural)
                                            captureRequest.set(CaptureRequest.JPEG_ORIENTATION, 270)
                                            session.capture(captureRequest.build(), null, null)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            cleanup()
                                        }
                                    }, 500)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    cleanup()
                                }
                            }

                            override fun onConfigureFailed(session: CameraCaptureSession) {
                                cleanup()
                            }
                        }, null)
                    } catch (e: Exception) {
                        cleanup()
                    }
                }

                override fun onDisconnected(camera: CameraDevice) {
                    activeCamera = camera
                    cleanup()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    activeCamera = camera
                    cleanup()
                }
            }, mainHandler)

        } catch (e: Exception) {
            Log.e("CameraHelper", "Error in captureSelfie", e)
            onComplete?.invoke()
        }
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
