package com.example.presencedetector.presence

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.presencedetector.R
import com.example.presencedetector.databinding.ActivityPresenceBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Configures the presence backend and shows who the passive sensor sees nearby. */
class PresenceActivity : AppCompatActivity() {
  private lateinit var binding: ActivityPresenceBinding
  private lateinit var prefs: PresencePreferences

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityPresenceBinding.inflate(layoutInflater)
    setContentView(binding.root)
    prefs = PresencePreferences(this)

    binding.toolbar.setNavigationOnClickListener { finish() }
    binding.etUrl.setText(prefs.baseUrl)
    binding.switchMonitor.isChecked = prefs.monitoringEnabled
    binding.switchMonitor.setOnCheckedChangeListener { _, on -> prefs.monitoringEnabled = on }
    binding.btnSave.setOnClickListener { save() }
    binding.fabRefresh.setOnClickListener { refresh() }
    if (prefs.isConfigured()) refresh()
  }

  private fun save() {
    val key = binding.etApiKey.text?.toString().orEmpty()
    if (key.isEmpty() && prefs.apiKey == null) {
      Toast.makeText(this, R.string.presence_need_key, Toast.LENGTH_SHORT).show()
      return
    }
    prefs.baseUrl = binding.etUrl.text?.toString().orEmpty()
    if (key.isNotEmpty()) prefs.apiKey = key
    binding.etApiKey.text = null
    refresh()
  }

  private fun refresh() {
    if (!prefs.isConfigured()) return
    setLoading(true)
    lifecycleScope.launch {
      val result = runCatching {
        withContext(Dispatchers.IO) { PresenceScanner(applicationContext).scan(background = false) }
      }
      setLoading(false)
      result.onSuccess(::render).onFailure {
        val reason = it.message ?: it.javaClass.simpleName
        binding.tvStatus.text = getString(R.string.presence_status_error, reason)
      }
    }
  }

  private fun render(snapshot: PresenceSnapshot) {
    binding.tvStatus.text =
      getString(
        R.string.presence_status_ok,
        snapshot.known.size,
        snapshot.unknown.size,
        snapshot.anonymousCount,
      )
    binding.tvKnown.text = describe(snapshot.known)
    binding.tvUnknown.text = describe(snapshot.unknown)
  }

  private fun describe(devices: List<PresentDevice>): String =
    if (devices.isEmpty()) getString(R.string.presence_none)
    else devices.joinToString("\n") { "${it.displayName} • ${it.mac} • ${it.rssi} dBm" }

  private fun setLoading(loading: Boolean) {
    binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
    if (loading) binding.tvStatus.setText(R.string.presence_status_loading)
  }
}
