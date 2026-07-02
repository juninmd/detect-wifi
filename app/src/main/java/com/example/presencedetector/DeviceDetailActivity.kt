package com.example.presencedetector

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.presencedetector.model.DeviceCategory
import com.example.presencedetector.utils.PreferencesUtil
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DeviceDetailActivity : AppCompatActivity() {

  companion object {
    const val EXTRA_BSSID = "extra_bssid"
    const val EXTRA_SSID = "extra_ssid"
    const val EXTRA_SIGNAL = "extra_signal"
    const val EXTRA_LAST_SEEN = "extra_last_seen"
  }

  private lateinit var preferences: PreferencesUtil
  private var bssid: String = ""
  private var ssid: String = ""
  private var signal: Int = 0
  private var lastSeen: Long = 0
  private var wifiStandard: Int = 0
  private var wifiChannelWidth: Int = 0

  // Views
  private lateinit var tvIconLarge: TextView
  private lateinit var tvDeviceNickname: TextView
  private lateinit var tvBssid: TextView
  private lateinit var tvStatusValue: TextView
  private lateinit var tvSignalValue: TextView
  private lateinit var tvLastSeenValue: TextView
  private lateinit var etEditNickname: TextInputEditText
  private lateinit var rgCategory: RadioGroup
  private lateinit var cbNotifyArrival: MaterialCheckBox
  private lateinit var cbNotifyDeparture: MaterialCheckBox
  private lateinit var switchCriticalAlert: SwitchMaterial
  private lateinit var switchTelegramAlert: SwitchMaterial
  private lateinit var btnSave: Button
  private lateinit var tvHistoryCount: TextView

  private lateinit var tvWifiStandard: TextView
  private lateinit var signalGraphView: com.example.presencedetector.ui.SignalGraphView

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_device_detail)

    preferences = PreferencesUtil(this)

    // Get Intent Data
    bssid = intent.getStringExtra(EXTRA_BSSID) ?: ""
    ssid = intent.getStringExtra(EXTRA_SSID) ?: "Unknown"
    signal = intent.getIntExtra(EXTRA_SIGNAL, -100)
    lastSeen = intent.getLongExtra(EXTRA_LAST_SEEN, System.currentTimeMillis())
    val standard = intent.getIntExtra("extra_standard", 0)
    val channelWidth = intent.getIntExtra("extra_channel_width", 0)

    // Store these to use in loadData (add fields first)
    this.wifiStandard = standard
    this.wifiChannelWidth = channelWidth

    if (bssid.isEmpty()) {
      finish()
      return
    }

    setupToolbar()
    initializeViews()
    setupCategories()
    loadData()
    setupListeners()
  }

  private fun setupToolbar() {
    val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    toolbar.setNavigationOnClickListener { finish() }
  }

  private fun initializeViews() {
    tvIconLarge = findViewById(R.id.tvIconLarge)
    tvDeviceNickname = findViewById(R.id.tvDeviceNickname)
    tvBssid = findViewById(R.id.tvBssid)
    tvStatusValue = findViewById(R.id.tvStatusValue)
    tvSignalValue = findViewById(R.id.tvSignalValue)
    tvLastSeenValue = findViewById(R.id.tvLastSeenValue)
    tvWifiStandard = findViewById(R.id.tvWifiStandard) // New
    signalGraphView = findViewById(R.id.signalGraphView) // New
    etEditNickname = findViewById(R.id.etEditNickname)
    rgCategory = findViewById(R.id.rgCategory)
    cbNotifyArrival = findViewById(R.id.cbNotifyArrival)
    cbNotifyDeparture = findViewById(R.id.cbNotifyDeparture)
    switchCriticalAlert = findViewById(R.id.switchCriticalAlert)
    switchTelegramAlert = findViewById(R.id.switchTelegramAlert)
    btnSave = findViewById(R.id.btnSave)
    tvHistoryCount = findViewById(R.id.tvHistoryCount)
  }

  private fun setupCategories() {
    DeviceCategory.values().forEach { category ->
      val rb =
        MaterialRadioButton(this).apply {
          id = View.generateViewId()
          text = "${category.iconRes} ${category.displayName}"
          tag = category
          setPadding(16, 16, 32, 16)
        }
      rgCategory.addView(rb)
    }
  }

  private fun loadData() {
    val nickname = preferences.getNickname(bssid)
    val category = preferences.getManualCategory(bssid) ?: DeviceCategory.UNKNOWN

    tvDeviceNickname.text = nickname ?: ssid
    etEditNickname.setText(nickname ?: ssid)
    tvBssid.text = bssid

    // Icon
    tvIconLarge.text = category.iconRes

    // Status
    tvSignalValue.text = "$signal dBm"
    val timeStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(lastSeen))
    tvLastSeenValue.text = timeStr

    // WiFi Standard
    val stdStr =
      when (wifiStandard) {
        4 -> "WiFi 4 (802.11n)"
        5 -> "WiFi 5 (802.11ac)"
        6 -> "WiFi 6 (802.11ax)"
        7 -> "WiFi 7 (802.11be)"
        else -> if (wifiStandard > 0) "Standard: $wifiStandard" else "Legacy/Unknown"
      }
    val widthStr = if (wifiChannelWidth > 0) " | ${wifiChannelWidth}MHz" else ""
    tvWifiStandard.text = "$stdStr$widthStr"

    // History
    val historyCount = preferences.getDetectionHistoryCount(bssid)
    tvHistoryCount.text = "Detected on $historyCount different days"

    // Initialize Graph
    signalGraphView.setDevice(bssid)

    // Notifications
    cbNotifyArrival.isChecked = preferences.shouldNotifyArrival(bssid)
    cbNotifyDeparture.isChecked = preferences.shouldNotifyDeparture(bssid)

    switchCriticalAlert.isChecked = preferences.isCriticalAlertEnabled(bssid)
    switchTelegramAlert.isChecked = preferences.isTelegramAlertEnabled(bssid)

    // Category Selection
    for (i in 0 until rgCategory.childCount) {
      val rb = rgCategory.getChildAt(i) as MaterialRadioButton
      if (rb.tag == category) {
        rb.isChecked = true
        break
      }
    }
  }

  private fun setupListeners() {
    btnSave.setOnClickListener {
      val newNickname = etEditNickname.text.toString().trim()
      preferences.saveNickname(bssid, newNickname)

      val checkedId = rgCategory.checkedRadioButtonId
      if (checkedId != -1) {
        val selectedRb = findViewById<MaterialRadioButton>(checkedId)
        val selectedCategory = selectedRb.tag as DeviceCategory
        preferences.saveManualCategory(bssid, selectedCategory)
      }

      preferences.setNotifyArrival(bssid, cbNotifyArrival.isChecked)
      preferences.setNotifyDeparture(bssid, cbNotifyDeparture.isChecked)
      preferences.setCriticalAlertEnabled(bssid, switchCriticalAlert.isChecked)
      preferences.setTelegramAlertEnabled(bssid, switchTelegramAlert.isChecked)

      Toast.makeText(this, "Settings Saved", Toast.LENGTH_SHORT).show()
      finish()
    }
  }
}
