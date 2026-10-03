package com.example.presencedetector.router

import java.util.concurrent.TimeUnit
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

/** [HttpTransport] backed by OkHttp with an in-memory cookie jar (the router uses `sysauth`). */
class OkHttpTransport(timeoutSec: Long = 8) : HttpTransport {
  private val cookies = mutableMapOf<String, List<Cookie>>()
  private val client =
    OkHttpClient.Builder()
      .connectTimeout(timeoutSec, TimeUnit.SECONDS)
      .readTimeout(timeoutSec, TimeUnit.SECONDS)
      .cookieJar(
        object : CookieJar {
          override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            this@OkHttpTransport.cookies[url.host] = cookies
          }

          override fun loadForRequest(url: HttpUrl) = cookies[url.host].orEmpty()
        }
      )
      .build()

  override fun postForm(url: String, form: Map<String, String>): String {
    val body = FormBody.Builder().apply { form.forEach { (k, v) -> add(k, v) } }.build()
    val request = Request.Builder().url(url).post(body).build()
    try {
      client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw RouterException("HTTP ${response.code} em $url")
        return response.body?.string() ?: throw RouterException("Resposta vazia")
      }
    } catch (e: java.io.IOException) {
      throw RouterException("Roteador inacessível: ${e.message}", e)
    }
  }
}
