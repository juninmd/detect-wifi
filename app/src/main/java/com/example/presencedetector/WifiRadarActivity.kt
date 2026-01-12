package com.example.presencedetector

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
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
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
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

    // Summary card views
    private lateinit var tvTotalDevices: TextView
    private lateinit var tvKnownDevices: TextView
    private lateinit var tvPersonDevices: TextView
    private lateinit var tvApplianceDevices: TextView
    private lateinit var chipGroupCategories: ChipGroup

    // Sorting
    enum class SortOrder {
        DISTANCE, NAME, CATEGORY
    }
    private var currentSortOrder = SortOrder.DISTANCE

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

        // Initialize summary card views from included layout
        val summaryCard = findViewById<View>(R.id.card_summary)
        tvTotalDevices = summaryCard.findViewById(R.id.tvTotalDevices)
        tvKnownDevices = summaryCard.findViewById(R.id.tvKnownDevices)
        tvPersonDevices = summaryCard.findViewById(R.id.tvPersonDevices)
        tvApplianceDevices = summaryCard.findViewById(R.id.tvApplianceDevices)
        chipGroupCategories = summaryCard.findViewById(R.id.chipGroupCategories)


        adapter = WifiAdapter(
            onItemClick = { device -> showEditDialog(device) },
            onItemLongClick = { device -> showDeviceInfoDialog(device) }
        )
        recyclerView.adapter = adapter

        detectionManager = PresenceDetectionManager(this, false)
        detectionManager.setPresenceListener { _, _, devices, _ ->
            runOnUiThread {
                val processedDevices = devices.map { device ->
                    device.apply {
                        manualCategory = preferences.getManualCategory(bssid)
                        nickname = preferences.getNickname(bssid)
                    }
                }
                adapter.updateDevices(processedDevices, currentSortOrder)
                radarView.updateDevices(processedDevices)
                updateSummary(processedDevices)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_radar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                forceRefreshDevices()
                true
            }
            R.id.action_sort -> {
                showSortDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSortDialog() {
        val sortOptions = arrayOf("📍 Distance (Default)", "🔤 Device Name", "📂 Category")
        val checkedItem = currentSortOrder.ordinal

        MaterialAlertDialogBuilder(this)
            .setTitle("Sort Devices By")
            .setSingleChoiceItems(sortOptions, checkedItem) { dialog, which ->
                currentSortOrder = SortOrder.values()[which]
                adapter.notifyDataSetChanged()  // Force re-sort
                dialog.dismiss()
            }
            .show()
    }

    private fun updateSummary(devices: List<WiFiDevice>) {
        val total = devices.size
        val known = devices.count { preferences.getNickname(it.bssid) != null }
        val people = devices.count { (preferences.getManualCategory(it.bssid) ?: it.category) == DeviceCategory.SMARTPHONE }
        val appliances = devices.count { (preferences.getManualCategory(it.bssid) ?: it.category) == DeviceCategory.SMART_LIGHT }

        tvTotalDevices.text = total.toString()
        tvKnownDevices.text = known.toString()
        tvPersonDevices.text = people.toString()
        tvApplianceDevices.text = appliances.toString()

        // Update category chips
        chipGroupCategories.removeAllViews()
        val categoryStats = mutableMapOf<DeviceCategory, Int>()
        devices.forEach { device ->
            val category = preferences.getManualCategory(device.bssid) ?: device.category
            categoryStats[category] = (categoryStats[category] ?: 0) + 1
        }

        categoryStats.forEach { (category, count) ->
            val chip = Chip(this).apply {
                text = "${category.iconRes} ${category.displayName} ($count)"
                isEnabled = false
                setTextColor(getColor(R.color.dark_text))
                chipIcon = null
            }
            chipGroupCategories.addView(chip)
        }
    }

    private fun forceRefreshDevices() {
        // Force WiFi scan to update device list
        detectionManager.startDetection()
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

        fun updateDevices(newDevices: List<WiFiDevice>, sortOrder: SortOrder = SortOrder.DISTANCE) {
            devices = when (sortOrder) {
                SortOrder.DISTANCE -> newDevices.sortedByDescending { it.level } // Strongest signal first
                SortOrder.NAME -> newDevices.sortedBy {
                    preferences.getNickname(it.bssid) ?: it.ssid
                }
                SortOrder.CATEGORY -> newDevices.sortedBy { device ->
                    val category = preferences.getManualCategory(device.bssid) ?: device.category
                    category.displayName
                }
            }
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

            if (nickname != null) {
                holder.tvName.setTextColor(getColor(R.color.primary_color))
                holder.chipNickname.visibility = View.VISIBLE
            } else {
                holder.tvName.setTextColor(getColor(R.color.dark_text))
                holder.chipNickname.visibility = View.GONE
            }

            val details = "${device.category.displayName} • ${device.level} dBm"
            holder.tvDetails.text = details

            if (hasNotify) {
                holder.tvNotificationStatus.visibility = View.VISIBLE
            } else {
                holder.tvNotificationStatus.visibility = View.GONE
            }

            holder.itemView.setOnClickListener { onItemClick(device) }
            holder.itemView.setOnLongClickListener {
                onItemLongClick(device)
                true
            }
        }

        override fun getItemCount(): Int = devices.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvIcon: TextView = view.findViewById(R.id.tvIcon)
            val tvName: TextView = view.findViewById(R.id.tvName)
            val tvDetails: TextView = view.findViewById(R.id.tvDetails)
            val tvNotificationStatus: TextView = view.findViewById(R.id.tvNotificationStatus)
            val chipNickname: Chip = view.findViewById(R.id.chipNickname)
        }
    }
}
