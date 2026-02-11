package com.example.presencedetector.security.model

/**
 * Representa um canal de câmera conectado ao DVR via RTSP.
 *
 * @param id Identificador único do canal
 * @param name Nome amigável para exibição (ex: "Câmera Garagem")
 * @param host IP ou hostname do DVR
 * @param port Porta RTSP (padrão 554)
 * @param channel Número do canal no DVR
 * @param username Usuário para autenticação RTSP
 * @param password Senha para autenticação RTSP
 * @param streamPath Caminho do stream (varia por fabricante)
 */
data class CameraChannel(
  val id: Int,
  val name: String,
  val host: String,
  val port: Int = 554,
  val channel: Int = 1,
  val username: String = "",
  val password: String = "",
  val streamPath: String = "cam/realmonitor", // Caminho padrão Intelbras
  val isHidden: Boolean = false // Ocultar câmera no dashboard
) {
  /**
   * Constrói a URL RTSP completa para conexão com o DVR.
   *
   * Formato Intelbras: rtsp://user:pass@ip:port/cam/realmonitor?channel=X&subtype=0
   * - subtype=0: Stream principal (alta qualidade)
   * - subtype=1: Stream secundário (baixa qualidade, menos consumo)
   */
  val rtspUrl: String
    get() = buildString {
      append("rtsp://")
      if (username.isNotEmpty()) {
        append(username)
        append(":")
        append(password)
        append("@")
      }
      append(host)
      append(":")
      append(port)
      append("/")
      append(streamPath)
      append("?channel=")
      append(channel)
      append("&subtype=0") // Stream principal para melhor detecção
    }

  /**
   * URL RTSP com substream (menor qualidade, menos CPU/banda). Útil para monitoramento em
   * background.
   */
  val rtspUrlSubstream: String
    get() = rtspUrl.replace("subtype=0", "subtype=1")

  companion object {
    /** Cria um canal a partir de uma URL RTSP completa. */
    fun fromUrl(id: Int, name: String, url: String): CameraChannel {
      val regex = Regex("""rtsp://(?:([^:]+):([^@]+)@)?([^:]+):(\d+)/([^?]+)\?channel=(\d+)""")
      val match = regex.find(url)

      return if (match != null) {
        val (user, pass, host, port, path, channel) = match.destructured
        CameraChannel(
          id = id,
          name = name,
          host = host,
          port = port.toIntOrNull() ?: 554,
          channel = channel.toIntOrNull() ?: 1,
          username = user,
          password = pass,
          streamPath = path
        )
      } else {
        CameraChannel(id = id, name = name, host = "")
      }
    }
  }
}
