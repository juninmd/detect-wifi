package com.example.presencedetector.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.presencedetector.R
import com.example.presencedetector.utils.PreferencesUtil
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider

class DeviceSecurityBottomSheet : BottomSheetDialogFragment() {

    private lateinit var preferences: PreferencesUtil
    private lateinit var switchPocketMode: MaterialSwitch
    private lateinit var switchChargerMode: MaterialSwitch
    private lateinit var sliderSensitivity: Slider
    private lateinit var tvSensitivityValue: TextView
    private lateinit var btnDone: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_device_security, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferences = PreferencesUtil(requireContext())

        switchPocketMode = view.findViewById(R.id.switchPocketMode)
        switchChargerMode = view.findViewById(R.id.switchChargerMode)
        sliderSensitivity = view.findViewById(R.id.sliderSensitivity)
        tvSensitivityValue = view.findViewById(R.id.tvSensitivityValue)
        btnDone = view.findViewById(R.id.btnDone)

        // Load Preferences
        switchPocketMode.isChecked = preferences.isPocketModeEnabled()
        switchChargerMode.isChecked = preferences.isChargerModeEnabled()

        val sensitivity = preferences.getAntiTheftSensitivity()
        sliderSensitivity.value = sensitivity
        updateSensitivityText(sensitivity)

        // Listeners
        switchPocketMode.setOnCheckedChangeListener { _, isChecked ->
            preferences.setPocketModeEnabled(isChecked)
        }

        switchChargerMode.setOnCheckedChangeListener { _, isChecked ->
            preferences.setChargerModeEnabled(isChecked)
        }

        sliderSensitivity.addOnChangeListener { _, value, _ ->
            preferences.setAntiTheftSensitivity(value)
            updateSensitivityText(value)
        }

        btnDone.setOnClickListener {
            dismiss()
        }
    }

    private fun updateSensitivityText(value: Float) {
        val label = when {
            value < 1.0 -> "Alta ($value)"
            value < 2.5 -> "Média ($value)"
            else -> "Baixa ($value)"
        }
        tvSensitivityValue.text = label
    }

    companion object {
        const val TAG = "DeviceSecurityBottomSheet"
    }
}
