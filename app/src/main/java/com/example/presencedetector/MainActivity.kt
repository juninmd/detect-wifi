package com.example.presencedetector

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.presencedetector.databinding.ActivityMainBinding
import com.example.presencedetector.model.WiFiDevice
import com.example.presencedetector.services.AntiTheftService
import com.example.presencedetector.services.DetectionBackgroundService
import com.example.presencedetector.services.PresenceDetectionManager
import com.example.presencedetector.utils.BiometricAuthenticator
import com.example.presencedetector.utils.LoggerUtil
import com.example.presencedetector.utils.NotificationUtil
import com.example.presencedetector.utils.PreferencesUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
  companion object {
    private const val PERMISSION_REQUEST_CODE = 100
    const val EXTRA_DISARM_REQUEST = "EXTRA_DISARM_REQUEST"
    const val EXTRA_NOTIFICATION_ID = "EXTRA_NOTIFICATION_ID"
  }

  private lateinit var binding: ActivityMainBinding
  private var detectionManager: PresenceDetectionManager? = null
  private var isDetecting = false
  private lateinit var preferences: PreferencesUtil

  private val batteryReceiver =
    object : BroadcastReceiver() {
      override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let { updateBatteryInfo(it) }
      }
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    preferences = PreferencesUtil(this)
    NotificationUtil.createNotificationChannels(this)

    initializeViews()
    checkAppLock()

    loadRecentLogs()
    if (!hasRequiredPermissions()) {
      requestPermissions()
    }
    setupDetectionManager()
    handleIntent(intent)
  }

  private fun checkAppLock() {
    if (preferences.isAppLockEnabled()) {
      binding.lockOverlay.visibility = View.VISIBLE
      // Trigger auth
      BiometricAuthenticator(this)
        .authenticate(
          onSuccess = {
            binding.lockOverlay.visibility = View.GONE
            Toast.makeText(this, R.string.msg_unlocked, Toast.LENGTH_SHORT).show()
            triggerHiddenCamera("App Unlock Success")
          },
          onFail = {
            Toast.makeText(this, R.string.msg_auth_failed, Toast.LENGTH_SHORT).show()
            triggerHiddenCamera("App Unlock Failed")
          }
        )
    } else {
      binding.lockOverlay.visibility = View.GONE
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleIntent(intent)
  }

  private fun handleIntent(intent: Intent?) {
    if (intent?.getBooleanExtra(EXTRA_DISARM_REQUEST, false) == true) {
      // Check if we should capture intruder selfie (silently)
      if (
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
          PackageManager.PERMISSION_GRANTED
      ) {
        captureIntruderSelfie()
      }

      // Trigger biometric check via existing toggle logic
      if (preferences.isAntiTheftArmed()) {
        toggleAntiTheft()
      }
    }
  }

  private fun captureIntruderSelfie() {
    if (
      ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
        PackageManager.PERMISSION_GRANTED
    )
      return

    val textureView = findViewById<android.view.TextureView>(R.id.cameraPreview)
    val cameraHelper = com.example.presencedetector.utils.CameraHelper(this)

    if (textureView.isAvailable && textureView.surfaceTexture != null) {
      cameraHelper.captureSelfie(textureView.surfaceTexture!!) {
        runOnUiThread { addLog(getString(R.string.log_snapshot_captured)) }
      }
    } else {
      textureView.surfaceTextureListener =
        object : android.view.TextureView.SurfaceTextureListener {
          override fun onSurfaceTextureAvailable(
            surface: android.graphics.SurfaceTexture,
            width: Int,
            height: Int
          ) {
            cameraHelper.captureSelfie(surface) {
              runOnUiThread { addLog(getString(R.string.log_snapshot_captured)) }
            }
          }

          override fun onSurfaceTextureSizeChanged(
            surface: android.graphics.SurfaceTexture,
            width: Int,
            height: Int
          ) {}

          override fun onSurfaceTextureDestroyed(
            surface: android.graphics.SurfaceTexture
          ): Boolean = true

          override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {}
        }
    }
  }

  override fun onResume() {
    super.onResume()
    updateAntiTheftUI()
    updateGlobalStatus()
    val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    registerReceiver(batteryReceiver, filter)
  }

  override fun onPause() {
    super.onPause()
    unregisterReceiver(batteryReceiver)
  }

  private fun updateBatteryInfo(intent: Intent) {
    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    val batteryPct = level * 100 / scale.toFloat()

    val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
    val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)

    binding.tvBatteryLevel.text = "${batteryPct.toInt()}%"
    binding.tvBatteryVoltage.text = "${String.format("%.1f", voltage / 1000f)}V"
    binding.tvBatteryTemp.text = "${String.format("%.1f", temp / 10f)}°C"
  }

  private fun initializeViews() {
    binding.switchSmartMode.isChecked = preferences.isSmartModeEnabled()
    binding.switchSmartMode.setOnCheckedChangeListener { _, isChecked ->
      preferences.setSmartModeEnabled(isChecked)
    }

    binding.switchSilentMode.isChecked = preferences.isSilentModeEnabled()
    binding.switchSilentMode.setOnCheckedChangeListener { _, isChecked ->
      preferences.setSilentModeEnabled(isChecked)
      val msg = if (isChecked) "Alarme Silencioso Ativado" else "Alarme Sonoro Ativado"
      Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    // Make Status Card clickable to arm/disarm
    binding.statusCard.setOnClickListener {
      if (preferences.isAntiTheftArmed()) {
        toggleAntiTheft()
      } else {
        toggleAntiTheft()
      }
    }

    binding.btnUnlockApp.setOnClickListener { checkAppLock() }

    binding.btnAntiTheft.setOnClickListener { toggleAntiTheft() }

    // Long click to test alarm
    binding.ivAntiTheftIcon.setOnLongClickListener {
      com.google.android.material.dialog
        .MaterialAlertDialogBuilder(this)
        .setTitle(R.string.dialog_test_alarm_title)
        .setMessage(R.string.dialog_test_alarm_msg)
        .setPositiveButton(R.string.btn_test) { _, _ -> testAlarm() }
        .setNegativeButton(R.string.btn_cancel, null)
        .show()
      true
    }

    updateAntiTheftUI()

    binding.cbNotifyPresence.isChecked = preferences.shouldNotifyOnPresence()
    binding.cbNotifyPresence.setOnCheckedChangeListener { _, isChecked ->
      preferences.setNotifyOnPresence(isChecked)
    }

    binding.switchHomeMonitor.setOnCheckedChangeListener { _, isChecked ->
      if (isChecked) startDetection() else stopDetection()
    }

    binding.btnQuickScan.setOnClickListener {
      Toast.makeText(this, getString(R.string.msg_scanning), Toast.LENGTH_SHORT).show()
      startDetection()
    }

    binding.btnOpenRadarFromGrid.setOnClickListener {
      startActivity(Intent(this, WifiRadarActivity::class.java))
    }

    binding.btnSettings.setOnClickListener {
      startActivity(Intent(this, SettingsActivity::class.java))
    }

    binding.btnOpenHistory.setOnClickListener {
      startActivity(Intent(this, HistoryActivity::class.java))
    }

    // Security Monitoring (RTSP Cameras)
    binding.btnSecuritySettings.setOnClickListener {
      startActivity(
        Intent(this, com.example.presencedetector.security.ui.CameraDashboardActivity::class.java)
      )
    }

    binding.btnPanic.setOnClickListener { triggerPanicMode() }
  }

  private fun triggerPanicMode() {
    Toast.makeText(this, R.string.msg_panic_activated, Toast.LENGTH_LONG).show()

    val serviceIntent =
      Intent(this, AntiTheftService::class.java).apply { action = AntiTheftService.ACTION_PANIC }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      startForegroundService(serviceIntent)
    } else {
      startService(serviceIntent)
    }

    addLog(getString(R.string.log_panic_pressed))
    updateGlobalStatus()
  }

  private fun toggleAntiTheft() {
    val currentlyArmed = preferences.isAntiTheftArmed()
    if (currentlyArmed) {
      if (preferences.isBiometricEnabled()) {
        BiometricAuthenticator(this).authenticate(onSuccess = { performDisarm() })
      } else {
        performDisarm()
      }
    } else {
      performArm()
    }
  }

  private fun performDisarm() {
    val serviceIntent =
      Intent(this, AntiTheftService::class.java).apply { action = AntiTheftService.ACTION_STOP }
    startService(serviceIntent)
    preferences.setAntiTheftArmed(false)
    addLog(getString(R.string.log_mobile_disarmed))
    preferences.logSystemEvent("Mobile Security Disarmed")
    updateAntiTheftUI()
  }

  private fun performArm() {
    val serviceIntent =
      Intent(this, AntiTheftService::class.java).apply { action = AntiTheftService.ACTION_START }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      startForegroundService(serviceIntent)
    } else {
      startService(serviceIntent)
    }
    preferences.setAntiTheftArmed(true)
    addLog(getString(R.string.log_mobile_armed))
    preferences.logSystemEvent("Mobile Security Armed")
    updateAntiTheftUI()
  }

  private fun updateAntiTheftUI() {
    val armed = preferences.isAntiTheftArmed()
    if (armed) {
      val modes = mutableListOf(getString(R.string.mode_motion))
      if (preferences.isPocketModeEnabled()) modes.add(getString(R.string.mode_pocket))
      if (preferences.isChargerModeEnabled()) modes.add(getString(R.string.mode_charger))

      val modeString = modes.joinToString(", ")
      binding.tvAntiTheftStatus.text = getString(R.string.status_active_format, modeString)
      binding.ivAntiTheftIcon.setImageResource(R.drawable.ic_status_active)
      binding.ivAntiTheftIcon.setColorFilter(ContextCompat.getColor(this, R.color.success_color))
    } else {
      binding.tvAntiTheftStatus.text = getString(R.string.text_tap_to_arm)
      binding.ivAntiTheftIcon.setImageResource(android.R.drawable.ic_lock_idle_lock)
      binding.ivAntiTheftIcon.setColorFilter(ContextCompat.getColor(this, R.color.primary_color))
    }
    updateGlobalStatus()
  }

  private fun updateGlobalStatus() {
    val isArmed = preferences.isAntiTheftArmed()

    // Determine Global State
    // Priority 1: Armed (Secure)
    // Priority 2: Scanning (Monitoring)
    // Default: Idle

    if (isArmed) {
      binding.statusCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.success_color))
      binding.ivGlobalStatus.setImageResource(R.drawable.ic_status_active)
      binding.ivGlobalStatus.setColorFilter(ContextCompat.getColor(this, R.color.white))
      binding.tvGlobalStatus.text = getString(R.string.status_system_secure)
      binding.tvGlobalStatus.setTextColor(ContextCompat.getColor(this, R.color.white))
      binding.tvGlobalDesc.text = getString(R.string.desc_system_secure)
      binding.tvGlobalDesc.setTextColor(ContextCompat.getColor(this, R.color.white))
    } else if (isDetecting) {
      binding.statusCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.primary_color))
      binding.ivGlobalStatus.setImageResource(android.R.drawable.ic_menu_rotate) // or radar icon
      binding.ivGlobalStatus.setColorFilter(ContextCompat.getColor(this, R.color.white))
      binding.tvGlobalStatus.text = getString(R.string.status_system_monitoring)
      binding.tvGlobalStatus.setTextColor(ContextCompat.getColor(this, R.color.white))
      binding.tvGlobalDesc.text = getString(R.string.desc_system_monitoring)
      binding.tvGlobalDesc.setTextColor(ContextCompat.getColor(this, R.color.white))
    } else {
      binding.statusCard.setCardBackgroundColor(
        ContextCompat.getColor(this, R.color.card_background_elevated)
      )
      binding.ivGlobalStatus.setImageResource(android.R.drawable.ic_lock_idle_lock)
      binding.ivGlobalStatus.setColorFilter(ContextCompat.getColor(this, R.color.light_text))
      binding.tvGlobalStatus.text = getString(R.string.status_system_idle)
      binding.tvGlobalStatus.setTextColor(ContextCompat.getColor(this, R.color.light_text))
      binding.tvGlobalDesc.text = getString(R.string.desc_system_idle)
      binding.tvGlobalDesc.setTextColor(ContextCompat.getColor(this, R.color.light_text))
    }
  }

  private fun loadRecentLogs() {
    val logs = preferences.getSystemLogs()
    if (logs.isNotEmpty()) {
      val recent = logs.take(5).joinToString("\n")
      binding.detectionLog.text = "--- Recent Events ---\n$recent\n---------------------\n"
    }
  }

  private fun setupDetectionManager() {
    detectionManager = PresenceDetectionManager(this, false)
    detectionManager?.setPresenceListener { _, method, devices, details ->
      runOnUiThread { updateDashboard(devices, method, details) }
    }
  }

  private fun updateDashboard(devices: List<WiFiDevice>, method: String, details: String) {
    val knownDevices = devices.filter { preferences.getNickname(it.bssid) != null }
    val unknownDevices = devices.filter { preferences.getNickname(it.bssid) == null }

    binding.tvCountKnown.text = knownDevices.size.toString()
    binding.tvCountUnknown.text = unknownDevices.size.toString()

    // Update Home Status (WiFi)
    if (isDetecting) {
      binding.ivHomeStatus.setImageResource(R.drawable.ic_status_active)
      binding.ivHomeStatus.setColorFilter(ContextCompat.getColor(this, R.color.success_color))
      binding.tvHomeStatus.text = getString(R.string.status_scanning)
      binding.tvHomeStatus.setTextColor(ContextCompat.getColor(this, R.color.success_color))
      binding.tvHomeStatusTitle.text = getString(R.string.title_home_monitor_active)
      if (!binding.switchHomeMonitor.isChecked) binding.switchHomeMonitor.isChecked = true
    } else {
      binding.ivHomeStatus.setImageResource(R.drawable.ic_status_inactive)
      binding.ivHomeStatus.setColorFilter(ContextCompat.getColor(this, R.color.primary_color))
      binding.tvHomeStatus.text = getString(R.string.status_home_idle)
      binding.tvHomeStatus.setTextColor(ContextCompat.getColor(this, R.color.light_text))
      binding.tvHomeStatusTitle.text = getString(R.string.title_home_monitor_idle)
      if (binding.switchHomeMonitor.isChecked) binding.switchHomeMonitor.isChecked = false
    }

    // Safe Zone Check
    val trustedSsid = preferences.getTrustedWifiSsid()
    val wifiManager =
      applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
    val info = wifiManager?.connectionInfo
    val currentSsid = info?.ssid?.replace("\"", "")

    if (!trustedSsid.isNullOrEmpty() && currentSsid == trustedSsid) {
      binding.ivSafeZoneBadge.visibility = View.VISIBLE
      binding.ivSafeZoneBadge.setCardBackgroundColor(
        ContextCompat.getColor(this, R.color.success_color)
      )
      binding.ivSafeZoneBadge.setOnClickListener(null) // Reset
    } else if (!currentSsid.isNullOrEmpty() && currentSsid != "<unknown ssid>") {
      // Suggest trusting this network
      binding.ivSafeZoneBadge.visibility = View.VISIBLE
      binding.ivSafeZoneBadge.setCardBackgroundColor(
        ContextCompat.getColor(this, R.color.warning_color)
      )

      binding.ivSafeZoneBadge.setOnClickListener {
        com.google.android.material.dialog
          .MaterialAlertDialogBuilder(this)
          .setTitle(R.string.dialog_safe_zone_title)
          .setMessage(getString(R.string.dialog_safe_zone_msg, currentSsid))
          .setPositiveButton(R.string.btn_yes) { _, _ ->
            preferences.setTrustedWifiSsid(currentSsid)
            addLog(getString(R.string.log_wifi_trusted, currentSsid))
            updateDashboard(devices, method, details) // Refresh
            Toast.makeText(this, R.string.msg_safe_zone_set, Toast.LENGTH_SHORT).show()
          }
          .setNegativeButton(R.string.btn_no, null)
          .show()
      }
    } else {
      binding.ivSafeZoneBadge.visibility = View.GONE
    }

    // Update Mobile Status (Anti-Theft) - Also called in updateAntiTheftUI, but good to refresh
    // here
    updateAntiTheftUI()
    updateGlobalStatus()

    if (devices.isNotEmpty()) {
      val summary =
        devices
          .sortedByDescending { it.level }
          .take(5)
          .joinToString(", ") {
            val name = preferences.getNickname(it.bssid) ?: it.ssid.take(8)
            "${it.category.iconRes} $name (${it.level}dBm)"
          }
      addLog(getString(R.string.log_scan_summary, devices.size, summary))
    }
  }

  private fun startDetection() {
    if (!hasRequiredPermissions()) {
      requestPermissions()
      return
    }

    isDetecting = true
    if (!binding.switchHomeMonitor.isChecked) binding.switchHomeMonitor.isChecked = true

    // UI Update
    binding.ivHomeStatus.setColorFilter(ContextCompat.getColor(this, R.color.success_color))
    binding.tvHomeStatus.text = getString(R.string.status_scanning)
    binding.tvHomeStatus.setTextColor(ContextCompat.getColor(this, R.color.success_color))

    val serviceIntent = Intent(this, DetectionBackgroundService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      startForegroundService(serviceIntent)
    } else {
      startService(serviceIntent)
    }

    detectionManager?.startDetection()
  }

  private fun stopDetection() {
    isDetecting = false
    if (binding.switchHomeMonitor.isChecked) binding.switchHomeMonitor.isChecked = false

    // UI Update
    binding.ivHomeStatus.setColorFilter(ContextCompat.getColor(this, R.color.primary_color))
    binding.ivHomeStatus.setImageResource(R.drawable.ic_status_inactive)
    binding.tvHomeStatus.text = getString(R.string.status_home_idle)
    binding.tvHomeStatus.setTextColor(ContextCompat.getColor(this, R.color.light_text))

    val serviceIntent = Intent(this, DetectionBackgroundService::class.java)
    stopService(serviceIntent)

    detectionManager?.stopDetection()
  }

  private fun addLog(message: String) {
    val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    val current = binding.detectionLog.text.toString()
    val newText = "[$timestamp] $message\n$current"
    binding.detectionLog.text = newText.take(1000)

    // Log to file as well
    LoggerUtil.logEvent(this, message)
  }

  private fun hasRequiredPermissions(): Boolean {
    val permissions = mutableListOf<String>()
    permissions.add(Manifest.permission.ACCESS_WIFI_STATE)
    permissions.add(Manifest.permission.CHANGE_WIFI_STATE)
    permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
    permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
    permissions.add(Manifest.permission.CAMERA) // Added for Intruder Snap
    permissions.add(Manifest.permission.RECORD_AUDIO) // Added for Audio Evidence
    permissions.add(Manifest.permission.READ_PHONE_STATE) // Added for SIM Protection

    // Bluetooth permissions (Android 12+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      permissions.add(Manifest.permission.BLUETOOTH_SCAN)
      permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      permissions.add(Manifest.permission.POST_NOTIFICATIONS)
      permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
    }

    // Background location for Android 10+ (Q)
    // Note: On Android 11+ (R), this must be requested separately or it may be ignored if requested
    // with foreground.
    // For simplicity in this optimization phase, we check it here, but strict flow might require
    // separation.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }

    return permissions.all {
      ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }
  }

  private fun requestPermissions() {
    val permissions = mutableListOf<String>()
    permissions.add(Manifest.permission.ACCESS_WIFI_STATE)
    permissions.add(Manifest.permission.CHANGE_WIFI_STATE)
    permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
    permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
    permissions.add(Manifest.permission.CAMERA) // Added for Intruder Snap
    permissions.add(Manifest.permission.RECORD_AUDIO) // Added for Audio Evidence
    permissions.add(Manifest.permission.READ_PHONE_STATE) // Added for SIM Protection

    // Bluetooth permissions (Android 12+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      permissions.add(Manifest.permission.BLUETOOTH_SCAN)
      permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      permissions.add(Manifest.permission.POST_NOTIFICATIONS)
      permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
    }

    // On Android 11+ (R), Background Location must be requested SEPARATELY after foreground is
    // granted.
    // If we request it here with others, the system ignores it.
    // So we do NOT add it here for R+. We add it only for Q.
    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
      permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }

    ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQUEST_CODE)

    checkBatteryOptimization()
  }

  private fun checkBatteryOptimization() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      val packageName = packageName
      val pm = getSystemService(android.os.PowerManager::class.java)
      if (!pm.isIgnoringBatteryOptimizations(packageName)) {
        addLog(getString(R.string.log_battery_opt_warning))

        com.google.android.material.dialog
          .MaterialAlertDialogBuilder(this)
          .setTitle(R.string.msg_battery_dialog_title)
          .setMessage(R.string.msg_battery_dialog_text)
          .setPositiveButton(R.string.btn_open_settings) { _, _ ->
            try {
              val intent =
                Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
              startActivity(intent)
            } catch (e: Exception) {
              Toast.makeText(this, getString(R.string.msg_error_open_settings), Toast.LENGTH_SHORT)
                .show()
            }
          }
          .setNegativeButton(R.string.btn_cancel, null)
          .show()
      }
    }

    // Check Usage Stats
    checkUsageStatsPermission()
  }

  private fun checkUsageStatsPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      val appOps = getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
      val mode =
        appOps.checkOpNoThrow(
          android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
          android.os.Process.myUid(),
          packageName
        )
      if (mode != android.app.AppOpsManager.MODE_ALLOWED) {
        com.google.android.material.dialog
          .MaterialAlertDialogBuilder(this)
          .setTitle(getString(R.string.dialog_usage_stats_title))
          .setMessage(getString(R.string.dialog_usage_stats_msg))
          .setPositiveButton(getString(R.string.btn_grant)) { _, _ ->
            try {
              startActivity(Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS))
            } catch (e: Exception) {
              e.printStackTrace()
            }
          }
          .show()
      }
    }
  }

  private fun testAlarm() {
    val serviceIntent =
      Intent(this, AntiTheftService::class.java).apply {
        action = AntiTheftService.ACTION_PANIC
        putExtra("com.example.presencedetector.EXTRA_REASON", getString(R.string.reason_siren_test))
      }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      startForegroundService(serviceIntent)
    } else {
      startService(serviceIntent)
    }

    // Auto-stop after 5 seconds
    android.os
      .Handler(android.os.Looper.getMainLooper())
      .postDelayed(
        {
          val stopIntent =
            Intent(this, AntiTheftService::class.java).apply {
              action = AntiTheftService.ACTION_STOP
            }
          startService(stopIntent)
        },
        5000
      )
  }

  private fun triggerHiddenCamera(reason: String) {
    try {
      val intent =
        Intent(this, com.example.presencedetector.security.ui.HiddenCameraActivity::class.java)
      startActivity(intent)
      addLog(getString(R.string.log_camera_triggered, reason))
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == PERMISSION_REQUEST_CODE) {
      // Check if all requested permissions were granted
      val allGranted =
        grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }

      if (allGranted) {
        // On Android 11+ (R), we must request Background Location separately
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          checkAndRequestBackgroundLocation()
        } else {
          startDetection()
        }
      } else {
        Toast.makeText(this, R.string.msg_permissions_required, Toast.LENGTH_LONG).show()
      }
    } else if (requestCode == 101) { // Background Location Request Code
      // Whether granted or not, try to start. Service will handle missing perm gracefully or run as
      // foreground-only.
      if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        addLog(getString(R.string.msg_bg_location_granted))
      } else {
        addLog(getString(R.string.msg_bg_location_denied))
      }
      startDetection()
    }
  }

  private fun checkAndRequestBackgroundLocation() {
    if (
      ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) !=
        PackageManager.PERMISSION_GRANTED
    ) {
      // We should show a dialog explaining why we need this before requesting
      // For now, request directly to keep flow moving
      ActivityCompat.requestPermissions(
        this,
        arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
        101
      )
    } else {
      startDetection()
    }
  }
}
