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
class PresenceDetectionManager(private val context: Context, private val areNotificationsEnabled: Boolean = true) {
    companion object {
        private const val TAG = "PresenceDetection"
        private const val DETECTION_TIMEOUT = 30000L // 30 seconds
        private const val EXTERNAL_DETECTION_TIMEOUT = 30000L // 30 seconds for camera events
        private const val ABSENCE_THRESHOLD = 30 * 60 * 1000L // 30 minutes - for logging/tracking
        private const val LONG_ABSENCE_THRESHOLD = 30 * 60 * 1000L // 30 minutes - triggers immediate notification on return
        private const val NOTIFICATION_DEBOUNCE_WINDOW = 5 * 60 * 1000L // 5 minutes - prevent spam when device oscillates
        private const val DEPARTURE_CONFIRMATION_TIME = 5 * 60 * 1000L // 5 minutes - confirm device is really gone before notifying
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
    private var externalPresenceDetected = false
    private var lastWifiDetection = 0L
    private var lastBluetoothDetection = 0L
    private var lastExternalDetection = 0L
    private var lastExternalDetectionName = ""
    private var lastPresenceState = false

    // Track timestamps for individual devices
    private val lastSeenMap = mutableMapOf<String, Long>()
    private val departureNotifiedMap = mutableMapOf<String, Boolean>()
    private val lastNotificationTimeMap = mutableMapOf<String, Long>() // Debounce notifications
    private val hasNotifiedArrivalMap = mutableMapOf<String, Boolean>() // Track if already notified arrival
    private val lastDepartureTimeMap = mutableMapOf<String, Long>() // Track when device last left
    private val deviceTypes = mutableMapOf<String, com.example.presencedetector.model.DeviceSource>() // Track device source (WiFi/Bluetooth)

    private var lastTimeSomeoneWasPresent = System.currentTimeMillis()
    private var currentWifiDevices: List<WiFiDevice> = emptyList()
    private var currentBluetoothDevices: List<WiFiDevice> = emptyList()

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

            if (detected) lastWifiDetection = System.currentTimeMillis()
            updateAndProcessDevices("WiFi", details)
        }

