package com.example.presencedetector

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.presencedetector.services.TelegramService
import com.example.presencedetector.utils.PreferencesUtil
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import java.util.Calendar

class SettingsActivity : AppCompatActivity() {

    private lateinit var preferences: PreferencesUtil
    private lateinit var telegramService: TelegramService

    private lateinit var cardAppearance: com.google.android.material.card.MaterialCardView
    private lateinit var tvCurrentTheme: android.widget.TextView

    private lateinit var switchNotifyWifi: MaterialSwitch
    private lateinit var switchTelegram: MaterialSwitch
    private lateinit var etTelegramToken: TextInputEditText
    private lateinit var etTelegramChatId: TextInputEditText
    private lateinit var btnTestTelegram: com.google.android.material.button.MaterialButton

    private lateinit var switchSecurityAlert: MaterialSwitch
    private lateinit var switchSecuritySound: MaterialSwitch
    private lateinit var etSecurityStart: TextInputEditText
    private lateinit var etSecurityEnd: TextInputEditText

    // MQTT
    private lateinit var switchMqtt: MaterialSwitch
    private lateinit var etMqttHost: TextInputEditText
    private lateinit var etMqttPort: TextInputEditText
    private lateinit var etMqttTopic: TextInputEditText
    private lateinit var etMqttUser: TextInputEditText
    private lateinit var etMqttPass: TextInputEditText

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
        cardAppearance = findViewById(R.id.cardAppearance)
        tvCurrentTheme = findViewById(R.id.tvCurrentTheme)

        switchNotifyWifi = findViewById(R.id.switchNotifyWifi)
        switchTelegram = findViewById(R.id.switchTelegram)
        etTelegramToken = findViewById(R.id.etTelegramToken)
        etTelegramChatId = findViewById(R.id.etTelegramChatId)
        btnTestTelegram = findViewById(R.id.btnTestTelegram)

        switchSecurityAlert = findViewById(R.id.switchSecurityAlert)
        switchSecuritySound = findViewById(R.id.switchSecuritySound)
        etSecurityStart = findViewById(R.id.etSecurityStart)
        etSecurityEnd = findViewById(R.id.etSecurityEnd)

        // MQTT
        switchMqtt = findViewById(R.id.switchMqtt)
        etMqttHost = findViewById(R.id.etMqttHost)
        etMqttPort = findViewById(R.id.etMqttPort)
        etMqttTopic = findViewById(R.id.etMqttTopic)
        etMqttUser = findViewById(R.id.etMqttUser)
        etMqttPass = findViewById(R.id.etMqttPass)
    }

    private fun loadSettings() {
        updateThemeText()

        // Notifications
        switchNotifyWifi.isChecked = preferences.shouldNotifyWifiArrival()

        // Telegram
        switchTelegram.isChecked = preferences.isTelegramEnabled()
        etTelegramToken.setText(preferences.getTelegramToken())
        etTelegramChatId.setText(preferences.getTelegramChatId())

        // Security
        switchSecurityAlert.isChecked = preferences.isSecurityAlertEnabled()
        switchSecuritySound.isChecked = preferences.isSecuritySoundEnabled()

        val schedule = preferences.getSecuritySchedule()
        etSecurityStart.setText(schedule.first)
        etSecurityEnd.setText(schedule.second)

        // MQTT
        switchMqtt.isChecked = preferences.isMqttEnabled()
        etMqttHost.setText(preferences.getMqttHost())
        etMqttPort.setText(preferences.getMqttPort())
        etMqttTopic.setText(preferences.getMqttTopic())
        etMqttUser.setText(preferences.getMqttUser())
        etMqttPass.setText(preferences.getMqttPass())
    }

    private fun updateThemeText() {
        val theme = preferences.getAppTheme()
        tvCurrentTheme.text = "Current: " + when(theme) {
            1 -> "Light Mode"
            2 -> "Dark Mode"
            else -> "System Default"
        }
    }

    private fun setupListeners() {
        // Appearance
        cardAppearance.setOnClickListener {
            showThemeDialog()
        }

        // Auto-save on change for switches
        switchNotifyWifi.setOnCheckedChangeListener { _, isChecked ->
            preferences.setNotifyWifiArrival(isChecked)
        }

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

        // MQTT
        switchMqtt.setOnCheckedChangeListener { _, isChecked ->
            preferences.setMqttEnabled(isChecked)
        }

        // Time Pickers
        etSecurityStart.setOnClickListener { showTimePicker(etSecurityStart) }
        etSecurityEnd.setOnClickListener { showTimePicker(etSecurityEnd) }
    }

    private fun showThemeDialog() {
        val themes = arrayOf("System Default", "Light Mode", "Dark Mode")
        val checkedItem = preferences.getAppTheme()

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Choose Theme")
            .setSingleChoiceItems(themes, checkedItem) { dialog, which ->
                preferences.setAppTheme(which)
                updateThemeText()
                dialog.dismiss()

                // Recreate the task stack to ensure all activities update
                val intent = packageManager.getLaunchIntentForPackage(packageName)
                if (intent != null) {
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    finish()
                } else {
                    recreate()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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

    private fun saveMqttSettings() {
        val newHost = etMqttHost.text.toString().trim()
        val oldHost = preferences.getMqttHost()

        preferences.setMqttHost(newHost)
        preferences.setMqttPort(etMqttPort.text.toString().trim())
        preferences.setMqttTopic(etMqttTopic.text.toString().trim())
        preferences.setMqttUser(etMqttUser.text.toString().trim())
        preferences.setMqttPass(etMqttPass.text.toString().trim())

        // If host changed, suggest restart
        if (newHost != oldHost && newHost.isNotEmpty()) {
             Toast.makeText(this, "MQTT Settings saved. Please restart detection to reconnect.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onPause() {
        super.onPause()
        saveTelegramSettings()
        saveSecuritySchedule()
        saveMqttSettings()
    }
}
