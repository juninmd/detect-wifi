package com.example.presencedetector.services

import android.content.Context
import android.util.Log
import com.example.presencedetector.utils.PreferencesUtil
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody

/** Service to handle Telegram notifications using OkHttp. */
open class TelegramService(
  private val context: Context,
  var preferences: PreferencesUtil? = null,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
  companion object {
    private const val TAG = "TelegramService"
    private const val TIMEOUT_SEC = 30L

    // Singleton OkHttpClient to reuse connection pool and threads
    var client: OkHttpClient =
      OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
        .build()
  }

  private val prefs: PreferencesUtil by lazy { preferences ?: PreferencesUtil(context) }

  open fun sendMessage(message: String) {
    executeTelegramAction("Message sent successfully", "Failed to send message") {
        val (token, chatId) = getCredentials() ?: return@executeTelegramAction null

        val url = "https://api.telegram.org/bot$token/sendMessage"

        val requestBody =
          MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", chatId)
            .addFormDataPart("text", message)
            .build()

        Request.Builder().url(url).post(requestBody).build()
    }
  }

  open fun sendPhoto(photoFile: File, caption: String = "") {
    executeTelegramAction("Photo sent successfully", "Failed to send photo") {
        val (token, chatId) = getCredentials() ?: return@executeTelegramAction null

        if (!photoFile.exists()) {
          Log.e(TAG, "Photo file does not exist: ${photoFile.absolutePath}")
          return@executeTelegramAction null
        }

        val url = "https://api.telegram.org/bot$token/sendPhoto"
        val mediaType = "image/jpeg".toMediaTypeOrNull()
        val fileBody = photoFile.asRequestBody(mediaType)

        val requestBody =
          MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", chatId)
            .addFormDataPart("caption", caption)
            .addFormDataPart("photo", photoFile.name, fileBody)
            .build()

        Request.Builder().url(url).post(requestBody).build()
    }
  }

  open fun sendAudio(audioFile: File, caption: String = "") {
    executeTelegramAction("Audio sent successfully", "Failed to send audio") {
        val (token, chatId) = getCredentials() ?: return@executeTelegramAction null

        if (!audioFile.exists()) {
          Log.e(TAG, "Audio file does not exist: ${audioFile.absolutePath}")
          return@executeTelegramAction null
        }

        val url = "https://api.telegram.org/bot$token/sendAudio"
        val mediaType = "audio/mp4".toMediaTypeOrNull()
        val fileBody = audioFile.asRequestBody(mediaType)

        val requestBody =
          MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", chatId)
            .addFormDataPart("caption", caption)
            .addFormDataPart("audio", audioFile.name, fileBody)
            .build()

        Request.Builder().url(url).post(requestBody).build()
    }
  }

  private fun getCredentials(): Pair<String, String>? {
    if (!prefs.isTelegramEnabled()) return null

    val token = prefs.getTelegramToken()
    val chatId = prefs.getTelegramChatId()

    if (token.isNullOrEmpty() || chatId.isNullOrEmpty()) {
      Log.w(TAG, "Telegram credentials missing")
      return null
    }
    return Pair(token, chatId)
  }

  private fun executeTelegramAction(successMsg: String, errorMsg: String, action: () -> Request?) {
    CoroutineScope(ioDispatcher).launch {
      try {
        val request = action() ?: return@launch
        client.newCall(request).execute().use { response ->
          if (response.isSuccessful) {
            Log.d(TAG, successMsg)
          } else {
            Log.e(TAG, "$errorMsg: ${response.code} ${response.message}")
            Log.e(TAG, "Response: ${response.body?.string()}")
          }
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error performing Telegram request: $errorMsg", e)
      }
    }
  }
}
