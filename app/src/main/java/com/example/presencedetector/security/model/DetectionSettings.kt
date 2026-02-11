package com.example.presencedetector.security.model

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/** Configurações de detecção de segurança persistidas em SharedPreferences. */
data class DetectionSettings(
  // Configurações de conexão
  val dvrHost: String = "",
  val dvrPort: Int = 554,
  val username: String = "admin",
  val password: String = "",

  // Configurações de detecção
  /** Tempo em segundos que a pessoa precisa permanecer para disparar alerta */
  val detectionThresholdSeconds: Int = 5,

  /**
   * Período de graça em segundos para tolerar glitches no stream. Se a detecção falhar por menos
   * que esse tempo e voltar, o timer NÃO é resetado.
   */
  val gracePeriodSeconds: Int = 1,

  /**
   * Cooldown em segundos entre notificações para a mesma câmera. Evita spam de notificações se a
   * pessoa ficar no local.
   */
  val notificationCooldownSeconds: Int = 60,

  // Configurações de monitoramento
  /** Se o monitoramento em background está ativo */
  val monitoringEnabled: Boolean = false,

  /** IDs dos canais habilitados para monitoramento */
  val enabledChannelIds: Set<Int> = emptySet(),

  /** Lista de canais configurados */
  val channels: List<CameraChannel> = emptyList()
) {
  /** Tempo de detecção em milissegundos. */
  val detectionThresholdMs: Long
    get() = detectionThresholdSeconds * 1000L

  /** Período de graça em milissegundos. */
  val gracePeriodMs: Long
    get() = gracePeriodSeconds * 1000L

  /** Cooldown de notificação em milissegundos. */
  val notificationCooldownMs: Long
    get() = notificationCooldownSeconds * 1000L

  companion object {
    private const val PREFS_NAME = "security_detection_settings"
    private const val KEY_DVR_HOST = "dvr_host"
    private const val KEY_DVR_PORT = "dvr_port"
    private const val KEY_USERNAME = "username"
    private const val KEY_PASSWORD = "password"
    private const val KEY_DETECTION_THRESHOLD = "detection_threshold"
    private const val KEY_GRACE_PERIOD = "grace_period"
    private const val KEY_NOTIFICATION_COOLDOWN = "notification_cooldown"
    private const val KEY_MONITORING_ENABLED = "monitoring_enabled"
    private const val KEY_ENABLED_CHANNELS = "enabled_channels"
    private const val KEY_CHANNELS_JSON = "channels_json"

    private val gson = Gson()

    /** Carrega as configurações do SharedPreferences. */
    fun load(context: Context): DetectionSettings {
      val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

      val channelsJson = prefs.getString(KEY_CHANNELS_JSON, "[]") ?: "[]"
      val channelsType = object : TypeToken<List<CameraChannel>>() {}.type
      val channels: List<CameraChannel> =
        try {
          gson.fromJson(channelsJson, channelsType) ?: emptyList()
        } catch (e: Exception) {
          emptyList()
        }

      val enabledChannelsStr = prefs.getStringSet(KEY_ENABLED_CHANNELS, emptySet()) ?: emptySet()
      val enabledChannelIds = enabledChannelsStr.mapNotNull { it.toIntOrNull() }.toSet()

      return DetectionSettings(
        dvrHost = prefs.getString(KEY_DVR_HOST, "") ?: "",
        dvrPort = prefs.getInt(KEY_DVR_PORT, 554),
        username = prefs.getString(KEY_USERNAME, "admin") ?: "admin",
        password = prefs.getString(KEY_PASSWORD, "") ?: "",
        detectionThresholdSeconds = prefs.getInt(KEY_DETECTION_THRESHOLD, 5),
        gracePeriodSeconds = prefs.getInt(KEY_GRACE_PERIOD, 1),
        notificationCooldownSeconds = prefs.getInt(KEY_NOTIFICATION_COOLDOWN, 60),
        monitoringEnabled = prefs.getBoolean(KEY_MONITORING_ENABLED, false),
        enabledChannelIds = enabledChannelIds,
        channels = channels
      )
    }

    /** Salva as configurações no SharedPreferences. */
    fun save(context: Context, settings: DetectionSettings) {
      val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

      val channelsJson = gson.toJson(settings.channels)
      val enabledChannelsStr = settings.enabledChannelIds.map { it.toString() }.toSet()

      prefs
        .edit()
        .putString(KEY_DVR_HOST, settings.dvrHost)
        .putInt(KEY_DVR_PORT, settings.dvrPort)
        .putString(KEY_USERNAME, settings.username)
        .putString(KEY_PASSWORD, settings.password)
        .putInt(KEY_DETECTION_THRESHOLD, settings.detectionThresholdSeconds)
        .putInt(KEY_GRACE_PERIOD, settings.gracePeriodSeconds)
        .putInt(KEY_NOTIFICATION_COOLDOWN, settings.notificationCooldownSeconds)
        .putBoolean(KEY_MONITORING_ENABLED, settings.monitoringEnabled)
        .putStringSet(KEY_ENABLED_CHANNELS, enabledChannelsStr)
        .putString(KEY_CHANNELS_JSON, channelsJson)
        .apply()
    }
  }

  /** Cria uma cópia com os canais atualizados usando as configurações globais de conexão. */
  fun withUpdatedChannelConnections(): DetectionSettings {
    val updatedChannels =
      channels.map { channel ->
        channel.copy(host = dvrHost, port = dvrPort, username = username, password = password)
      }
    return copy(channels = updatedChannels)
  }

  /** Adiciona um novo canal. */
  fun addChannel(name: String, channelNumber: Int): DetectionSettings {
    val newId = (channels.maxOfOrNull { it.id } ?: 0) + 1
    val newChannel =
      CameraChannel(
        id = newId,
        name = name,
        host = dvrHost,
        port = dvrPort,
        channel = channelNumber,
        username = username,
        password = password
      )
    return copy(channels = channels + newChannel, enabledChannelIds = enabledChannelIds + newId)
  }

  /** Remove um canal pelo ID. */
  fun removeChannel(channelId: Int): DetectionSettings {
    return copy(
      channels = channels.filter { it.id != channelId },
      enabledChannelIds = enabledChannelIds - channelId
    )
  }
}
