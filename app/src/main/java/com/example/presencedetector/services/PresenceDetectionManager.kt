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
        private const val TAG = "PresenceDetectionManager"
        private const val DETECTION_TIMEOUT = 30000L // 30 seconds
        private const val ABSENCE_THRESHOLD = 5 * 60 * 1000L // 5 minutes
    }

    private val wifiService = WiFiDetectionService(context)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val preferences = PreferencesUtil(context)
    private val telegramService = TelegramService(context)

    private var presenceListener: PresenceListener? = null
    private var wifiPresenceDetected = false
    private var lastWifiDetection = 0L
    private var lastPresenceState = false
    
    // Track timestamps for individual devices
    private val lastSeenMap = mutableMapOf<String, Long>()
    private val arrivalNotifiedMap = mutableMapOf<String, Boolean>()
    private val departureNotifiedMap = mutableMapOf<String, Boolean>()
    
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
            
            // Track history (1x per day)
            devices.forEach { preferences.trackDetection(it.bssid) }
            
            // Process smart notifications for individual devices
            processSmartDeviceEvents(devices)
            
            if (detected) lastWifiDetection = System.currentTimeMillis()
            evaluateGlobalPresence("WiFi", details)
        }
    }

    private fun processSmartDeviceEvents(detectedDevices: List<WiFiDevice>) {
        if (!preferences.shouldNotifyOnPresence()) return

        val now = System.currentTimeMillis()
        val detectedBssids = detectedDevices.map { it.bssid }.toSet()

        // 1. Handle Arrivals and Updates
        detectedDevices.forEach { device ->
            val bssid = device.bssid
            val lastSeen = lastSeenMap[bssid] ?: 0L

            // Security Check for NEW devices
            val isNewDevice = preferences.getDetectionHistoryCount(device.bssid) == 0
            if (isNewDevice && preferences.isSecurityAlertEnabled()) {
                handleSecurityThreat(device)
            }

            // Logic: Only notify arrival if the device was gone for > 5 minutes
            // (or if it's the first time we see it and we haven't seen it recently)
            if (lastSeen != 0L && (now - lastSeen) > ABSENCE_THRESHOLD) {
                if (preferences.shouldNotifyArrival(bssid)) {
                    sendArrivalNotification(device)
                }
            }

            lastSeenMap[bssid] = now
            departureNotifiedMap[bssid] = false
        }

        // 2. Handle Departures
        // Check all devices we've ever seen
        lastSeenMap.keys.forEach { bssid ->
            val lastSeen = lastSeenMap[bssid] ?: 0L
            val isCurrentlyMissing = !detectedBssids.contains(bssid)

            if (isCurrentlyMissing && (now - lastSeen) >= ABSENCE_THRESHOLD) {
                // Device has been missing for more than 5 minutes
                if (departureNotifiedMap[bssid] != true) {
                    if (preferences.shouldNotifyDeparture(bssid)) {
                        // Find device info from history or current scan (if we had it)
                        val device = currentWifiDevices.find { it.bssid == bssid }
                        sendDepartureNotification(bssid, device)
                    }
                    departureNotifiedMap[bssid] = true
                }
            }
        }
    }

    private fun handleSecurityThreat(device: WiFiDevice) {
        // Only alert if signal is reasonably strong to avoid false positives from street traffic
        if (device.level < -80) return

        val now = System.currentTimeMillis()
        val key = "${device.bssid}_security_alert"

        // Prevent spamming alerts (throttle to once per hour per device)
        // Note: In a real app we would store this timestamp persistently or in a map
        // For now, we assume this is critical enough to alert once per session or throttle lightly

        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val msg = "⚠️ SECURITY ALERT: New Unknown Network detected! SSID: ${device.ssid} (${device.level}dBm) at $time"

        // 1. Send High Priority Notification
        NotificationUtil.sendPresenceNotification(context, "⚠️ SECURITY THREAT", msg, true)

        // 2. Send Telegram
        telegramService.sendMessage(msg)

        // 3. Play Sound (Audit/Alarm) if conditions met
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

            // Stop after 5 seconds
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
        val category = device.category.displayName
        
        val title = "🔔 ${device.category.iconRes} Detected: $nickname"
        val message = "Just arrived at $time. Signal strength is ${device.level}dBm. Recognized as $category."
        
        NotificationUtil.sendPresenceNotification(context, title, message, true)
        telegramService.sendMessage("🔔 $nickname ($category) arrived at $time. Signal: ${device.level}dBm")
    }

    private fun sendDepartureNotification(bssid: String, device: WiFiDevice?) {
        val nickname = preferences.getNickname(bssid) ?: device?.ssid ?: "Known Device"
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val icon = device?.category?.iconRes ?: "📱"
        
        val title = "🚪 Device Left: $nickname"
        val message = "No longer detected as of $time. ${device?.category?.iconRes} signal has dropped."
        
        NotificationUtil.sendPresenceNotification(context, title, message, false)
        telegramService.sendMessage("🚪 $nickname left at $time.")
    }

    fun startDetection() {
        wifiService.startScanning()
    }

    fun stopDetection() {
        wifiService.stopScanning()
        lastSeenMap.clear()
        arrivalNotifiedMap.clear()
        departureNotifiedMap.clear()
    }

    private fun evaluateGlobalPresence(method: String, details: String) {
        val now = System.currentTimeMillis()
        val isCurrentlyDetected = (wifiPresenceDetected && (now - lastWifiDetection) < DETECTION_TIMEOUT)

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
            // Only send GLOBAL aggregate notification if user hasn't opted for specific device notifications
            // to avoid double alerts. If they have specific devices, we'll rely on those.
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
        // For simplicity, we send global if Master switch is on.
        // But let's filter: only send if it's a REAL presence change.
        return preferences.shouldNotifyOnPresence()
    }

    private fun sendGlobalNotification(peoplePresent: Boolean, method: String, details: String) {
        // We only notify global presence for devices that the user actually "cares" about (has nicknames)
        // This prevents the "Router" from spamming unless it has a nickname.
        val relevantDevices = currentWifiDevices.filter { preferences.getNickname(it.bssid) != null }
        
        if (peoplePresent && relevantDevices.isEmpty()) return // Don't notify generic unknown presence unless it's very strong

        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val title = if (peoplePresent) "🏠 Presence Detected" else "🏠 Area Clear"
        
        val message = if (peoplePresent) {
            val names = relevantDevices.joinToString(", ") { preferences.getNickname(it.bssid) ?: "" }
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
            append(" | Present: ${if (lastPresenceState) "YES" else "NO"}")
        }
    }

    fun destroy() {
        stopDetection()
        wifiService.destroy()
    }
}
