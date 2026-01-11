package com.example.presencedetector.services

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*

/**
 * Bluetooth-based presence detection service.
 * Scans for Bluetooth devices as a fallback to WiFi detection.
 */
class BluetoothDetectionService(private val context: Context) {
    companion object {
        private const val TAG = "BluetoothDetector"
        private const val SCAN_INTERVAL = 10000L // 10 seconds
        private const val SIGNAL_THRESHOLD = -70 // dBm
    }

    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private var scanJob: Job? = null
    private var presenceListener: PresenceListener? = null
    private var isScanning = false
    private val detectedDevices = mutableSetOf<String>()

    fun interface PresenceListener {
        fun onPresenceDetected(peopleDetected: Boolean, details: String)
    }

    fun setPresenceListener(listener: PresenceListener) {
        this.presenceListener = listener
    }

    fun startScanning() {
        if (isScanning) {
            Log.w(TAG, "Bluetooth scanning already started")
            return
        }

        if (!hasBluetoothPermissions()) {
            Log.w(TAG, "Bluetooth permissions not granted")
            notifyPresence(false, "Permissions missing")
            return
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.w(TAG, "Bluetooth not available or not enabled")
            notifyPresence(false, "Bluetooth disabled")
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
    }

    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun stopScanning() {
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
        scanJob = null
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
                    bluetoothAdapter.bluetoothLeScanner?.startScan(scanCallback)
                    
                    // Stop scan after 5 seconds to save battery
                    scope.launch {
                        delay(5000)
                        if (isScanning && hasBluetoothPermissions()) {
                            bluetoothAdapter.bluetoothLeScanner?.stopScan(scanCallback)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during Bluetooth scan", e)
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.let {
                val device = it.device
                val rssi = it.rssi
                if (rssi >= SIGNAL_THRESHOLD) {
                    detectedDevices.add(device.address)
                    notifyPresence(true, "Device detected: ${device.address}")
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG, "BLE scan failed: $errorCode")
        }
    }

    private fun notifyPresence(detected: Boolean, details: String) {
        presenceListener?.let {
            mainHandler.post {
                it.onPresenceDetected(detected, details)
            }
        }
    }

    fun isScanning(): Boolean = isScanning

    fun destroy() {
        stopScanning()
        scope.cancel()
    }
}
