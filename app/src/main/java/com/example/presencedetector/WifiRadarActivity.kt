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
            onItemClick = { device -> showEditDialog(device) },
            onItemLongClick = { device -> showDeviceInfoDialog(device) }
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

    private fun showDeviceInfoDialog(device: WiFiDevice) {
        val nickname = preferences.getNickname(device.bssid) ?: "No nickname"
        val historyCount = preferences.getDetectionHistoryCount(device.bssid)
        val lastSeenDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(device.lastSeen))
        
        val notifyArr = if (preferences.shouldNotifyArrival(device.bssid)) "🔔 Arrival On" else "🔕 Arrival Off"
        val notifyDep = if (preferences.shouldNotifyDeparture(device.bssid)) "🔔 Leave On" else "🔕 Leave Off"
        
        val status = if (device.isHidden) "⚠️ Hidden Network" else "✅ Visible"
        
        val info = """
            $status
            📌 SSID: ${if (device.isHidden) "[HIDDEN]" else device.ssid}
            🆔 BSSID: ${device.bssid}
            🏷️ Nickname: $nickname
            📂 Category: ${device.category.displayName}
            📶 Signal: ${device.level} dBm
            🕒 Last Seen: $lastSeenDate
            📊 Days Detected: $historyCount days
            $notifyArr | $notifyDep
        """.trimIndent()

        MaterialAlertDialogBuilder(this)
            .setTitle("Device Details")
            .setMessage(info)
            .setPositiveButton("Edit Profile") { _, _ -> showEditDialog(device) }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showEditDialog(device: WiFiDevice) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_nickname, null)
        val etNickname = dialogView.findViewById<EditText>(R.id.etNickname)
        val tvBssid = dialogView.findViewById<TextView>(R.id.tvBssid)
        val cbNotifyArrival = dialogView.findViewById<MaterialCheckBox>(R.id.cbNotifyArrival)
        val cbNotifyDeparture = dialogView.findViewById<MaterialCheckBox>(R.id.cbNotifyDeparture)
        val rgCategories = dialogView.findViewById<RadioGroup>(R.id.rgCategories)
        
        tvBssid.text = "BSSID: ${device.bssid}"
        etNickname.setText(preferences.getNickname(device.bssid) ?: device.ssid)
        cbNotifyArrival.isChecked = preferences.shouldNotifyArrival(device.bssid)
        cbNotifyDeparture.isChecked = preferences.shouldNotifyDeparture(device.bssid)

        // Populate Categories using MaterialRadioButtons for better visibility
        val categories = DeviceCategory.values()
        categories.forEach { category ->
            val rb = MaterialRadioButton(this).apply {
                id = View.generateViewId()
                text = "${category.iconRes} ${category.displayName}"
                setTextColor(getColor(R.color.dark_text))
                tag = category
                setPadding(0, 12, 0, 12)
            }
            rgCategories.addView(rb)
            if (category == device.category) {
                rgCategories.check(rb.id)
            }
        }

        MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newNickname = etNickname.text.toString()
                preferences.saveNickname(device.bssid, newNickname)
                
                val checkedId = rgCategories.checkedRadioButtonId
                val selectedRb = rgCategories.findViewById<MaterialRadioButton>(checkedId)
                val selectedCategory = selectedRb?.tag as? DeviceCategory ?: device.category
                
                preferences.saveManualCategory(device.bssid, selectedCategory)
                preferences.setNotifyArrival(device.bssid, cbNotifyArrival.isChecked)
                preferences.setNotifyDeparture(device.bssid, cbNotifyDeparture.isChecked)
                
                adapter.notifyDataSetChanged()
            }
            .setNegativeButton("Cancel", null)
            .show()
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
        private val onItemClick: (WiFiDevice) -> Unit,
        private val onItemLongClick: (WiFiDevice) -> Unit
    ) : RecyclerView.Adapter<WifiAdapter.ViewHolder>() {
        private var devices: List<WiFiDevice> = emptyList()

        fun updateDevices(newDevices: List<WiFiDevice>) {
            devices = newDevices.sortedByDescending { it.level }
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val device = devices[position]
            val nickname = preferences.getNickname(device.bssid)
            val hasNotify = preferences.shouldNotifyArrival(device.bssid) || preferences.shouldNotifyDeparture(device.bssid)
            
            val icon = device.category.iconRes
            val nameDisplay = if (device.isHidden) "🔒 [Hidden Network]" else (nickname ?: device.ssid)
            val notifyIcon = if (hasNotify) " 🔔" else ""
            
            holder.text1.text = "$icon $nameDisplay$notifyIcon"
            holder.text1.setTextColor(if (nickname != null) getColor(R.color.primary_color) else getColor(R.color.dark_text))
            
            holder.text2.text = "Type: ${device.category.displayName} | Signal: ${device.level} dBm"
            holder.text2.setTextColor(getColor(R.color.light_text))
            
            holder.itemView.setOnClickListener { onItemClick(device) }
            holder.itemView.setOnLongClickListener {
                onItemLongClick(device)
                true
            }
        }

        override fun getItemCount(): Int = devices.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val text1: TextView = view.findViewById(android.R.id.text1)
            val text2: TextView = view.findViewById(android.R.id.text2)
        }
    }
}
