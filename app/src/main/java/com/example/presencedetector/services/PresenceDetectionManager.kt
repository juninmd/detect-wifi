package com.example.presencedetector.services

import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
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
 * Enhanced presence detection service with smart debouncing.
 */
class PresenceDetectionManager(private val context: Context) {
    companion object {
        private const val TAG = "PresenceDetection"
        private const val DETECTION_TIMEOUT = 30000L // 30 seconds
        private const val ABSENCE_THRESHOLD = 5 * 60 * 1000L // 5 minutes
        private const val NOTIFICATION_DEBOUNCE_WINDOW = 30000L // 30 seconds - ignore duplicate notifs
        private const val MIN_SIGNAL_THRESHOLD = -90 // dBm - ignore weak signals
    }

    private val wifiService = WiFiDetectionService(context)
    private val bluetoothService = BluetoothDetectionService(context)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val preferences = PreferencesUtil(context)
    private val telegramService = TelegramService(context)

    private var presenceListener: PresenceListener? = null
    private var wifiPresenceDetected = false
    private var bluetoothPresenceDetected = false
    private var lastWifiDetection = 0L
    private var lastBluetoothDetection = 0L
    private var lastPresenceState = false

    // Track timestamps for individual devices
    private val lastSeenMap = mutableMapOf<String, Long>()
    private val departureNotifiedMap = mutableMapOf<String, Boolean>()
    private val lastNotificationTimeMap = mutableMapOf<String, Long>() // Debounce notifications
    private val hasNotifiedArrivalMap = mutableMapOf<String, Boolean>() // Track if already notified arrival

    private var lastTimeSomeoneWasPresent = System.currentTimeMillis()
    private var currentWifiDevices: List<WiFiDevice> = emptyList()

    fun interface PresenceListener {
        fun onPresenceChanged(peoplePresent: Boolean, method: String, devices: List<WiFiDevice>, details: String)
    }

    init {
        setupListeners()
    }

    private fun setupListeners() {
        // WiFi Detection Listener
        wifiService.setPresenceListener { detected, devices, details ->
            wifiPresenceDetected = detected
            currentWifiDevices = devices

            // Process smart notifications and EVENT LOGGING
            processSmartDeviceEvents(devices)

            // Then Track daily history count
            devices.forEach { preferences.trackDetection(it.bssid) }

            if (detected) lastWifiDetection = System.currentTimeMillis()
            evaluateGlobalPresence("WiFi", details)
        }

        // Bluetooth Detection Listener
        bluetoothService.setPresenceListener { detected, details ->
            bluetoothPresenceDetected = detected
            if (detected) lastBluetoothDetection = System.currentTimeMillis()

            Log.i(TAG, "Bluetooth detection: $detected - $details")
            evaluateGlobalPresence("Bluetooth", details)
        }
    }

    private fun processSmartDeviceEvents(detectedDevices: List<WiFiDevice>) {
        val now = System.currentTimeMillis()
        val detectedBssids = detectedDevices.map { it.bssid }.toSet()

        // Filter out weak signals to reduce noise
        val validDevices = detectedDevices.filter { it.level >= MIN_SIGNAL_THRESHOLD }

        // 1. Handle Arrivals and Updates
        validDevices.forEach { device ->
            val bssid = device.bssid
            val lastSeen = lastSeenMap[bssid] ?: 0L
            val wasNotifiedArrival = hasNotifiedArrivalMap[bssid] ?: false

            // LOG ARRIVAL if device was gone for > 5 minutes (or first time seen)
            if (lastSeen == 0L || (now - lastSeen) > ABSENCE_THRESHOLD) {
                preferences.logEvent(bssid, "Arrived")

                // Security Check for NEW devices (only if not seen before in history)
                val isNewDevice = preferences.getDetectionHistoryCount(bssid) == 0
                if (isNewDevice && preferences.isSecurityAlertEnabled()) {
                    handleSecurityThreat(device)
                }

                // Notify Arrival ONLY if this is the first time we're notifying about this arrival cycle
                // (Not repeatedly while device is still present)
                if (!wasNotifiedArrival && preferences.shouldNotifyOnPresence() && preferences.shouldNotifyArrival(bssid)) {
                    if (canSendNotification(bssid)) {
                        sendArrivalNotification(device)
                        lastNotificationTimeMap[bssid] = now
                        hasNotifiedArrivalMap[bssid] = true
                    }
                }
            }

            lastSeenMap[bssid] = now
            departureNotifiedMap[bssid] = false
        }

        // 2. Handle Departures
        lastSeenMap.keys.forEach { bssid ->
            val lastSeen = lastSeenMap[bssid] ?: 0L
            val isCurrentlyMissing = !detectedBssids.contains(bssid)

            if (isCurrentlyMissing && (now - lastSeen) >= ABSENCE_THRESHOLD) {
                // Device has been missing for more than 5 minutes
                if (departureNotifiedMap[bssid] != true) {
                    // LOG DEPARTURE
                    preferences.logEvent(bssid, "Left")

                    if (preferences.shouldNotifyOnPresence() && preferences.shouldNotifyDeparture(bssid)) {
                        if (canSendNotification(bssid)) {
                            val device = currentWifiDevices.find { it.bssid == bssid }
                            sendDepartureNotification(bssid, device)
                            lastNotificationTimeMap[bssid] = now
                        }
                    }
                    departureNotifiedMap[bssid] = true
                    hasNotifiedArrivalMap[bssid] = false  // Reset arrival notification flag for next arrival
                }
            }
        }
    }

    /**
     * Check if enough time has passed since last notification to avoid spam.
     */
    private fun canSendNotification(bssid: String): Boolean {
        val lastTime = lastNotificationTimeMap[bssid] ?: 0L
        return (System.currentTimeMillis() - lastTime) >= NOTIFICATION_DEBOUNCE_WINDOW
    }

