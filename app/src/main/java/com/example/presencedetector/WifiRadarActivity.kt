package com.example.presencedetector

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.presencedetector.databinding.ActivityWifiRadarBinding
import com.example.presencedetector.databinding.DialogEditNicknameBinding
import com.example.presencedetector.databinding.ItemWifiDeviceBinding
import com.example.presencedetector.model.DeviceCategory
import com.example.presencedetector.model.DeviceSource
import com.example.presencedetector.model.WiFiDevice
import com.example.presencedetector.services.PresenceDetectionManager
import com.example.presencedetector.utils.PreferencesUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.radiobutton.MaterialRadioButton

class WifiRadarActivity : AppCompatActivity() {

  private lateinit var binding: ActivityWifiRadarBinding
  private lateinit var detectionManager: PresenceDetectionManager
  private lateinit var preferences: PreferencesUtil
  private lateinit var adapter: WifiAdapter

  // Sorting
  enum class SortOrder {
    DISTANCE,
    NAME,
    CATEGORY
  }

  private var currentSortOrder = SortOrder.DISTANCE

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityWifiRadarBinding.inflate(layoutInflater)
    setContentView(binding.root)

    setSupportActionBar(binding.toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    supportActionBar?.setHomeAsUpIndicator(android.R.drawable.ic_menu_revert)

    binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

    preferences = PreferencesUtil(this)
    binding.wifiRecyclerView.layoutManager = LinearLayoutManager(this)

    adapter =
      WifiAdapter(
        onItemClick = { device -> showDeviceInfoDialog(device) },
        onItemLongClick = { device -> showDeviceInfoDialog(device) }
      )
    binding.wifiRecyclerView.adapter = adapter

    detectionManager = PresenceDetectionManager(this, false)
    detectionManager.setPresenceListener { _, _, devices, _ ->
      runOnUiThread {
        val processedDevices =
          devices.map { device ->
            device.apply {
              manualCategory = preferences.getManualCategory(bssid)
              nickname = preferences.getNickname(bssid)
            }
          }
        adapter.updateDevices(processedDevices, currentSortOrder)
        binding.radarView.updateDevices(processedDevices)
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
    val sortOptions =
      arrayOf(
        getString(R.string.sort_option_distance),
        getString(R.string.sort_option_name),
        getString(R.string.sort_option_category)
      )
    val checkedItem = currentSortOrder.ordinal

    MaterialAlertDialogBuilder(this)
      .setTitle(getString(R.string.dialog_sort_title))
      .setSingleChoiceItems(sortOptions, checkedItem) { dialog, which ->
        currentSortOrder = SortOrder.values()[which]
        adapter.notifyDataSetChanged() // Force re-sort
        dialog.dismiss()
      }
      .show()
  }

  private fun forceRefreshDevices() {
    // Force WiFi scan to update device list
    detectionManager.startDetection()
  }

  private fun showDeviceInfoDialog(device: WiFiDevice) {
    val intent =
      android.content.Intent(this, DeviceDetailActivity::class.java).apply {
        putExtra(DeviceDetailActivity.EXTRA_BSSID, device.bssid)
        putExtra(DeviceDetailActivity.EXTRA_SSID, device.ssid)
        putExtra(DeviceDetailActivity.EXTRA_SIGNAL, device.level)
        putExtra(DeviceDetailActivity.EXTRA_LAST_SEEN, device.lastSeen)
        putExtra("extra_standard", device.standard)
        putExtra("extra_channel_width", device.channelWidth)
      }
    startActivity(intent)
  }

  private fun showEditDialog(device: WiFiDevice) {
    val dialogBinding = DialogEditNicknameBinding.inflate(layoutInflater)

    dialogBinding.tvBssid.text = getString(R.string.format_bssid, device.bssid)
    dialogBinding.etNickname.setText(preferences.getNickname(device.bssid) ?: device.ssid)
    dialogBinding.cbNotifyArrival.isChecked = preferences.shouldNotifyArrival(device.bssid)
    dialogBinding.cbNotifyDeparture.isChecked = preferences.shouldNotifyDeparture(device.bssid)

    val categories = DeviceCategory.values()
    categories.forEach { category ->
      val rb =
        MaterialRadioButton(this).apply {
          id = View.generateViewId()
          text = "${category.iconRes} ${category.displayName}"
          setTextColor(getColor(R.color.dark_text))
          tag = category
          setPadding(0, 12, 0, 12)
        }
      dialogBinding.rgCategories.addView(rb)
      if (category == device.category) {
        dialogBinding.rgCategories.check(rb.id)
      }
    }

    MaterialAlertDialogBuilder(this)
      .setView(dialogBinding.root)
      .setPositiveButton(getString(R.string.btn_save)) { _, _ ->
        val newNickname = dialogBinding.etNickname.text.toString()
        preferences.saveNickname(device.bssid, newNickname)

        val checkedId = dialogBinding.rgCategories.checkedRadioButtonId
        val selectedRb = dialogBinding.rgCategories.findViewById<MaterialRadioButton>(checkedId)
        val selectedCategory = selectedRb?.tag as? DeviceCategory ?: device.category

        preferences.saveManualCategory(device.bssid, selectedCategory)
        preferences.setNotifyArrival(device.bssid, dialogBinding.cbNotifyArrival.isChecked)
        preferences.setNotifyDeparture(device.bssid, dialogBinding.cbNotifyDeparture.isChecked)

        adapter.notifyDataSetChanged()
      }
      .setNegativeButton(getString(R.string.btn_cancel), null)
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
      devices =
        when (sortOrder) {
          SortOrder.DISTANCE -> newDevices.sortedByDescending { it.level } // Strongest signal first
          SortOrder.NAME -> newDevices.sortedBy { preferences.getNickname(it.bssid) ?: it.ssid }
          SortOrder.CATEGORY ->
            newDevices.sortedBy { device ->
              val category = preferences.getManualCategory(device.bssid) ?: device.category
              category.displayName
            }
        }
      notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
      val binding =
        ItemWifiDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
      return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
      val device = devices[position]
      val nickname = preferences.getNickname(device.bssid)
      val hasNotify =
        preferences.shouldNotifyArrival(device.bssid) ||
          preferences.shouldNotifyDeparture(device.bssid)

      holder.binding.tvIcon.text = device.category.iconRes

      if (device.isHidden) {
        holder.binding.tvName.text = getString(R.string.text_hidden_network)
      } else {
        holder.binding.tvName.text = nickname ?: device.ssid
      }

      if (nickname != null) {
        holder.binding.tvName.setTextColor(getColor(R.color.primary_color))
        holder.binding.chipNickname.visibility = View.VISIBLE
      } else {
        holder.binding.tvName.setTextColor(getColor(R.color.dark_text))
        holder.binding.chipNickname.visibility = View.GONE
      }

      val details = "${device.category.displayName} • ${device.level} dBm"
      holder.binding.tvDetails.text = details

      if (hasNotify) {
        holder.binding.tvNotificationStatus.visibility = View.VISIBLE
      } else {
        holder.binding.tvNotificationStatus.visibility = View.GONE
      }

      holder.itemView.setOnClickListener { onItemClick(device) }
      holder.itemView.setOnLongClickListener {
        onItemLongClick(device)
        true
      }

      // Show source badge (WiFi or Bluetooth)
      when (device.source) {
        DeviceSource.WIFI -> {
          holder.binding.chipSourceBadge.text = getString(R.string.badge_wifi)
          holder.binding.chipSourceBadge.setChipBackgroundColorResource(R.color.success_color)
        }
        DeviceSource.BLUETOOTH -> {
          holder.binding.chipSourceBadge.text = getString(R.string.badge_bluetooth)
          holder.binding.chipSourceBadge.setChipBackgroundColorResource(R.color.primary_vibrant)
        }
      }
    }

    override fun getItemCount(): Int = devices.size

    inner class ViewHolder(val binding: ItemWifiDeviceBinding) :
      RecyclerView.ViewHolder(binding.root)
  }
}
