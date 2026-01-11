package com.example.presencedetector.ui

import android.app.AlertDialog
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.presencedetector.R
import com.example.presencedetector.model.WiFiDevice
import com.example.presencedetector.utils.PreferencesUtil
import java.util.Random

class RadarActivity : AppCompatActivity() {

    private lateinit var radarView: RadarView
    private lateinit var btnSimulate: Button
    private lateinit var btnClose: Button
    private lateinit var tvDeviceList: TextView
    private lateinit var deviceListInfo: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var isSimulating = false
    private val random = Random()
    private lateinit var preferences: PreferencesUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_radar)

        preferences = PreferencesUtil(this)

        radarView = findViewById(R.id.radarView)
        btnSimulate = findViewById(R.id.btnSimulate)
        btnClose = findViewById(R.id.btnClose)
        tvDeviceList = findViewById(R.id.tvDeviceList)
        deviceListInfo = findViewById(R.id.deviceListInfo)

        btnSimulate.setOnClickListener {
            toggleSimulation()
        }

        btnClose.setOnClickListener {
            finish()
        }

        // Allow clicking the text view to "mock" editing a nickname for the first device
        // Ideally we would use a ListView/RecyclerView
        tvDeviceList.setOnClickListener {
            // For demo: Edit nickname of the first device in list
            if (currentDevices.isNotEmpty()) {
                showNicknameDialog(currentDevices[0])
            }
        }
    }

    private var currentDevices: List<WiFiDevice> = emptyList()

    private fun toggleSimulation() {
        if (isSimulating) {
            isSimulating = false
            btnSimulate.text = "Simulate"
            handler.removeCallbacks(simulationRunnable)
        } else {
            isSimulating = true
            btnSimulate.text = "Stop Sim"
            handler.post(simulationRunnable)
        }
    }

    private val simulationRunnable = object : Runnable {
        override fun run() {
            if (!isSimulating) return

            // Generate fake devices
            val newDevices = mutableListOf<WiFiDevice>()
            val numDevices = random.nextInt(5) + 3 // 3 to 7 devices

            for (i in 0 until numDevices) {
                val bssid = "00:11:22:33:44:5$i"
                val level = -30 - random.nextInt(60) // -30 to -90
                val ssid = "Device-$i"
                
                // Check for saved nickname
                val savedNickname = preferences.getNickname(bssid)

                newDevices.add(WiFiDevice(
                    ssid = ssid,
                    bssid = bssid,
                    level = level,
                    frequency = 2400,
                    nickname = savedNickname
                ))
            }

            // Specific "Wife's Phone" simulation (Device 99)
            val wifeBssid = "AA:BB:CC:DD:EE:FF"
            val wifeLevel = -40 - random.nextInt(20) // Close
            val wifeNickname = preferences.getNickname(wifeBssid) ?: "Unknown"
            
            newDevices.add(WiFiDevice(
                ssid = "MyPhone",
                bssid = wifeBssid,
                level = wifeLevel,
                frequency = 5000,
                nickname = wifeNickname
            ))

            currentDevices = newDevices
            updateUI(newDevices)

            handler.postDelayed(this, 1000) // Update every second
        }
    }

    private fun updateUI(devices: List<WiFiDevice>) {
        radarView.setDevices(devices)
        deviceListInfo.text = "Detected Devices: ${devices.size}"
        
        val sb = StringBuilder()
        for (device in devices) {
            val name = device.nickname ?: device.ssid
            sb.append("• $name [${device.level} dBm] (${device.bssid})\n")
        }
        tvDeviceList.text = sb.toString()
    }

    private fun showNicknameDialog(device: WiFiDevice) {
        val input = EditText(this)
        input.setText(device.nickname ?: "")
        
        AlertDialog.Builder(this)
            .setTitle("Set Nickname")
            .setMessage("Enter nickname for ${device.ssid} (${device.bssid})")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newName = input.text.toString()
                if (newName.isNotEmpty()) {
                    preferences.saveNickname(device.bssid, newName)
                    Toast.makeText(this, "Nickname saved!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
