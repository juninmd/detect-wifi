package com.example.presencedetector

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.presencedetector.services.TelegramService
import com.example.presencedetector.utils.PreferencesUtil
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import java.util.Calendar

class SettingsActivity : AppCompatActivity() {

    private lateinit var preferences: PreferencesUtil
    private lateinit var telegramService: TelegramService

    private lateinit var switchTelegram: SwitchMaterial
    private lateinit var etTelegramToken: TextInputEditText
    private lateinit var etTelegramChatId: TextInputEditText
    private lateinit var btnTestTelegram: Button

    private lateinit var switchSecurityAlert: SwitchMaterial
    private lateinit var switchSecuritySound: SwitchMaterial
    private lateinit var etSecurityStart: TextInputEditText
    private lateinit var etSecurityEnd: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        preferences = PreferencesUtil(this)
        telegramService = TelegramService(this)

        setupToolbar()
        initializeViews()
        loadSettings()
        setupListeners()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun initializeViews() {
        switchTelegram = findViewById(R.id.switchTelegram)
        etTelegramToken = findViewById(R.id.etTelegramToken)
        etTelegramChatId = findViewById(R.id.etTelegramChatId)
        btnTestTelegram = findViewById(R.id.btnTestTelegram)

        switchSecurityAlert = findViewById(R.id.switchSecurityAlert)
        switchSecuritySound = findViewById(R.id.switchSecuritySound)
        etSecurityStart = findViewById(R.id.etSecurityStart)
        etSecurityEnd = findViewById(R.id.etSecurityEnd)
    }

    private fun loadSettings() {
        // Telegram
        switchTelegram.isChecked = preferences.isTelegramEnabled()
        etTelegramToken.setText(preferences.getTelegramToken())
        etTelegramChatId.setText(preferences.getTelegramChatId())

        // Security
        switchSecurityAlert.isChecked = preferences.isSecurityAlertEnabled()
        switchSecuritySound.isChecked = preferences.isSecuritySoundEnabled()

        val (start, end) = preferences.getSecuritySchedule()
        etSecurityStart.setText(start)
        etSecurityEnd.setText(end)
    }

    private fun setupListeners() {
        // Auto-save on change for switches
        switchTelegram.setOnCheckedChangeListener { _, isChecked ->
            preferences.setTelegramEnabled(isChecked)
        }

        switchSecurityAlert.setOnCheckedChangeListener { _, isChecked ->
            preferences.setSecurityAlertEnabled(isChecked)
        }

        switchSecuritySound.setOnCheckedChangeListener { _, isChecked ->
            preferences.setSecuritySoundEnabled(isChecked)
        }

        // Test Telegram Button
        btnTestTelegram.setOnClickListener {
            saveTelegramSettings()
            if (preferences.getTelegramToken().isNullOrEmpty()) {
                Toast.makeText(this, "Please enter a bot token", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Sending test message...", Toast.LENGTH_SHORT).show()
                telegramService.sendMessage("🔔 Test message from Presence Detector App!")
            }
        }

        // Time Pickers
        etSecurityStart.setOnClickListener { showTimePicker(etSecurityStart) }
        etSecurityEnd.setOnClickListener { showTimePicker(etSecurityEnd) }
    }

    private fun showTimePicker(editText: TextInputEditText) {
        val cal = Calendar.getInstance()
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val currentMinute = cal.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(this, { _, hourOfDay, minute ->
            val time = String.format("%02d:%02d", hourOfDay, minute)
            editText.setText(time)
            saveSecuritySchedule()
        }, currentHour, currentMinute, true)

        timePickerDialog.show()
    }

    private fun saveTelegramSettings() {
        preferences.setTelegramToken(etTelegramToken.text.toString().trim())
        preferences.setTelegramChatId(etTelegramChatId.text.toString().trim())
    }

    private fun saveSecuritySchedule() {
        preferences.setSecuritySchedule(
            etSecurityStart.text.toString(),
            etSecurityEnd.text.toString()
        )
    }

    override fun onPause() {
        super.onPause()
        saveTelegramSettings()
        saveSecuritySchedule()
    }
}
