package com.example.presencedetector.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.presencedetector.R
import com.example.presencedetector.utils.PreferencesUtil
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText

class HomeSecurityBottomSheet : BottomSheetDialogFragment() {

  private lateinit var preferences: PreferencesUtil
  private lateinit var switchIntruderAlert: MaterialSwitch
  private lateinit var switchSound: MaterialSwitch
  private lateinit var etTrustedWifi: TextInputEditText
  private lateinit var btnSave: MaterialButton

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View? {
    return inflater.inflate(R.layout.bottom_sheet_home_security, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    preferences = PreferencesUtil(requireContext())

    switchIntruderAlert = view.findViewById(R.id.switchIntruderAlert)
    switchSound = view.findViewById(R.id.switchSound)
    etTrustedWifi = view.findViewById(R.id.etTrustedWifi)
    btnSave = view.findViewById(R.id.btnSave)

    // Load Preferences
    switchIntruderAlert.isChecked = preferences.isSecurityAlertEnabled()
    switchSound.isChecked = preferences.isSecuritySoundEnabled()
    etTrustedWifi.setText(preferences.getTrustedWifiSsid())

    btnSave.setOnClickListener {
      savePreferences()
      dismiss()
    }
  }

  private fun savePreferences() {
    preferences.setSecurityAlertEnabled(switchIntruderAlert.isChecked)
    preferences.setSecuritySoundEnabled(switchSound.isChecked)

    val ssid = etTrustedWifi.text.toString().trim()
    preferences.setTrustedWifiSsid(ssid)
  }

  companion object {
    const val TAG = "HomeSecurityBottomSheet"
  }
}
