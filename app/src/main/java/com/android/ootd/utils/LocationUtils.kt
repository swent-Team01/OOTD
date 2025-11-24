package com.android.ootd.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.android.ootd.LocationProvider.fusedLocationClient
import com.android.ootd.model.map.Location
import com.android.ootd.model.map.LocationRepository
import com.android.ootd.model.map.LocationRepositoryProvider
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
   * @param locationRepository The repository to use for reverse geocoding
   * @param onSuccess Callback invoked with the retrieved Location on success
   * @param onFailure Callback invoked with an error message on failure
   */
  @RequiresPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
  fun getCurrentGPSLocation(
      locationRepository: LocationRepository = LocationRepositoryProvider.repository,
      onSuccess: (Location) -> Unit,
      onFailure: (String) -> Unit
  ) {
    val cancellationTokenSource = CancellationTokenSource()

    fusedLocationClient
        .getCurrentLocation(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellationTokenSource.token)
        .addOnSuccessListener { androidLocation: android.location.Location? ->
          if (androidLocation != null) {
            // Try to reverse geocode the location to get a human-readable name
            CoroutineScope(Dispatchers.IO).launch {
              val locationName =
                  try {
                    val reverseGeocodedLocation =
                        locationRepository.reverseGeocode(
                            androidLocation.latitude, androidLocation.longitude)
                    reverseGeocodedLocation.name
                  } catch (e: Exception) {
                    Log.w("LocationUtils", "Reverse geocoding failed, using coordinates", e)
                    "Current Location (${androidLocation.latitude.format()}, ${androidLocation.longitude.format()})"
                  }

              // Convert Android Location to our app's Location model
              val appLocation =
                  Location(
                      latitude = androidLocation.latitude,
                      longitude = androidLocation.longitude,
                      name = locationName)
              onSuccess(appLocation)
            }
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
