package com.example.presencedetector.security.ui

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.TextureView
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.presencedetector.MainActivity
import com.example.presencedetector.R
import com.example.presencedetector.services.AntiTheftService
import com.example.presencedetector.services.TelegramService
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmActivity : AppCompatActivity() {

  companion object {
    const val EXTRA_REASON = "EXTRA_REASON"
    private const val TAG = "AlarmActivity"
  }

  private lateinit var textureView: TextureView
  private lateinit var telegramService: TelegramService

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Wake Screen Logic
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
      setShowWhenLocked(true)
      setTurnScreenOn(true)
      val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
      keyguardManager?.requestDismissKeyguard(this, null)
    } else {
      @Suppress("DEPRECATION")
      window.addFlags(
        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
          WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
          WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
          WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
      )
    }

    setContentView(R.layout.activity_alarm)
    telegramService = TelegramService(this)

    val reason = intent.getStringExtra(EXTRA_REASON) ?: getString(R.string.reason_motion)
    findViewById<TextView>(R.id.tvAlertReason).text = reason

    findViewById<android.view.View>(R.id.btnStopAlarm).setOnClickListener {
      // Stop alarm via service action
      val stopIntent =
        Intent(this, AntiTheftService::class.java).apply { action = AntiTheftService.ACTION_STOP }
      startService(stopIntent)

      // Go to Main Activity
      val mainIntent = Intent(this, MainActivity::class.java)
      mainIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      startActivity(mainIntent)
      finish()
    }

    textureView = findViewById(R.id.cameraPreview)
    if (
      ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    ) {
      startCamera()
    } else {
      Log.w(TAG, "Camera permission not granted for Intruder Snap")
    }
  }

  private fun startCamera() {
    val helper = com.example.presencedetector.utils.CameraHelper(this)
    if (textureView.isAvailable) {
      helper.captureSelfie(textureView.surfaceTexture!!, { bytes -> saveAndSendPhoto(bytes) })
    } else {
      textureView.surfaceTextureListener =
        object : TextureView.SurfaceTextureListener {
          override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            helper.captureSelfie(surface, { bytes -> saveAndSendPhoto(bytes) })
          }

          override fun onSurfaceTextureSizeChanged(
            surface: SurfaceTexture,
            width: Int,
            height: Int
          ) {}

          override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true

          override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
    }
  }

  private fun saveAndSendPhoto(bytes: ByteArray) {
    try {
      val filename =
        "THIEF_TRAP_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".jpg"
      val file = File(getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), filename)
      FileOutputStream(file).use { it.write(bytes) }

      Log.d(TAG, "Captured Intruder Photo: ${file.absolutePath}")
      telegramService.sendPhoto(file, "🚨 INTRUDER ALERT! Photo captured from device.")
    } catch (e: Exception) {
      Log.e(TAG, "Failed to save photo", e)
    }
  }
}