    private fun handleSecurityThreat(device: WiFiDevice) {
        if (device.level < -80) return
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val msg = "⚠️ SECURITY ALERT: New Unknown Network detected! SSID: ${device.ssid} (${device.level}dBm) at $time"
        NotificationUtil.sendPresenceNotification(context, "⚠️ SECURITY THREAT", msg, true)
        telegramService.sendMessage(msg)
        if (preferences.isSecuritySoundEnabled() && preferences.isCurrentTimeInSecuritySchedule()) {
            playSecurityAlarm()
        }
    }

    private fun playSecurityAlarm() {
        try {
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(context, alarmSound)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                ringtone.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }
            ringtone.play()
            Handler(Looper.getMainLooper()).postDelayed({
                if (ringtone.isPlaying) ringtone.stop()
            }, 5000)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play alarm", e)
        }
    }

    private fun sendArrivalNotification(device: WiFiDevice) {
        val nickname = preferences.getNickname(device.bssid) ?: device.ssid
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        // Use manual category if available, otherwise auto-classified
        val category = preferences.getManualCategory(device.bssid) ?: device.category
        val categoryDisplay = category.displayName

        if (preferences.isCriticalAlertEnabled(device.bssid)) {
             playSecurityAlarm()
        }

        val title = "🔔 ${category.iconRes} Detected: $nickname"
        val message = "Just arrived at $time. Signal strength is ${device.level}dBm. Recognized as $categoryDisplay."

        NotificationUtil.sendPresenceNotification(context, title, message, true)

        if (preferences.isTelegramAlertEnabled(device.bssid) && preferences.isTelegramEnabled()) {
             telegramService.sendMessage("🔔 $nickname ($categoryDisplay) arrived at $time. Signal: ${device.level}dBm")
        }
    }

    private fun sendDepartureNotification(bssid: String, device: WiFiDevice?) {
        val nickname = preferences.getNickname(bssid) ?: device?.ssid ?: "Known Device"
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        // Use manual category if available
        val category = preferences.getManualCategory(bssid) ?: device?.category ?: com.example.presencedetector.model.DeviceCategory.UNKNOWN

        if (preferences.isCriticalAlertEnabled(bssid)) {
             playSecurityAlarm()
        }

        val title = "🚪 Device Left: $nickname"
        val message = "No longer detected as of $time. ${category.iconRes} signal has dropped."

        NotificationUtil.sendPresenceNotification(context, title, message, false)

        if (preferences.isTelegramAlertEnabled(bssid) && preferences.isTelegramEnabled()) {
             telegramService.sendMessage("🚪 $nickname left at $time.")
        }
    }

    fun startDetection() {
        Log.i(TAG, "Starting WiFi and Bluetooth detection...")
        wifiService.startScanning()
        bluetoothService.startScanning()
    }

    fun stopDetection() {
        Log.i(TAG, "Stopping WiFi and Bluetooth detection...")
        wifiService.stopScanning()
        bluetoothService.stopScanning()
        lastSeenMap.clear()
        departureNotifiedMap.clear()
        hasNotifiedArrivalMap.clear()
    }

    private fun evaluateGlobalPresence(method: String, details: String) {
        val now = System.currentTimeMillis()

        // Check both WiFi and Bluetooth detection methods
        val isWifiDetected = wifiPresenceDetected && (now - lastWifiDetection) < DETECTION_TIMEOUT
        val isBluetoothDetected = bluetoothPresenceDetected && (now - lastBluetoothDetection) < DETECTION_TIMEOUT
        val isCurrentlyDetected = isWifiDetected || isBluetoothDetected

        if (isCurrentlyDetected) {
            lastTimeSomeoneWasPresent = now
        }

        val finalPresenceState = if (isCurrentlyDetected) {
            true
        } else {
            (now - lastTimeSomeoneWasPresent) < ABSENCE_THRESHOLD
        }

        if (finalPresenceState != lastPresenceState) {
            lastPresenceState = finalPresenceState
            if (shouldSendGlobalNotification()) {
                sendGlobalNotification(finalPresenceState, method, details)
            }
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

    private fun shouldSendGlobalNotification(): Boolean {
        return preferences.shouldNotifyOnPresence()
    }

    private fun sendGlobalNotification(peoplePresent: Boolean, method: String, details: String) {
        val relevantDevices = currentWifiDevices.filter { preferences.getNickname(it.bssid) != null }
        if (peoplePresent && relevantDevices.isEmpty()) return
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val title = if (peoplePresent) "🏠 Presence Detected" else "🏠 Area Clear"
        val message = if (peoplePresent) {
            val names = relevantDevices.joinToString(", ") { device ->
                val nickname = preferences.getNickname(device.bssid)
                val category = preferences.getManualCategory(device.bssid) ?: device.category
                "$nickname ${category.iconRes}"
            }
            "At $time: $names detected."
        } else {
            "All tracked devices have left the area."
        }
        NotificationUtil.sendPresenceNotification(context, title, message, peoplePresent)
    }

    fun setPresenceListener(listener: PresenceListener) {
        this.presenceListener = listener
    }

    fun getDetectionStatus(): String {
        return buildString {
            append("WiFi: ${if (wifiService.isScanning()) "Active" else "Off"}")
            append(" | Bluetooth: ${if (bluetoothService.isScanning()) "Active" else "Off"}")
            append(" | Present: ${if (lastPresenceState) "YES" else "NO"}")
        }
    }

    fun destroy() {
        stopDetection()
        wifiService.destroy()
        bluetoothService.destroy()
    }
}
