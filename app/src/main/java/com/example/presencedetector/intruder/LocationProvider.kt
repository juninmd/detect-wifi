package com.example.presencedetector.intruder

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import java.util.concurrent.atomic.AtomicBoolean

/** One-shot location lookup using only framework APIs; falls back to the last known fix. */
class LocationProvider(private val context: Context) {
  companion object {
    private const val TIMEOUT_MS = 8_000L
  }

  private val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

  @SuppressLint("MissingPermission")
  fun current(timeoutMs: Long = TIMEOUT_MS, onResult: (GeoPoint?) -> Unit) {
    if (!hasPermission()) return onResult(null)
    val provider =
      listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).firstOrNull {
        manager.isProviderEnabled(it)
      } ?: return onResult(lastKnown())

    val done = AtomicBoolean(false)
    val signal = CancellationSignal()
    val handler = Handler(Looper.getMainLooper())
    val finish = { location: Location? ->
      if (done.compareAndSet(false, true)) {
        handler.removeCallbacksAndMessages(null)
        onResult((location ?: lastKnown()?.let { toLocation(it) })?.let(::toPoint))
      }
    }
    handler.postDelayed(
      {
        signal.cancel()
        finish(null)
      },
      timeoutMs,
    )
    manager.getCurrentLocation(provider, signal, context.mainExecutor) { finish(it) }
  }

  @SuppressLint("MissingPermission")
  fun lastKnown(): GeoPoint? =
    if (!hasPermission()) null
    else
      manager
        .getProviders(true)
        .mapNotNull { manager.getLastKnownLocation(it) }
        .maxByOrNull { it.time }
        ?.let(::toPoint)

  private fun hasPermission() =
    listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
      .any { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

  private fun toPoint(l: Location) = GeoPoint(l.latitude, l.longitude, l.accuracy)

  private fun toLocation(p: GeoPoint) =
    Location("last_known").apply {
      latitude = p.latitude
      longitude = p.longitude
      p.accuracyMeters?.let { accuracy = it }
    }
}
