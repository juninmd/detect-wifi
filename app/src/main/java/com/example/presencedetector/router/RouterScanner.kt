package com.example.presencedetector.router

import android.content.Context
import android.util.Log
import com.example.presencedetector.router.tplink.TpLinkClient
import com.example.presencedetector.services.TelegramService
import com.example.presencedetector.utils.NotificationUtil

/** Fetches the router state and alerts (notification + Telegram) about strangers that joined. */
class RouterScanner(
  private val context: Context,
  private val prefs: RouterPreferences = RouterPreferences(context),
  private val clientFactory: (RouterPreferences) -> RouterClient = ::defaultClient,
) {
  companion object {
    private const val TAG = "RouterScanner"

    fun defaultClient(p: RouterPreferences): RouterClient =
      TpLinkClient(p.host, p.username, p.password.orEmpty(), OkHttpTransport())
  }

  /**
   * Blocking. Returns the snapshot, or throws [RouterException] when the router cannot be read.
   * Only a [background] scan alerts and moves the baseline; manual scans just read.
   */
  fun scan(background: Boolean): RouterSnapshot {
    val snapshot = clientFactory(prefs).fetchSnapshot()
    val macs = snapshot.devices.map { it.mac }.toSet()
    if (background) alert(RouterMonitor.evaluate(snapshot, prefs.trustedMacs, prefs.lastSeenMacs))
    if (background || prefs.lastSeenMacs == null) prefs.lastSeenMacs = macs
    return snapshot
  }

  private fun alert(diff: RouterMonitor.Diff) {
    diff.joinedUnknown.forEach { device ->
      val where = device.connection.label
      val text = "${device.displayName} (${device.ip ?: "sem IP"}, $where)\nMAC ${device.mac}"
      Log.w(TAG, "Unknown device joined: ${device.mac}")
      NotificationUtil.sendPresenceNotification(
        context,
        "📡 Dispositivo desconhecido na sua rede",
        text,
        true,
        channelType = NotificationUtil.HOME_SECURITY_CHANNEL_ID,
      )
      TelegramService(context).sendMessage("📡 Dispositivo desconhecido na rede\n$text")
    }
  }
}
