package com.example.presencedetector.services

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.presencedetector.model.WiFiDevice
import com.example.presencedetector.utils.NotificationUtil
import com.example.presencedetector.utils.PreferencesUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Combined presence detection service using WiFi as primary method.
 */
class PresenceDetectionManager(private val context: Context) {
    companion object {
        private const val TAG = "PresenceDetectionManager"
        private const val DETECTION_TIMEOUT = 30000L // 30 seconds
        private const val ABSENCE_DELAY = 5 * 60 * 1000L // 5 minutes in milliseconds
    }

    private val wifiService = WiFiDetectionService(context)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val preferences = PreferencesUtil(context)

    private var presenceListener: PresenceListener? = null
    private var wifiPresenceDetected = false
    private var lastWifiDetection = 0L
    private var lastPresenceState = false
    
    // Tracks current presence of individual devices to detect leave events
    private val devicesInRangeMap = mutableMapOf<String, WiFiDevice>()
    private val notifiedArrivalsToday = mutableSetOf<String>()
    
    // Timer to track how long someone has been away (global)
    private var lastTimeSomeoneWasPresent = System.currentTimeMillis()

    private var currentWifiDevices: List<WiFiDevice> = emptyList()

    fun interface PresenceListener {
        fun onPresenceChanged(peoplePresent: Boolean, method: String, devices: List<WiFiDevice>, details: String)
    }

    init {
        setupListeners()
    }

    private fun setupListeners() {
        wifiService.setPresenceListener { detected, devices, details ->
            wifiPresenceDetected = detected
            currentWifiDevices = devices
            
            // Track detection history for all found devices (1x per day)
            devices.forEach { preferences.trackDetection(it.bssid) }
            
            // Handle arrival and departure of specific devices
            processDeviceEvents(devices)
            
            if (detected) lastWifiDetection = System.currentTimeMillis()
            evaluatePresence("WiFi", details)
        }
    }

    private fun processDeviceEvents(detectedDevices: List<WiFiDevice>) {
        val detectedBssids = detectedDevices.map { it.bssid }.toSet()
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())

        // 1. Check for ARRIVALS
        detectedDevices.forEach { device ->
            if (!devicesInRangeMap.containsKey(device.bssid)) {
                // Device just appeared
                devicesInRangeMap[device.bssid] = device
                
                if (preferences.shouldNotifyOnPresence() && preferences.shouldNotifyArrival(device.bssid)) {
                    val arrivalKey = "${device.bssid}_arr_$today"
                    if (!notifiedArrivalsToday.contains(arrivalKey)) {
                        notifiedArrivalsToday.add(arrivalKey)
                        sendArrivalNotification(device)
                    }
                }
            } else {
                // Update stored device data
                devicesInRangeMap[device.bssid] = device
            }
        }

        // 2. Check for DEPARTURES
        val leftBssids = devicesInRangeMap.keys.filter { !detectedBssids.contains(it) }
        leftBssids.forEach { bssid ->
            val device = devicesInRangeMap[bssid]
            devicesInRangeMap.remove(bssid)
            
            if (device != null && preferences.shouldNotifyOnPresence() && preferences.shouldNotifyDeparture(bssid)) {
                sendDepartureNotification(device)
            }
        }
    }

    private fun sendArrivalNotification(device: WiFiDevice) {
        val nickname = preferences.getNickname(device.bssid) ?: device.ssid
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val category = device.category.displayName
        
        val title = "🔔 ${device.category.iconRes} Detected: $nickname"
        val message = "Just arrived at $time. Signal strength is ${device.level}dBm. Recognized as $category."
        
        NotificationUtil.sendPresenceNotification(context, title, message, true)
    }

    private fun sendDepartureNotification(device: WiFiDevice) {
        val nickname = preferences.getNickname(device.bssid) ?: device.ssid
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        
        val title = "🚪 Device Left: $nickname"
        val message = "No longer detected as of $time. ${device.category.iconRes} signal has dropped."
        
        NotificationUtil.sendPresenceNotification(context, title, message, false)
    }

    fun startDetection() {
        wifiService.startScanning()
    }

    fun stopDetection() {
        wifiService.stopScanning()
        devicesInRangeMap.clear()
    }

    private fun evaluatePresence(method: String, details: String) {
        val now = System.currentTimeMillis()
        val isCurrentlyDetected = isPeoplePresentNow()

        if (isCurrentlyDetected) {
            lastTimeSomeoneWasPresent = now
        }

        val finalPresenceState = if (isCurrentlyDetected) {
            true
        } else {
            (now - lastTimeSomeoneWasPresent) < ABSENCE_DELAY
        }

        if (finalPresenceState != lastPresenceState) {
            lastPresenceState = finalPresenceState
            sendNotification(finalPresenceState, method, details)
        }

        presenceListener?.let { listener ->
            mainHandler.post {
                val devicesWithNicknames = currentWifiDevices.map { 
                    it.copy(nickname = preferences.getNickname(it.bssid))
                }
                listener.onPresenceChanged(finalPresenceState, method, devicesWithNicknames, details)
            }
        }
    }

    private fun isPeoplePresentNow(): Boolean {
        val now = System.currentTimeMillis()
        return (wifiPresenceDetected && (now - lastWifiDetection) < DETECTION_TIMEOUT)
    }

    private fun sendNotification(peoplePresent: Boolean, method: String, details: String) {
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val title = if (peoplePresent) "🏠 Presence Active" else "🏠 Area Vacant"
        
        val knownInRange = currentWifiDevices.filter { preferences.getNickname(it.bssid) != null }
        
        val message = if (peoplePresent) {
            if (knownInRange.isNotEmpty()) {
                val names = knownInRange.joinToString(", ") { preferences.getNickname(it.bssid) ?: "" }
                "At $time: $names detected."
            } else {
                "Presence detected at $time. ($details)"
            }
        } else {
            "All known devices left. Last activity recorded at $time."
        }

        NotificationUtil.sendPresenceNotification(context, title, message, peoplePresent)
    }

    fun setPresenceListener(listener: PresenceListener) {
        this.presenceListener = listener
    }

    fun getDetectionStatus(): String {
        return buildString {
            append("WiFi: ${if (wifiService.isScanning()) "Active" else "Off"}")
            append(" | Present: ${if (lastPresenceState) "YES" else "NO"}")
        }
    }

    fun destroy() {
        stopDetection()
        wifiService.destroy()
    }
}
