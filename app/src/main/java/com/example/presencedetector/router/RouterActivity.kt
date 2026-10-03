package com.example.presencedetector.router

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.presencedetector.R
import com.example.presencedetector.databinding.ActivityRouterBinding
import com.example.presencedetector.databinding.ItemRouterNetworkBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Connects to the TP-Link router, shows its networks and who is connected, and manages trust. */
class RouterActivity : AppCompatActivity() {
  private lateinit var binding: ActivityRouterBinding
  private lateinit var prefs: RouterPreferences
  private val adapter = RouterDeviceAdapter { toggleTrust(it) }
  private var lastSnapshot: RouterSnapshot? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityRouterBinding.inflate(layoutInflater)
    setContentView(binding.root)
    prefs = RouterPreferences(this)

    binding.toolbar.setNavigationOnClickListener { finish() }
    binding.rvDevices.layoutManager = LinearLayoutManager(this)
    binding.rvDevices.adapter = adapter
    binding.etHost.setText(prefs.host)
    binding.etUser.setText(prefs.username)
    binding.tvRouterHost.text = prefs.host
    binding.switchMonitor.isChecked = prefs.monitoringEnabled
    binding.switchMonitor.setOnCheckedChangeListener { _, on -> prefs.monitoringEnabled = on }
    binding.btnSave.setOnClickListener { save() }
    binding.fabRefresh.setOnClickListener { refresh() }
    binding.btnTrustAll.setOnClickListener { trustAll() }
    if (prefs.isConfigured()) refresh()
  }

  private fun save() {
    val password = binding.etPassword.text?.toString().orEmpty()
    if (password.isEmpty() && prefs.password == null) {
      Toast.makeText(this, R.string.router_need_password, Toast.LENGTH_SHORT).show()
      return
    }
    prefs.host = binding.etHost.text?.toString().orEmpty()
    prefs.username = binding.etUser.text?.toString().orEmpty()
    if (password.isNotEmpty()) prefs.password = password
    binding.etPassword.text = null
    binding.tvRouterHost.text = prefs.host
    refresh()
  }

  private fun refresh() {
    if (!prefs.isConfigured()) return
    setLoading(true)
    lifecycleScope.launch {
      val result = runCatching {
        withContext(Dispatchers.IO) { RouterScanner(applicationContext).scan(background = false) }
      }
      setLoading(false)
      result.onSuccess(::render).onFailure {
        val reason = it.message ?: it.javaClass.simpleName
        binding.tvRouterStatus.text = getString(R.string.router_status_error, reason)
      }
    }
  }

  private fun render(snapshot: RouterSnapshot) {
    lastSnapshot = snapshot
    val trusted = prefs.trustedMacs
    val unknown = snapshot.devices.count { it.mac !in trusted }
    binding.tvRouterStatus.text =
      getString(R.string.router_status_ok, snapshot.devices.size, unknown)
    adapter.submit(snapshot.devices.sortedBy { it.mac in trusted }, trusted)
    renderNetworks(snapshot.networks)
  }

  private fun renderNetworks(networks: List<RouterNetwork>) {
    binding.networksContainer.removeAllViews()
    if (networks.isEmpty()) {
      Toast.makeText(this, R.string.router_no_networks, Toast.LENGTH_SHORT).show()
    }
    networks
      .sortedBy { it.guest }
      .forEach { network ->
        val row =
          ItemRouterNetworkBinding.inflate(
            LayoutInflater.from(this),
            binding.networksContainer,
            false,
          )
        row.tvSsid.text = network.ssid
        val state = if (network.enabled) "" else " • desligada"
        row.tvBand.text = "${network.band}${if (network.guest) " • convidados" else ""}$state"
        binding.networksContainer.addView(row.root)
      }
  }

  private fun toggleTrust(device: RouterClientDevice) {
    prefs.setTrusted(device.mac, device.mac !in prefs.trustedMacs)
    lastSnapshot?.let(::render)
  }

  private fun trustAll() {
    val snapshot = lastSnapshot ?: return
    prefs.trustedMacs = prefs.trustedMacs + snapshot.devices.map { it.mac }
    render(snapshot)
  }

  private fun setLoading(loading: Boolean) {
    binding.progress.visibility = if (loading) android.view.View.VISIBLE else android.view.View.GONE
    if (loading) binding.tvRouterStatus.setText(R.string.router_status_loading)
  }
}
