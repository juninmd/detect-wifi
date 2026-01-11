package com.example.presencedetector

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.presencedetector.model.DeviceCategory
import com.example.presencedetector.model.WiFiDevice
import com.example.presencedetector.services.PresenceDetectionManager
import com.example.presencedetector.ui.RadarView
import com.example.presencedetector.utils.PreferencesUtil
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.radiobutton.MaterialRadioButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WifiRadarActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var radarView: RadarView
    private lateinit var detectionManager: PresenceDetectionManager
    private lateinit var preferences: PreferencesUtil
    private lateinit var adapter: WifiAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wifi_radar)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(android.R.drawable.ic_menu_revert)
        
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        preferences = PreferencesUtil(this)
        radarView = findViewById(R.id.radarView)
        recyclerView = findViewById(R.id.wifiRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        adapter = WifiAdapter(
            onItemClick = { device -> openDeviceDetails(device) }
        )
        recyclerView.adapter = adapter

        detectionManager = PresenceDetectionManager(this)
        detectionManager.setPresenceListener { _, _, devices, _ ->
            runOnUiThread {
                val processedDevices = devices.map { device ->
                    device.apply { 
                        manualCategory = preferences.getManualCategory(bssid)
                        nickname = preferences.getNickname(bssid)
                    }
                }
                adapter.updateDevices(processedDevices)
                radarView.updateDevices(processedDevices)
            }
        }
    }

    private fun openDeviceDetails(device: WiFiDevice) {
        val intent = android.content.Intent(this, DeviceDetailActivity::class.java).apply {
            putExtra(DeviceDetailActivity.EXTRA_BSSID, device.bssid)
            putExtra(DeviceDetailActivity.EXTRA_SSID, device.ssid)
            putExtra(DeviceDetailActivity.EXTRA_SIGNAL, device.level)
            putExtra(DeviceDetailActivity.EXTRA_LAST_SEEN, device.lastSeen)
        }
        startActivity(intent)
    }

    override fun onStart() {
        super.onStart()
        detectionManager.startDetection()
    }

    override fun onStop() {
        super.onStop()
        detectionManager.stopDetection()
    }

    inner class WifiAdapter(
        private val onItemClick: (WiFiDevice) -> Unit
    ) : RecyclerView.Adapter<WifiAdapter.ViewHolder>() {
        private var devices: List<WiFiDevice> = emptyList()

        fun updateDevices(newDevices: List<WiFiDevice>) {
            devices = newDevices.sortedByDescending { it.level }
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_wifi_device, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val device = devices[position]
            val nickname = preferences.getNickname(device.bssid)
            val hasNotify = preferences.shouldNotifyArrival(device.bssid) || preferences.shouldNotifyDeparture(device.bssid)
            
            holder.tvIcon.text = device.category.iconRes
            
            if (device.isHidden) {
                holder.tvName.text = "🔒 [Hidden Network]"
            } else {
                holder.tvName.text = nickname ?: device.ssid
            }

            // Highlight nicknamed devices with a different color
            if (nickname != null) {
                holder.tvName.setTextColor(getColor(R.color.primary_color))
            } else {
                holder.tvName.setTextColor(getColor(R.color.dark_text))
            }

            val details = "${device.category.displayName} • ${device.level} dBm"
            holder.tvDetails.text = details

            if (hasNotify) {
                holder.tvNotificationStatus.visibility = View.VISIBLE
            } else {
                holder.tvNotificationStatus.visibility = View.GONE
            }
            
            holder.itemView.setOnClickListener { onItemClick(device) }
        }

        override fun getItemCount(): Int = devices.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvIcon: TextView = view.findViewById(R.id.tvIcon)
            val tvName: TextView = view.findViewById(R.id.tvName)
            val tvDetails: TextView = view.findViewById(R.id.tvDetails)
            val tvNotificationStatus: TextView = view.findViewById(R.id.tvNotificationStatus)
        }
    }
}
