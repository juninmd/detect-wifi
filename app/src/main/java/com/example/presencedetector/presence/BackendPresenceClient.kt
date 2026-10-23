package com.example.presencedetector.presence

/** [PresenceClient] that queries the NestJS/Node backend (`GET /presence`) with an API key. */
class BackendPresenceClient(
  baseUrl: String,
  private val apiKey: String,
  private val http: HttpGetter = OkHttpGetter(),
) : PresenceClient {
  private val base =
    baseUrl.trim().trimEnd('/').let {
      if (it.startsWith("http://") || it.startsWith("https://")) it else "http://$it"
    }

  override fun fetch(windowSeconds: Int): PresenceSnapshot =
    PresenceParser.parse(
      http.get("$base/presence?window=$windowSeconds", mapOf("x-api-key" to apiKey))
    )
}
