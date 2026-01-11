package com.example.presencedetector.services

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.*

/**
 * Bluetooth-based presence detection service.
 * Scans for Bluetooth devices as a fallback to WiFi detection.
 */
class BluetoothDetectionService(context: Context) {
    companion object {
        private const val TAG = "BluetoothDetector"
        private const val SCAN_INTERVAL = 10000L // 10 seconds
        private const val SIGNAL_THRESHOLD = -70 // dBm
        private const val SCAN_DURATION = 5000L // Scan for 5 seconds
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

        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            Log.w(TAG, "Bluetooth not available or not enabled")
            notifyPresence(false, "Bluetooth not available")
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

    fun stopScanning() {
        if (isScanning && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
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
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            notifyPresence(false, "Bluetooth disabled")
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                bluetoothAdapter!!.bluetoothLeScanner?.startScan(scanCallback)
                Log.d(TAG, "BLE scan started")
            }

            val bondedDeviceCount = bluetoothAdapter!!.bondedDevices.size
            Log.d(TAG, "Bonded devices found: $bondedDeviceCount")

        } catch (e: Exception) {
            Log.e(TAG, "Error during Bluetooth scan", e)
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)

            result?.device?.let { device ->
                val rssi = result.rssi

                if (rssi >= SIGNAL_THRESHOLD) {
                    detectedDevices.add(device.address)
                    Log.d(TAG, "Device detected: ${device.name} (${device.address}) RSSI: $rssi")
                    notifyPresence(true, "BLE device detected: ${device.name}")
                }
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)

            if (!results.isNullOrEmpty()) {
                Log.d(TAG, "Batch scan results: ${results.size}")
                notifyPresence(true, "Multiple BLE devices detected: ${results.size}")
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG, "BLE scan failed with error code: $errorCode")
            notifyPresence(false, "Bluetooth scan error: $errorCode")
        }
    }

    private fun notifyPresence(detected: Boolean, details: String) {
        presenceListener?.let {
            mainHandler.post {
                it.onPresenceDetected(detected, details)
            }
        }
        Log.i(TAG, "Bluetooth presence detection: $details")
    }

    fun isScanning(): Boolean = isScanning

    fun getDetectedDeviceCount(): Int = detectedDevices.size

    fun destroy() {
        stopScanning()
        scope.cancel()
    }
}
