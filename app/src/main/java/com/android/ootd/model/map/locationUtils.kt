package com.android.ootd.model.map

/**
 * Utility helpers for converting between Firestore location map structures and the app's [Location]
 * data class.
 */

/**
 * Converts a Firestore-stored map (or null) to a [Location]. If the map is null or missing fields,
 * returns [emptyLocation]. This mirrors existing behavior in repositories that treat missing or
 * malformed location data as emptyLocation.
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
