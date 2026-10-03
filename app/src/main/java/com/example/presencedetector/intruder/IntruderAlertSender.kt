package com.example.presencedetector.intruder

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Environment
import android.util.Log
import com.example.presencedetector.security.repository.LogRepository
import com.example.presencedetector.services.TelegramService
import com.example.presencedetector.utils.CameraHelper
import com.example.presencedetector.utils.NotificationUtil
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Collects a selfie and the current location in parallel and sends both to Telegram. Each part is
 * best effort: a missing photo or location never blocks the alert itself.
 */
object IntruderAlertSender {
  private const val TAG = "IntruderAlertSender"

  fun report(context: Context, reason: String) {
    val app = context.applicationContext
    val timeMs = System.currentTimeMillis()
    val join =
      Join<ByteArray?, GeoPoint?> { photo, point -> deliver(app, reason, timeMs, photo, point) }

    LocationProvider(app).current { join.second(it) }
    CameraHelper(app)
      .captureSelfie(
        onImageCaptured = { join.first(it) },
        onError = {
          Log.w(TAG, "No selfie: ${it.message}")
          join.first(null)
        },
      )
  }

  private fun deliver(
    context: Context,
    reason: String,
    timeMs: Long,
    photo: ByteArray?,
    point: GeoPoint?,
  ) {
    val caption = IntruderReport.caption(reason, timeMs, point)
    val telegram = TelegramService(context)
    val file = photo?.let { save(context, it, timeMs) }
    if (file != null) telegram.sendPhoto(file, caption) else telegram.sendMessage(caption)
    point?.let { telegram.sendLocation(it.latitude, it.longitude) }
    LogRepository.logSystemEvent(context, "🚨 $reason")
    photo?.let {
      NotificationUtil.sendIntruderAlert(context, BitmapFactory.decodeByteArray(it, 0, it.size))
    }
  }

  private fun save(context: Context, bytes: ByteArray, timeMs: Long): File {
    val name =
      "INTRUDER_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(timeMs)) + ".jpg"
    return File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), name).also {
      it.writeBytes(bytes)
    }
  }

  /** Runs [onBoth] once both the first and the second value have arrived (in any order). */
  private class Join<A, B>(private val onBoth: (A, B) -> Unit) {
    private var a: Holder<A>? = null
    private var b: Holder<B>? = null

    private class Holder<T>(val value: T)

    fun first(value: A) =
      synchronized(this) {
        a = Holder(value)
        fire()
      }

    fun second(value: B) =
      synchronized(this) {
        b = Holder(value)
        fire()
      }

    private fun fire() {
      val x = a
      val y = b
      if (x != null && y != null) Thread { onBoth(x.value, y.value) }.start()
    }
  }
}
