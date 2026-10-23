package com.example.presencedetector.presence

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request

/** Minimal HTTP GET surface so the client can be tested without the network. */
fun interface HttpGetter {
  /** Returns the response body; throws [PresenceException] on failure. */
  fun get(url: String, headers: Map<String, String>): String
}

class OkHttpGetter(timeoutSec: Long = 8) : HttpGetter {
  private val client =
    OkHttpClient.Builder()
      .connectTimeout(timeoutSec, TimeUnit.SECONDS)
      .readTimeout(timeoutSec, TimeUnit.SECONDS)
      .build()

  override fun get(url: String, headers: Map<String, String>): String {
    val request =
      try {
        Request.Builder().url(url).apply { headers.forEach { (k, v) -> header(k, v) } }.build()
      } catch (e: IllegalArgumentException) {
        throw PresenceException("Endereço inválido: $url", e)
      }
    try {
      client.newCall(request).execute().use { response ->
        if (response.code == 401) throw PresenceException("Chave de API recusada pelo backend")
        if (!response.isSuccessful) throw PresenceException("HTTP ${response.code} em $url")
        return response.body?.string() ?: throw PresenceException("Resposta vazia")
      }
    } catch (e: java.io.IOException) {
      throw PresenceException("Backend inacessível: ${e.message}", e)
    }
  }
}
