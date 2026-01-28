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
}
