package com.example.presencedetector.services

import android.content.Context
import android.util.Log
import com.example.presencedetector.utils.PreferencesUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Service to handle Telegram notifications.
 */
class TelegramService(private val context: Context) {
    companion object {
        private const val TAG = "TelegramService"
        private const val TIMEOUT = 10000 // 10 seconds
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
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection

                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.connectTimeout = TIMEOUT
                conn.readTimeout = TIMEOUT
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                val data = "chat_id=" + URLEncoder.encode(chatId, "UTF-8") +
                           "&text=" + URLEncoder.encode(message, "UTF-8")

                Log.d(TAG, "Sending message to chatId: $chatId")

                OutputStreamWriter(conn.outputStream).use { writer ->
                    writer.write(data)
                    writer.flush()
                }

                val responseCode = conn.responseCode
                if (responseCode == 200) {
                    Log.d(TAG, "Message sent successfully")
                } else {
                    Log.e(TAG, "Failed to send message: $responseCode")
                    try {
                        val errorStream = conn.errorStream
                        val response = errorStream?.bufferedReader()?.use { it.readText() }
                        Log.e(TAG, "Error response: $response")
                    } catch (e: Exception) {
                        Log.e(TAG, "Could not read error stream", e)
                    }
                }
                conn.disconnect()

            } catch (e: Exception) {
                Log.e(TAG, "Error sending Telegram message", e)
            }
        }
    }
}
