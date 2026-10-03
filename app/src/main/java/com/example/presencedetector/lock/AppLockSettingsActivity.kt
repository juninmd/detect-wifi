package com.example.presencedetector.lock

import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.presencedetector.R
import com.example.presencedetector.databinding.ActivityAppLockSettingsBinding
import com.example.presencedetector.databinding.DialogSetPinBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Chooses which apps are covered by the extra PIN and shows the grants the feature needs. */
class AppLockSettingsActivity : AppCompatActivity() {
  private lateinit var binding: ActivityAppLockSettingsBinding
  private lateinit var prefs: AppLockPreferences
  private lateinit var controller: AppLockController
  private val adapter = LockAppAdapter { app, on -> setProtected(app.packageName, on) }
  private var apps: List<LockApp> = emptyList()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityAppLockSettingsBinding.inflate(layoutInflater)
    setContentView(binding.root)
    prefs = AppLockPreferences(this)
    controller = AppLockModule.controller(this)

    binding.toolbar.setNavigationOnClickListener { finish() }
    binding.rvApps.layoutManager = LinearLayoutManager(this)
    binding.rvApps.adapter = adapter
    binding.btnPin.setOnClickListener { showPinDialog() }
    binding.cardService.setOnClickListener {
      startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }
    binding.cardIntruder.setOnClickListener { toggleDeviceAdmin() }
    binding.switchEnabled.setOnCheckedChangeListener { button, on ->
      if (on && prefs.pin == null) {
        button.isChecked = false
        Toast.makeText(this, R.string.applock_need_pin, Toast.LENGTH_SHORT).show()
        showPinDialog()
      } else {
        prefs.enabled = on
      }
    }
    lifecycleScope.launch {
      apps = withContext(Dispatchers.IO) { loadApps() }
      showApps()
    }
  }

  override fun onResume() {
    super.onResume()
    binding.switchEnabled.isChecked = prefs.enabled
    binding.btnPin.setText(
      if (prefs.pin == null) R.string.applock_set_pin else R.string.applock_change_pin
    )
    binding.tvService.setText(
      if (AccessStatus.isAccessibilityEnabled(this)) R.string.applock_service_on
      else R.string.applock_service_off
    )
    binding.tvIntruder.setText(
      if (AccessStatus.isDeviceAdminActive(this)) R.string.applock_intruder_on
      else R.string.applock_intruder_off
    )
  }

  private fun loadApps(): List<LockApp> {
    val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return packageManager
      .queryIntentActivities(launcher, PackageManager.ResolveInfoFlags.of(0))
      .map { it.activityInfo.packageName }
      .distinct()
      .filter { it != packageName }
      .map { pkg ->
        val info = packageManager.getApplicationInfo(pkg, 0)
        LockApp(
          pkg,
          packageManager.getApplicationLabel(info).toString(),
          packageManager.getApplicationIcon(info),
        )
      }
  }

  private fun showApps() {
    val selected = prefs.protectedApps
    adapter.submit(
      apps.sortedWith(compareBy({ it.packageName !in selected }, { it.label.lowercase() })),
      selected,
    )
  }

  private fun setProtected(pkg: String, on: Boolean) {
    prefs.protectedApps = if (on) prefs.protectedApps + pkg else prefs.protectedApps - pkg
  }

  private fun showPinDialog() {
    val view = DialogSetPinBinding.inflate(layoutInflater)
    MaterialAlertDialogBuilder(this)
      .setTitle(R.string.applock_set_pin)
      .setView(view.root)
      .setPositiveButton(android.R.string.ok) { _, _ ->
        val pin = view.etPin.text?.toString().orEmpty()
        val message =
          when {
            pin != view.etPinConfirm.text?.toString().orEmpty() -> R.string.applock_pin_mismatch
            !controller.setPin(pin) -> R.string.applock_pin_invalid
            else -> R.string.applock_pin_saved
          }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        onResume()
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }

  private fun toggleDeviceAdmin() {
    val component = AccessStatus.adminComponent(this)
    if (AccessStatus.isDeviceAdminActive(this)) {
      getSystemService(DevicePolicyManager::class.java).removeActiveAdmin(component)
      onResume()
      return
    }
    startActivity(
      Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
        .putExtra(
          DevicePolicyManager.EXTRA_ADD_EXPLANATION,
          getString(R.string.applock_intruder_explain),
        )
    )
  }
}
