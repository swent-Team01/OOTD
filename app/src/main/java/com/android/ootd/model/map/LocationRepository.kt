package com.android.ootd.model.map

/**
 * Repository API for looking up locations by free-text query.
 *
 * Implementations should return a list of [Location] matching the query.
 *
 * Note: Pars of this file is adapted from the Bootcamp Week 3 solution.
 */
interface LocationRepository {
  /**
   * Search for locations that match [query].
   *
   * @param query Free-text search string.
   * @return List of matching [Location] objects; may be empty.
   */
  suspend fun search(query: String): List<Location>

  /**
   * Reverse geocode a latitude/longitude pair into a human-readable [Location].
   *
   * @param latitude Latitude of the location to reverse geocode.
   * @param longitude Longitude of the location to reverse geocode.
   * @return A [Location] representing the given coordinates.
   */
  suspend fun reverseGeocode(latitude: Double, longitude: Double): Location
}
