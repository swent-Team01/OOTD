package com.android.ootd.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.android.ootd.LocationProvider.fusedLocationClient
import com.android.ootd.model.map.Location
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

/** Utility object for location-related operations. */
object LocationUtils {

  /**
   * Checks if location permission is already granted.
   *
   * @param context The application context
   * @return true if ACCESS_COARSE_LOCATION permission is granted, false otherwise
   */
  fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
  }

  /**
   * Retrieves the current GPS location using FusedLocationProviderClient.
   *
   * @param onSuccess Callback invoked with the retrieved Location on success
   * @param onFailure Callback invoked with an error message on failure
   */
  @RequiresPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
  fun getCurrentGPSLocation(onSuccess: (Location) -> Unit, onFailure: (String) -> Unit) {
    val cancellationTokenSource = CancellationTokenSource()

    fusedLocationClient
        .getCurrentLocation(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellationTokenSource.token)
        .addOnSuccessListener { androidLocation: android.location.Location? ->
          if (androidLocation != null) {
            // Convert Android Location to our app's Location model
            val appLocation =
                Location(
                    latitude = androidLocation.latitude,
                    longitude = androidLocation.longitude,
                    name =
                        "Current Location (${androidLocation.latitude.format()}, ${androidLocation.longitude.format()})")
            onSuccess(appLocation)
          } else {
            Log.w("LocationUtils", "getCurrentLocation returned null")
            onFailure(
                "Unable to get current location. Please enable location services or search manually.")
          }
          cancellationTokenSource.cancel()
        }
        .addOnFailureListener { exception ->
          Log.e("LocationUtils", "Failed to get current location", exception)
          onFailure("Failed to get current location: ${exception.message ?: "Unknown error"}")
          cancellationTokenSource.cancel()
        }
  }

  /** Formats a Double coordinate to 4 decimal places for display. */
  private fun Double.format(): String = "%.4f".format(this)
}
