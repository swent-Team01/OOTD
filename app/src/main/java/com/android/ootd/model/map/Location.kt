package com.android.ootd.model.map

/**
 * Simple data class representing a geographic location returned by a geocoding service
 *
 * @property latitude Latitude in decimal degrees.
 * @property longitude Longitude in decimal degrees.
 * @property name Human-readable display name for the location.
 *
 * Note: This file is adapted from the Bootcamp Week 3 solution.
 */
data class Location(
    val latitude: Double,
    val longitude: Double,
    val name: String,
)

/** An empty location constant */
val emptyLocation = Location(0.0, 0.0, "")

/** EPFL default location constant */
val epflLocation =
    Location(46.5191, 6.5668, "École Polytechnique Fédérale de Lausanne (EPFL), Switzerland")

/** Validation function to check if a Location is valid (i.e., has a non-blank name) */
val isValidLocation: (Location) -> Boolean = { location: Location ->
  location.name.isNotBlank() && !location.latitude.isNaN() && !location.longitude.isNaN()
}
