package com.example.presencedetector.services

import android.content.Context
import android.util.Log
import com.example.presencedetector.utils.PreferencesUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Service to handle Telegram notifications using OkHttp.
 */
class TelegramService(private val context: Context) {
    companion object {
        private const val TAG = "TelegramService"

        // Share client to use connection pooling
        private val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
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
                val urlString = "https://api.telegram.org/bot$token/sendMessage"

                val formBody = FormBody.Builder()
                    .add("chat_id", chatId)
                    .add("text", message)
                    .build()

                val request = Request.Builder()
                    .url(urlString)
                    .post(formBody)
                    .build()

                Log.d(TAG, "Sending message to chatId: $chatId")

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.d(TAG, "Message sent successfully")
                    } else {
                        Log.e(TAG, "Failed to send message: ${response.code} ${response.message}")
                        val errorBody = response.body?.string()
                        Log.e(TAG, "Error response: $errorBody")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error sending Telegram message", e)
            }
        }
    }
}
