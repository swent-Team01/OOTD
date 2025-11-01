package com.android.ootd.model

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.ootd.model.map.NominatimLocationRepository
import com.android.ootd.utils.FakeHttpClient
import com.android.ootd.utils.FakeHttpClient.locationSuggestions
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Fast, focused tests for NominatimLocationRepository using FakeHttpClient. Tests only core
 * functionality to avoid slowing down CI. Disclaimer: Test where created with the help of AI.
 */
@RunWith(AndroidJUnit4::class)
class NominatimLocationRepositoryTest {

  @Test
  fun searchReturnsLocationsForValidQuery() = runTest {
    val repository = NominatimLocationRepository(FakeHttpClient.getClient())
    val results = repository.search(FakeHttpClient.FakeLocation.EPFL.queryName)

    val expected = FakeHttpClient.FakeLocation.EPFL.locationSuggestions
    assertEquals(expected.size, results.size)
    assertEquals(expected[0].name, results[0].name)
    assertEquals(expected[0].latitude, results[0].latitude, 0.0001)
    assertEquals(expected[0].longitude, results[0].longitude, 0.0001)
  }

  @Test
  fun searchReturnsEmptyListForNoResults() = runTest {
    val repository = NominatimLocationRepository(FakeHttpClient.getClient())
    val results = repository.search(FakeHttpClient.FakeLocation.NOWHERE.queryName)

    assertTrue(results.isEmpty())
  }

  @Test
  fun searchHandlesMultipleResults() = runTest {
    val repository = NominatimLocationRepository(FakeHttpClient.getClient())
    val results = repository.search(FakeHttpClient.FakeLocation.EVERYWHERE.queryName)

    val expected = FakeHttpClient.FakeLocation.EVERYWHERE.locationSuggestions
    assertEquals(expected.size, results.size)
    assertTrue(results.size > 1)
  }
}
