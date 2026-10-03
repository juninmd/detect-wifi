package com.example.presencedetector.router

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.presencedetector.R
import com.example.presencedetector.databinding.ItemRouterDeviceBinding

/** Lists the devices connected to the router, flagging the ones that are not trusted. */
class RouterDeviceAdapter(private val onToggleTrust: (RouterClientDevice) -> Unit) :
  RecyclerView.Adapter<RouterDeviceAdapter.Holder>() {
  private var items: List<RouterClientDevice> = emptyList()
  private var trusted: Set<String> = emptySet()

  class Holder(val binding: ItemRouterDeviceBinding) : RecyclerView.ViewHolder(binding.root)

  fun submit(devices: List<RouterClientDevice>, trustedMacs: Set<String>) {
    items = devices
    trusted = trustedMacs
    notifyDataSetChanged()
  }

  override fun getItemCount() = items.size

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
    Holder(ItemRouterDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false))

  override fun onBindViewHolder(holder: Holder, position: Int) {
    val device = items[position]
    val isTrusted = device.mac in trusted
    val b = holder.binding
    val context = b.root.context
    b.tvName.text = device.displayName
    b.tvDetails.text = "${device.ip ?: "—"} • ${device.connection.label}\n${device.mac}"
    b.ivConnection.setImageResource(
      if (device.connection == Connection.WIRED) R.drawable.ic_router else R.drawable.ic_wifi_signal
    )
    b.tvBadge.setText(
      if (isTrusted) R.string.router_badge_trusted else R.string.router_badge_unknown
    )
    b.tvBadge.setBackgroundResource(
      if (isTrusted) R.drawable.bg_badge_trusted else R.drawable.bg_badge_unknown
    )
    b.tvBadge.setTextColor(
      ContextCompat.getColor(
        context,
        if (isTrusted) R.color.success_color else R.color.warning_color,
      )
    )
    b.btnTrust.setText(if (isTrusted) R.string.router_untrust else R.string.router_trust)
    b.btnTrust.setOnClickListener { onToggleTrust(device) }
  }
}
