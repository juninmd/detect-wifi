package com.example.presencedetector.services

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowApplication
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BluetoothDetectionServiceTest {

    private lateinit var context: Context
    private lateinit var service: BluetoothDetectionService

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        service = BluetoothDetectionService(context)
    }

    @Test
    fun `startScanning checks permissions`() {
        ShadowApplication.getInstance().denyPermissions(Manifest.permission.BLUETOOTH_SCAN)

        service.startScanning()
        assertFalse(service.isScanning())
    }

    @Test
    fun `startScanning starts scan when permitted`() {
        ShadowApplication.getInstance().grantPermissions(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_CONNECT
        )
        // Ensure adapter is enabled
        val adapter = BluetoothAdapter.getDefaultAdapter()
        // Use adapter.enable() which is public method on BluetoothAdapter
        adapter.enable()

        service.startScanning()
        assertTrue(service.isScanning())

        service.stopScanning()
        assertFalse(service.isScanning())
    }

    @Test
    fun `scan logic processes results`() {
        ShadowApplication.getInstance().grantPermissions(Manifest.permission.BLUETOOTH_SCAN)
        val adapter = BluetoothAdapter.getDefaultAdapter()
        adapter.enable()

        service.startScanning()

        assertTrue(service.isScanning())
    }

    @Test
    fun `ScanCallback handles results`() {
        ShadowApplication.getInstance().grantPermissions(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_CONNECT
        )
        val adapter = BluetoothAdapter.getDefaultAdapter()
        adapter.enable()

        // We need to mock the scanner because Robolectric's ShadowBluetoothLeScanner might be tricky to access directly via adapter in some versions,
        // but let's try assuming standard Shadow behavior.
        // Actually, we can just use reflection to access the private ScanCallback inside the service if needed,
        // or rely on ShadowBluetoothLeScanner to trigger it if we can find it.

        // Let's try reflection to access 'scanCallback' field in BluetoothDetectionService
        // This is the most reliable way to test the private callback logic.

        val field = BluetoothDetectionService::class.java.getDeclaredField("scanCallback")
        field.isAccessible = true
        val scanCallback = field.get(service) as android.bluetooth.le.ScanCallback

        var devicesFound: List<com.example.presencedetector.model.WiFiDevice> = emptyList()
        service.setPresenceListener { _, devices, _ ->
            devicesFound = devices
        }

        // Create a mock ScanResult
        val device = adapter.getRemoteDevice("AA:BB:CC:DD:EE:FF")
        org.robolectric.Shadows.shadowOf(device).setName("TestBLE")

        // ScanResult constructor is not public in older API, but ok in newer or via builder?
        // Robolectric usually allows it.
        // constructor(BluetoothDevice device, ScanRecord scanRecord, int rssi, long timestampNanos)
        // scanRecord is tricky.

        // Alternative: Mock the ScanResult
        val scanResult = org.mockito.Mockito.mock(android.bluetooth.le.ScanResult::class.java)
        org.mockito.Mockito.`when`(scanResult.device).thenReturn(device)
        org.mockito.Mockito.`when`(scanResult.rssi).thenReturn(-60)

        // Call onScanResult
        scanCallback.onScanResult(android.bluetooth.le.ScanSettings.CALLBACK_TYPE_ALL_MATCHES, scanResult)

        // Verify detectedDevices contains it.
        // BluetoothDetectionService stores it in detectedDevices map.
        // But it only notifies in 'notifyPresence()' which is called by 'cleanupOldDevices' or 'stopScanning'.
        // Or if we call 'notifyPresence' manually via reflection?

        // The service calls 'notifyPresence' after scan cycle in the coroutine.
        // We can force it by calling the private method notifyPresence via reflection.
        val notifyMethod = BluetoothDetectionService::class.java.getDeclaredMethod("notifyPresence")
        notifyMethod.isAccessible = true
        notifyMethod.invoke(service)

        org.robolectric.shadows.ShadowLooper.idleMainLooper(100)

        assertTrue("Should find 1 device", devicesFound.size == 1)
        org.junit.Assert.assertEquals("TestBLE", devicesFound[0].ssid)
    }
}
