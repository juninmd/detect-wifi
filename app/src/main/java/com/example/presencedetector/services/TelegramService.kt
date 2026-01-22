package com.example.presencedetector.services

import android.content.Context
import android.util.Log
import com.example.presencedetector.utils.PreferencesUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Service to handle Telegram notifications using OkHttp.
 */
class TelegramService(private val context: Context) {
    companion object {
        private const val TAG = "TelegramService"
        private const val TIMEOUT_SEC = 30L

        // Singleton OkHttpClient to reuse connection pool and threads
        private val client by lazy {
            OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
                .build()
        }
    }

    private val preferences = PreferencesUtil(context)

    fun sendMessage(message: String) {
        if (!preferences.isTelegramEnabled()) return

        val token = preferences.getTelegramToken()
        val chatId = preferences.getTelegramChatId()

        if (token.isNullOrEmpty() || chatId.isNullOrEmpty()) {
            Log.w(TAG, "Telegram credentials missing")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "https://api.telegram.org/bot$token/sendMessage"

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("chat_id", chatId)
                    .addFormDataPart("text", message)
                    .build()

                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.d(TAG, "Message sent successfully")
                    } else {
                        Log.e(TAG, "Failed to send message: ${response.code} ${response.message}")
                        Log.e(TAG, "Response: ${response.body?.string()}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending Telegram message", e)
            }
        }
    }

    fun sendPhoto(photoFile: File, caption: String = "") {
        if (!preferences.isTelegramEnabled()) return

        val token = preferences.getTelegramToken()
        val chatId = preferences.getTelegramChatId()

        if (token.isNullOrEmpty() || chatId.isNullOrEmpty()) {
            Log.w(TAG, "Telegram credentials missing")
            return
        }

        if (!photoFile.exists()) {
            Log.e(TAG, "Photo file does not exist: ${photoFile.absolutePath}")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "https://api.telegram.org/bot$token/sendPhoto"

                val mediaType = "image/jpeg".toMediaTypeOrNull()
                val fileBody = photoFile.asRequestBody(mediaType)

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("chat_id", chatId)
                    .addFormDataPart("caption", caption)
                    .addFormDataPart("photo", photoFile.name, fileBody)
                    .build()

                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.d(TAG, "Photo sent successfully")
                    } else {
                        Log.e(TAG, "Failed to send photo: ${response.code} ${response.message}")
                        Log.e(TAG, "Response: ${response.body?.string()}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending Telegram photo", e)
            }
        }
    }
}