        // Bluetooth Detection Listener
        bluetoothService.setPresenceListener { detected, devices, details ->
            bluetoothPresenceDetected = detected
            currentBluetoothDevices = devices

            if (detected) lastBluetoothDetection = System.currentTimeMillis()

            Log.i(TAG, "Bluetooth detection: $detected - $details")
            updateAndProcessDevices("Bluetooth", details)
        }
    }

    fun handleExternalPresence(source: String, name: String) {
        externalPresenceDetected = true
        lastExternalDetection = System.currentTimeMillis()
        lastExternalDetectionName = name
        Log.i(TAG, "External presence detection ($source): $name")
        updateAndProcessDevices("Camera", "Detected by $name")

        // Also log this event
        preferences.logEvent("camera_$name", "Detected on Camera")

        // Telegram notification for camera
        if (preferences.isTelegramEnabled()) {
             val time = SimpleDateFormat("HH:mm", Locale.US).format(Date())
             telegramService.sendMessage("📹 Camera Detection: Person detected on $name at $time")
        }
    }

    private fun updateAndProcessDevices(method: String, details: String) {
        val allDevices = currentWifiDevices + currentBluetoothDevices

        // Process events for all visible devices
        processSmartDeviceEvents(allDevices)

        // Track history for all devices
        allDevices.forEach { 
            preferences.trackDetection(it.bssid)
            deviceTypes[it.bssid] = it.source
        }

        evaluateGlobalPresence(method, details)
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

                if (areNotificationsEnabled) {
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

                    // Send Telegram notification (independent of system notification)
                    if (!wasNotifiedArrival && preferences.isTelegramAlertEnabled(bssid)) {
                        sendArrivalTelegramAlert(device)
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
                    lastDepartureTimeMap[bssid] = now  // Record departure time for dynamic debounce

                    if (areNotificationsEnabled) {
                        if (preferences.shouldNotifyOnPresence() && preferences.shouldNotifyDeparture(bssid)) {
                            if (canSendNotification(bssid)) {
                                // Try to find device info from last known state if possible, or create dummy
                                val device = (currentWifiDevices + currentBluetoothDevices).find { it.bssid == bssid }
                                sendDepartureNotification(bssid, device)
                                lastNotificationTimeMap[bssid] = now
                            }
                        }

                        // Send Telegram notification (independent of system notification)
                        if (preferences.isTelegramAlertEnabled(bssid)) {
                            val device = (currentWifiDevices + currentBluetoothDevices).find { it.bssid == bssid }
                            sendDepartureTelegramAlert(bssid, device)
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
     * Uses smart logic based on device state:
     *
     * Logic:
     * 1. If device is present/nearby and already notified of arrival this cycle -> DON'T notify again
     * 2. If device left/departed -> CAN notify about departure
     * 3. If device returned after 30+ min of absence -> notify IMMEDIATELY (no debounce)
     * 4. If device recently present -> respect 30s debounce window
     */
    private fun canSendNotification(bssid: String): Boolean {
        val now = System.currentTimeMillis()
        val lastSeen = lastSeenMap[bssid] ?: 0L
        val lastDeparture = lastDepartureTimeMap[bssid] ?: 0L
        val lastNotification = lastNotificationTimeMap[bssid] ?: 0L
        val wasNotifiedArrival = hasNotifiedArrivalMap[bssid] ?: false

        // If device is currently present and we already notified of arrival, don't notify again
        if (wasNotifiedArrival && (now - lastSeen) < NOTIFICATION_DEBOUNCE_WINDOW) {
            return false
        }

        // If device came back after 30+ minutes of absence, notify immediately
        if (lastDeparture > 0L && lastSeen > lastDeparture) {
            val absenceDuration = lastSeen - lastDeparture
            if (absenceDuration >= LONG_ABSENCE_THRESHOLD) {
                return true
            }
        }

        // First notification in cycle, always send
        if (lastNotification == 0L) {
            return true
        }

        // Otherwise, respect 30s debounce window for duplicate notifications
        return (now - lastNotification) >= NOTIFICATION_DEBOUNCE_WINDOW
    }

    private fun handleSecurityThreat(device: WiFiDevice) {
        if (device.level < -80) return
        val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
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
        // Filter: Only notify for Bluetooth devices (or manual override)
        // User requested: "só notificar dispositivos bluetooth ou presença na camera"
        if (device.source == com.example.presencedetector.model.DeviceSource.WIFI) {
            Log.d(TAG, "Skipping arrival notification for WiFi device: ${device.ssid}")
            return
        }

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
    }

    private fun sendDepartureNotification(bssid: String, device: WiFiDevice?) {
        // Filter: Only notify for Bluetooth devices
        val source = device?.source ?: deviceTypes[bssid]
        if (source == com.example.presencedetector.model.DeviceSource.WIFI) {
            Log.d(TAG, "Skipping departure notification for WiFi device: $bssid")
            return
        }

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
    }

    private fun sendArrivalTelegramAlert(device: WiFiDevice) {
        if (!preferences.isTelegramEnabled()) return

        val nickname = preferences.getNickname(device.bssid) ?: device.ssid
        val time = SimpleDateFormat("HH:mm", Locale.US).format(Date())
        val category = preferences.getManualCategory(device.bssid) ?: device.category
        val categoryDisplay = category.displayName

        val message = "🔔 $nickname ($categoryDisplay) arrived at $time. Signal: ${device.level}dBm"
        telegramService.sendMessage(message)
        Log.d(TAG, "Sent Telegram arrival alert for $nickname")
    }

    private fun sendDepartureTelegramAlert(bssid: String, device: WiFiDevice?) {
        if (!preferences.isTelegramEnabled()) return

        val nickname = preferences.getNickname(bssid) ?: device?.ssid ?: "Known Device"
        val time = SimpleDateFormat("HH:mm", Locale.US).format(Date())

        val message = "🚪 $nickname left at $time."
        telegramService.sendMessage(message)
        Log.d(TAG, "Sent Telegram departure alert for $nickname")
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
        lastDepartureTimeMap.clear()
    }

    private fun evaluateGlobalPresence(method: String, details: String) {
        val now = System.currentTimeMillis()

        // Check both WiFi and Bluetooth detection methods
        val isWifiDetected = wifiPresenceDetected && (now - lastWifiDetection) < DETECTION_TIMEOUT
        val isBluetoothDetected = bluetoothPresenceDetected && (now - lastBluetoothDetection) < DETECTION_TIMEOUT
        val isExternalDetected = externalPresenceDetected && (now - lastExternalDetection) < EXTERNAL_DETECTION_TIMEOUT

        val isCurrentlyDetected = isWifiDetected || isBluetoothDetected || isExternalDetected

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
            if (shouldSendGlobalNotification() && areNotificationsEnabled) {
                // Determine what triggered the change for the notification text
                val detectionDetails = if (isCurrentlyDetected) {
                    when {
                        isExternalDetected -> "Camera: $lastExternalDetectionName"
                        isBluetoothDetected -> "Bluetooth Device"
                        isWifiDetected -> "WiFi Device"
                        else -> details
                    }
                } else {
                    details
                }
                sendGlobalNotification(finalPresenceState, method, detectionDetails)
            }
        }

        presenceListener?.let { listener ->
            mainHandler.post {
                val allDevices = currentWifiDevices + currentBluetoothDevices
                val devicesWithNicknames = allDevices.map {
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
        val relevantDevices = (currentWifiDevices + currentBluetoothDevices).filter { preferences.getNickname(it.bssid) != null }
        if (peoplePresent && relevantDevices.isEmpty() && method != "Camera") return

        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val title = if (peoplePresent) "🏠 Presence Detected" else "🏠 Area Clear"

        val message = if (peoplePresent) {
            if (method == "Camera") {
                "At $time: Person detected by camera."
            } else {
                val names = relevantDevices.joinToString(", ") { device ->
                    val nickname = preferences.getNickname(device.bssid)
                    val category = preferences.getManualCategory(device.bssid) ?: device.category
                    "$nickname ${category.iconRes}"
                }
                "At $time: $names detected."
            }
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
            append(" | Camera: ${if (externalPresenceDetected) "Active" else "None"}")
            append(" | Present: ${if (lastPresenceState) "YES" else "NO"}")
        }
    }

    fun destroy() {
        stopDetection()
        wifiService.destroy()
        bluetoothService.destroy()
    }
}
