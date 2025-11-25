package com.android.ootd.utils

import android.Manifest
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.ootd.model.map.Location
import com.android.ootd.model.map.LocationRepository
import com.android.ootd.model.map.emptyLocation
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * Unit tests for LocationUtils.
 *
 * These tests cover:
 * - Permission checking
 * - Location data conversion (locationFromMap, mapFromLocation)
 * - Edge cases for map conversions
 * - Android location processing (processAndroidLocation)
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class LocationUtilsTest {

  private lateinit var context: Context
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  // ========== Permission Tests ==========

  @Test
  fun hasLocationPermission_returnsTrueWhenGranted() {
    val app = shadowOf(context as android.app.Application)
    app.grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)

    assertTrue(LocationUtils.hasLocationPermission(context))
  }

  @Test
  fun hasLocationPermission_returnsFalseWhenNotGranted() {
    val app = shadowOf(context as android.app.Application)
    app.denyPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)

    assertFalse(LocationUtils.hasLocationPermission(context))
  }

  // ========== processAndroidLocation Tests ==========

  @Test
  fun processAndroidLocation_withValidLocation_successfullyReverseGeocodes() = runTest {
    val mockRepository = mockk<LocationRepository>()
    val androidLocation = createMockAndroidLocation(46.5197, 6.5657)
    val expectedLocation = Location(46.5197, 6.5657, "EPFL, Lausanne, Switzerland")

    coEvery { mockRepository.reverseGeocode(46.5197, 6.5657) } returns expectedLocation

    var resultLocation: Location? = null
    var failureMessage: String? = null

    LocationUtils.processAndroidLocation(
        androidLocation = androidLocation,
        locationRepository = mockRepository,
        onSuccess = { resultLocation = it },
        onFailure = { failureMessage = it },
        dispatcher = testDispatcher)

    advanceUntilIdle()

    assertNotNull(resultLocation)
    assertEquals(46.5197, resultLocation?.latitude ?: 0.0, 0.0001)
    assertEquals(6.5657, resultLocation?.longitude ?: 0.0, 0.0001)
    assertEquals("EPFL, Lausanne, Switzerland", resultLocation?.name)
    assertEquals(null, failureMessage)
  }

  @Test
  fun processAndroidLocation_withReverseGeocodingFailure_usesCoordinates() = runTest {
    val mockRepository = mockk<LocationRepository>()
    val androidLocation = createMockAndroidLocation(46.5197, 6.5657)

    coEvery { mockRepository.reverseGeocode(any(), any()) } throws Exception("Network error")

    var resultLocation: Location? = null
    var failureMessage: String? = null

    LocationUtils.processAndroidLocation(
        androidLocation = androidLocation,
        locationRepository = mockRepository,
        onSuccess = { resultLocation = it },
        onFailure = { failureMessage = it },
        dispatcher = testDispatcher)

    advanceUntilIdle()

    assertNotNull(resultLocation)
    assertEquals(46.5197, resultLocation?.latitude ?: 0.0, 0.0001)
    assertEquals(6.5657, resultLocation?.longitude ?: 0.0, 0.0001)
    // Should use formatted coordinates when reverse geocoding fails
    assertEquals("Current Location (46.5197, 6.5657)", resultLocation?.name)
    assertEquals(null, failureMessage)
  }

  @Test
  fun processAndroidLocation_withNullLocation_callsOnFailure() = runTest {
    val mockRepository = mockk<LocationRepository>()

    var resultLocation: Location? = null
    var failureMessage: String? = null

    LocationUtils.processAndroidLocation(
        androidLocation = null,
        locationRepository = mockRepository,
        onSuccess = { resultLocation = it },
        onFailure = { failureMessage = it },
        dispatcher = testDispatcher)

    advanceUntilIdle()

    assertEquals(null, resultLocation)
    assertNotNull(failureMessage)
    assertTrue(failureMessage?.contains("Unable to get current location") ?: false)
  }

  @Test
  fun processAndroidLocation_withEdgeCaseCoordinates_handlesCorrectly() = runTest {
    val mockRepository = mockk<LocationRepository>()
    val testCases =
        listOf(
            Triple(0.0, 0.0, "Null Island"),
            Triple(90.0, 180.0, "North Pole"),
            Triple(-90.0, -180.0, "South Pole"),
            Triple(46.123456789, 6.987654321, "High Precision"))

    for ((lat, lon, name) in testCases) {
      val androidLocation = createMockAndroidLocation(lat, lon)
      val expectedLocation = Location(lat, lon, name)
      coEvery { mockRepository.reverseGeocode(lat, lon) } returns expectedLocation

      var resultLocation: Location? = null

      LocationUtils.processAndroidLocation(
          androidLocation = androidLocation,
          locationRepository = mockRepository,
          onSuccess = { resultLocation = it },
          onFailure = {},
          dispatcher = testDispatcher)

      advanceUntilIdle()

      assertNotNull(resultLocation)
      assertEquals(lat, resultLocation?.latitude ?: 0.0, 0.0001)
      assertEquals(lon, resultLocation?.longitude ?: 0.0, 0.0001)
      assertEquals(name, resultLocation?.name)
    }
  }

  // Helper function to create mock Android Location
  private fun createMockAndroidLocation(
      latitude: Double,
      longitude: Double
  ): android.location.Location {
    return android.location.Location("mock").apply {
      this.latitude = latitude
      this.longitude = longitude
    }
  }

  // ========== locationFromMap Tests ==========

  @Test
  fun locationFromMap_withValidMap_returnsCorrectLocation() {
    val map =
        mapOf("latitude" to 46.5197, "longitude" to 6.5657, "name" to "EPFL, Lausanne, Switzerland")

    val result = LocationUtils.locationFromMap(map)

    assertEquals(46.5197, result.latitude, 0.0001)
    assertEquals(6.5657, result.longitude, 0.0001)
    assertEquals("EPFL, Lausanne, Switzerland", result.name)
  }

  @Test
  fun locationFromMap_withNullMap_returnsEmptyLocation() {
    val result = LocationUtils.locationFromMap(null)

    assertEquals(emptyLocation, result)
  }

  @Test
  fun locationFromMap_withMissingFields_returnsDefaultValues() {
    val map = mapOf("latitude" to 46.5197)

    val result = LocationUtils.locationFromMap(map)

    assertEquals(46.5197, result.latitude, 0.0001)
    assertEquals(0.0, result.longitude, 0.0001)
    assertEquals("", result.name)
  }

  @Test
  fun locationFromMap_withEmptyMap_returnsDefaultLocation() {
    val result = LocationUtils.locationFromMap(emptyMap<String, Any>())

    assertEquals(0.0, result.latitude, 0.0001)
    assertEquals(0.0, result.longitude, 0.0001)
    assertEquals("", result.name)
  }

  @Test
  fun locationFromMap_withIntegerCoordinates_convertsCorrectly() {
    val map = mapOf("latitude" to 46, "longitude" to 6, "name" to "Test Location")

    val result = LocationUtils.locationFromMap(map)

    assertEquals(46.0, result.latitude, 0.0001)
    assertEquals(6.0, result.longitude, 0.0001)
    assertEquals("Test Location", result.name)
  }

  @Test
  fun locationFromMap_withInvalidTypes_usesDefaults() {
    val map = mapOf("latitude" to "invalid", "longitude" to true, "name" to 123)

    val result = LocationUtils.locationFromMap(map)

    assertEquals(0.0, result.latitude, 0.0001)
    assertEquals(0.0, result.longitude, 0.0001)
    assertEquals("", result.name)
  }

  // ========== mapFromLocation Tests ==========

  @Test
  fun mapFromLocation_createsCorrectMap() {
    val location = Location(46.5197, 6.5657, "EPFL, Lausanne, Switzerland")

    val result = LocationUtils.mapFromLocation(location)

    assertEquals(46.5197, result["latitude"])
    assertEquals(6.5657, result["longitude"])
    assertEquals("EPFL, Lausanne, Switzerland", result["name"])
  }

  @Test
  fun mapFromLocation_withEmptyName_preservesEmptyString() {
    val location = Location(0.0, 0.0, "")

    val result = LocationUtils.mapFromLocation(location)

    assertEquals(0.0, result["latitude"])
    assertEquals(0.0, result["longitude"])
    assertEquals("", result["name"])
  }

  @Test
  fun mapFromLocation_withNegativeCoordinates_preservesValues() {
    val location = Location(-45.0, -120.5, "Southern Hemisphere")

    val result = LocationUtils.mapFromLocation(location)

    assertEquals(-45.0, result["latitude"])
    assertEquals(-120.5, result["longitude"])
    assertEquals("Southern Hemisphere", result["name"])
  }

  // ========== Round-trip Conversion Tests ==========
  @Test
  fun roundTripConversion_withEdgeCaseCoordinates_preservesData() {
    val edgeCases =
        listOf(
            Location(0.0, 0.0, "Null Island"),
            Location(90.0, 180.0, "North Pole Area"),
            Location(-90.0, -180.0, "South Pole Area"),
            Location(46.123456789, 6.987654321, "High Precision Location"))

    for (original in edgeCases) {
      val map = LocationUtils.mapFromLocation(original)
      val converted = LocationUtils.locationFromMap(map)

      assertEquals(original.latitude, converted.latitude, 0.0001)
      assertEquals(original.longitude, converted.longitude, 0.0001)
      assertEquals(original.name, converted.name)
    }
  }
}
