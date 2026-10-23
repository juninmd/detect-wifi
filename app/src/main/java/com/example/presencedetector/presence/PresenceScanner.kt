package com.example.presencedetector.presence

import android.content.Context
import android.util.Log
import com.example.presencedetector.services.TelegramService
import com.example.presencedetector.utils.NotificationUtil

/** Polls the presence backend and alerts (notification + Telegram) about new strangers nearby. */
class PresenceScanner(
  private val context: Context,
  private val prefs: PresencePreferences = PresencePreferences(context),
  private val clientFactory: (PresencePreferences) -> PresenceClient = ::defaultClient,
) {
  companion object {
    private const val TAG = "PresenceScanner"

    fun defaultClient(p: PresencePreferences): PresenceClient =
      BackendPresenceClient(p.baseUrl, p.apiKey.orEmpty())
  }

  /**
   * Blocking. Throws [PresenceException] when the backend cannot be read. Only a [background]
   * scan alerts and moves the baseline; manual scans just read.
   */
  fun scan(background: Boolean): PresenceSnapshot {
    val snapshot = clientFactory(prefs).fetch(PresencePreferences.WINDOW_SECONDS)
    val unknown = snapshot.unknown.map { it.mac }.toSet()
    if (background) alert(PresenceMonitor.joinedUnknown(snapshot, prefs.lastUnknownMacs))
    if (background || prefs.lastUnknownMacs == null) prefs.lastUnknownMacs = unknown
    return snapshot
  }

  private fun alert(joined: List<PresentDevice>) {
    joined.forEach { device ->
      val text = "${device.displayName}\nMAC ${device.mac} • ${device.rssi} dBm"
      Log.w(TAG, "Unknown device nearby: ${device.mac}")
      NotificationUtil.sendPresenceNotification(
        context,
        "📡 Dispositivo desconhecido por perto",
        text,
        true,
        channelType = NotificationUtil.HOME_SECURITY_CHANNEL_ID,
      )
      TelegramService(context).sendMessage("📡 Dispositivo desconhecido por perto\n$text")
    }
  }
}
