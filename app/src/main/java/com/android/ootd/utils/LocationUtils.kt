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
import com.android.ootd.model.map.emptyLocation
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Utility object for location-related operations. */
object LocationUtils {

  /**
   * Checks if location permission is already granted.
   *
   * @param context The application context
   * @return true if ACCESS_FINE_LOCATION permission is granted, false otherwise
   */
  fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
  }

  /**
   * Processes an Android location and converts it to our app's Location model. This internal
   * function is exposed for testing purposes.
   *
   * @param androidLocation The Android location to process (can be null)
   * @param locationRepository The repository to use for reverse geocoding
   * @param onSuccess Callback invoked with the retrieved Location on success
   * @param onFailure Callback invoked with an error message on failure
   * @param dispatcher The coroutine dispatcher to use (default: Dispatchers.IO)
   */
  internal fun processAndroidLocation(
      androidLocation: android.location.Location?,
      locationRepository: LocationRepository,
      onSuccess: (Location) -> Unit,
      onFailure: (String) -> Unit,
      dispatcher: CoroutineContext = Dispatchers.IO
  ) {
    if (androidLocation != null) {
      // Try to reverse geocode the location to get a human-readable name
      CoroutineScope(dispatcher).launch {
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
  }

  /**
   * Retrieves the current GPS location using FusedLocationProviderClient.
   *
   * @param locationRepository The repository to use for reverse geocoding
   * @param onSuccess Callback invoked with the retrieved Location on success
   * @param onFailure Callback invoked with an error message on failure
   */
  @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
  fun getCurrentGPSLocation(
      locationRepository: LocationRepository = LocationRepositoryProvider.repository,
      onSuccess: (Location) -> Unit,
      onFailure: (String) -> Unit
  ) {
    val cancellationTokenSource = CancellationTokenSource()

    fusedLocationClient
        .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
        .addOnSuccessListener { androidLocation: android.location.Location? ->
          processAndroidLocation(androidLocation, locationRepository, onSuccess, onFailure)
          cancellationTokenSource.cancel()
        }
        .addOnFailureListener { exception ->
          Log.e("LocationUtils", "Failed to get current location", exception)
          onFailure("Failed to get current location: ${exception.message ?: "Unknown error"}")
          cancellationTokenSource.cancel()
        }
  }

  /**
   * Converts a Firestore-stored map (or null) to a [Location]. If the map is null or missing
   * fields, returns [emptyLocation]. This mirrors existing behavior in repositories that treat
   * missing or malformed location data as emptyLocation.
   */
  fun locationFromMap(map: Map<*, *>?): Location {
    if (map == null) return emptyLocation
    val lat = (map["latitude"] as? Number)?.toDouble() ?: 0.0
    val lon = (map["longitude"] as? Number)?.toDouble() ?: 0.0
    val name = map["name"] as? String ?: ""
    return Location(latitude = lat, longitude = lon, name = name)
  }

  /**
   * Converts a [Location] to a Firestore-friendly map with keys `latitude`, `longitude` and `name`.
   */
  fun mapFromLocation(location: Location): Map<String, Any> {
    return mapOf(
        "latitude" to location.latitude,
        "longitude" to location.longitude,
        "name" to location.name,
    )
  }

  /** Formats a Double coordinate to 4 decimal places for display. */
  private fun Double.format(): String = "%.4f".format(this)

  /** Converts a [Location] to a Google Maps [LatLng] for map display. */
  fun Location.toLatLng(): LatLng {
    return LatLng(latitude, longitude)
  }
}
