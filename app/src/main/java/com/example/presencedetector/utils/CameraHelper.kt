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
    private val mainHandler = Handler(Looper.getMainLooper())

    fun captureSelfie(surfaceTexture: SurfaceTexture? = null, onComplete: (() -> Unit)? = null) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.w("CameraHelper", "Camera permission not granted")
            onComplete?.invoke()
            return
        }

        val session = SelfieCaptureSession(surfaceTexture, onComplete)
        session.start()
    }

    private inner class SelfieCaptureSession(
        private val providedSurfaceTexture: SurfaceTexture?,
        private val onComplete: (() -> Unit)?
    ) {
        private var activeCamera: CameraDevice? = null
        private var activeSession: CameraCaptureSession? = null
        private var imageReader: ImageReader? = null
        private var createdSurfaceTexture: SurfaceTexture? = null

        fun start() {
            try {
                val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                val cameraId = findFrontFacingCamera(cameraManager)

                if (cameraId == null) {
                    cleanup()
                    return
                }

                setupImageReader()
                openCamera(cameraManager, cameraId)
            } catch (e: Exception) {
                Log.e("CameraHelper", "Error starting capture session", e)
                cleanup()
            }
        }

        private fun findFrontFacingCamera(manager: CameraManager): String? {
            return manager.cameraIdList.firstOrNull {
                manager.getCameraCharacteristics(it)
                    .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
            } ?: manager.cameraIdList.firstOrNull()
        }

        private fun setupImageReader() {
            imageReader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 1).apply {
                setOnImageAvailableListener({ reader ->
                    handleImageAvailable(reader)
                }, mainHandler)
            }
        }

        private fun handleImageAvailable(reader: ImageReader) {
            try {
                val image = reader.acquireLatestImage()
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                image.close()

                saveAndSendImage(bytes)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                mainHandler.post { cleanup() }
            }
        }

        private fun openCamera(manager: CameraManager, cameraId: String) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                cleanup()
                return
            }

            manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    activeCamera = camera
                    createCaptureSession()
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
        }

        private fun createCaptureSession() {
            try {
                val texture = providedSurfaceTexture ?: SurfaceTexture(10).also { createdSurfaceTexture = it }
                texture.setDefaultBufferSize(640, 480)

                val previewSurface = Surface(texture)
                val captureSurface = imageReader?.surface ?: return

                activeCamera?.createCaptureSession(listOf(previewSurface, captureSurface), object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        activeSession = session
                        triggerCapture(session, previewSurface, captureSurface)
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        cleanup()
                    }
                }, null)
            } catch (e: Exception) {
                cleanup()
            }
        }

        private fun triggerCapture(session: CameraCaptureSession, previewSurface: Surface, captureSurface: Surface) {
            try {
                // Warm up preview
                val previewRequest = activeCamera?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)?.apply {
                    addTarget(previewSurface)
                }
                if (previewRequest != null) {
                    session.setRepeatingRequest(previewRequest.build(), null, null)
                }

                // Capture after delay
                mainHandler.postDelayed({
                    try {
                        val captureRequest = activeCamera?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)?.apply {
                            addTarget(captureSurface)
                            set(CaptureRequest.JPEG_ORIENTATION, 270)
                        }
                        if (captureRequest != null) {
                            session.capture(captureRequest.build(), null, null)
                        }
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

        private fun cleanup() {
            try {
                activeSession?.close()
                activeCamera?.close()
                imageReader?.close()
                createdSurfaceTexture?.release()
                onComplete?.invoke()
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
