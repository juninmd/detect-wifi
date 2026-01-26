package com.example.presencedetector.services

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.presencedetector.model.DeviceCategory
import com.example.presencedetector.model.DeviceSource
import com.example.presencedetector.model.WiFiDevice
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Bluetooth-based presence detection service.
 * Scans for Bluetooth LE devices.
 */
open class BluetoothDetectionService(private val context: Context) {
    companion object {
        private const val TAG = "BluetoothDetector"
        private const val SCAN_INTERVAL = 30000L // Optimized to 30 seconds for battery
        private const val SCAN_DURATION = 5000L  // 5 seconds scan duration
        private const val SIGNAL_THRESHOLD = -85 // dBm (ignore very weak signals)
        private const val DEVICE_TIMEOUT = 60000L // Remove device if not seen for 60 seconds
    }

    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private var scanJob: Job? = null
    private var cleanupJob: Job? = null
    private var presenceListener: PresenceListener? = null
    private var isScanning = false

    // Map to store unique devices by MAC address
    private val detectedDevices = ConcurrentHashMap<String, WiFiDevice>()

    fun interface PresenceListener {
        fun onPresenceDetected(peopleDetected: Boolean, devices: List<WiFiDevice>, details: String)
    }

    open fun setPresenceListener(listener: PresenceListener) {
        this.presenceListener = listener
    }

    open fun startScanning() {
        if (isScanning) {
            Log.w(TAG, "Bluetooth scanning already started")
            return
        }

        if (!hasBluetoothPermissions()) {
            Log.w(TAG, "Bluetooth permissions not granted")
            return
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.w(TAG, "Bluetooth not available or not enabled")
            return
        }

        isScanning = true
        Log.i(TAG, "Starting Bluetooth scanning")

        scanJob = scope.launch {
            while (isActive) {
                performScan()
                delay(SCAN_INTERVAL)
            }
        }

        cleanupJob = scope.launch {
            while (isActive) {
                cleanupOldDevices()
                delay(5000) // Check for old devices every 5 seconds
            }
        }
    }

    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun hasConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    open fun stopScanning() {
        if (isScanning && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                if (hasBluetoothPermissions()) {
                    bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping BLE scan", e)
            }
        }

        scanJob?.cancel()
        cleanupJob?.cancel()
        scanJob = null
        cleanupJob = null
        isScanning = false
        detectedDevices.clear()
        Log.i(TAG, "Bluetooth scanning stopped")
    }

    private fun performScan() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (hasBluetoothPermissions()) {
                    val settings = ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build()

                    bluetoothAdapter.bluetoothLeScanner?.startScan(null, settings, scanCallback)

                    // Stop scan after SCAN_DURATION
                    scope.launch {
                        delay(SCAN_DURATION)
                        if (isScanning && hasBluetoothPermissions()) {
                            try {
                                bluetoothAdapter.bluetoothLeScanner?.stopScan(scanCallback)
                                notifyPresence() // Notify listeners after scan cycle
                            } catch (e: Exception) {
                                Log.e(TAG, "Error stopping scan", e)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting Bluetooth scan", e)
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.let {
                val rssi = it.rssi
                if (rssi >= SIGNAL_THRESHOLD) {
                    processScanResult(it)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG, "BLE scan failed: $errorCode")
        }
    }

    private fun processScanResult(result: ScanResult) {
        val device = result.device
        val address = device.address
        val rssi = result.rssi

        var name = if (hasConnectPermission()) device.name else null
        if (name.isNullOrEmpty()) {
            name = result.scanRecord?.deviceName
        }
        if (name.isNullOrEmpty()) {
            name = "Unknown Bluetooth"
        }

        // Only track devices that look like personal devices (have a name or are specific types)
        // For now, we track everything that has a decent signal, but we try to filter nameless ones if rssi is weak
        if (name == "Unknown Bluetooth" && rssi < -80) return

        val wifiDevice = WiFiDevice(
            ssid = name ?: "Bluetooth Device",
            bssid = address,
            level = rssi,
            frequency = 2400, // Bluetooth uses 2.4GHz
            nickname = null, // Will be set by manager/prefs
            lastSeen = System.currentTimeMillis(),
            manualCategory = DeviceCategory.UNKNOWN, // Manager will handle classification
            source = DeviceSource.BLUETOOTH
        )

        detectedDevices[address] = wifiDevice
    }

    private fun cleanupOldDevices() {
        val now = System.currentTimeMillis()
        val iterator = detectedDevices.iterator()
        var changed = false

        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value.lastSeen > DEVICE_TIMEOUT) {
                iterator.remove()
                changed = true
            }
        }

        if (changed) {
            notifyPresence()
        }
    }

    private fun notifyPresence() {
        val devicesList = detectedDevices.values.toList()
        val hasDevices = devicesList.isNotEmpty()

        presenceListener?.let { listener ->
            mainHandler.post {
                listener.onPresenceDetected(hasDevices, devicesList, "Bluetooth scan complete")
            }
        }
    }

    fun isScanning(): Boolean = isScanning

    fun destroy() {
        stopScanning()
        scope.cancel()
    }
}
