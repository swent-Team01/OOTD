package com.android.ootd.ui.map

import com.android.ootd.model.map.Location
import com.android.ootd.model.map.LocationRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for LocationSelectionViewModel.
 *
 * These tests focus on the location selection functionality, covering:
 * - Query updates and suggestion fetching
 * - Location selection
 * - Loading states and error handling
 * - Edge cases and state management
 *
 * This test has been written with the help of AI.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LocationSelectionViewModelTest {

  private lateinit var viewModel: LocationSelectionViewModel
  private lateinit var locationRepository: LocationRepository

  private val testDispatcher = StandardTestDispatcher()

  private val sampleLocations =
      listOf(
          Location(47.3769, 8.5417, "Zürich, Switzerland"),
          Location(46.2044, 6.1432, "Lausanne, Switzerland"),
          Location(46.9481, 7.4474, "Bern, Switzerland"))

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    locationRepository = mockk(relaxed = true)
    viewModel = LocationSelectionViewModel(locationRepository = locationRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  // ========== Initial State ==========

  @Test
  fun initialState_hasNoLocationData() {
    assertEquals("", viewModel.uiState.value.locationQuery)
    assertNull(viewModel.uiState.value.selectedLocation)
    assertTrue(viewModel.uiState.value.locationSuggestions.isEmpty())
    assertFalse(viewModel.uiState.value.isLoadingLocations)
  }

  // ========== Query and Suggestion Tests ==========

  @Test
  fun setLocationQuery_updatesQueryAndFetchesSuggestions() = runTest {
    coEvery { locationRepository.search("Zürich") } returns sampleLocations

    viewModel.setLocationQuery("Zürich")
    advanceUntilIdle()

    assertEquals("Zürich", viewModel.uiState.value.locationQuery)
    assertEquals(sampleLocations, viewModel.uiState.value.locationSuggestions)
    assertFalse(viewModel.uiState.value.isLoadingLocations)
  }

  @Test
  fun setLocationQuery_withEmptyQuery_clearsSuggestionsAndDoesNotLoad() = runTest {
    // First add some suggestions
    coEvery { locationRepository.search("Zürich") } returns sampleLocations
    viewModel.setLocationQuery("Zürich")
    advanceUntilIdle()

    // Then clear with empty query
    viewModel.setLocationQuery("")

    assertTrue(viewModel.uiState.value.locationSuggestions.isEmpty())
    assertEquals("", viewModel.uiState.value.locationQuery)
    assertFalse(viewModel.uiState.value.isLoadingLocations)
  }

  @Test
  fun setLocationQuery_clearsSelectedLocation() = runTest {
    viewModel.setLocation(sampleLocations[0])
    assertEquals(sampleLocations[0], viewModel.uiState.value.selectedLocation)

    viewModel.setLocationQuery("New query")

    assertNull(viewModel.uiState.value.selectedLocation)
  }

  @Test
  fun setLocationQuery_debounces_multipleQueries() = runTest {
    coEvery { locationRepository.search(any()) } returns sampleLocations

    // Type multiple characters quickly
    viewModel.setLocationQuery("Z")
    viewModel.setLocationQuery("Zü")
    viewModel.setLocationQuery("Zürich")

    advanceUntilIdle()

    // Only the last query should be processed
    assertEquals("Zürich", viewModel.uiState.value.locationQuery)
    assertEquals(sampleLocations, viewModel.uiState.value.locationSuggestions)
  }

  @Test
  fun setLocationQuery_populatesSuggestions_afterDebounceDelay() =
      runTest(testDispatcher) {
        coEvery { locationRepository.search("Zur") } returns sampleLocations.take(1)

        viewModel.setLocationQuery("Zur")
        advanceTimeBy(1_100) // debounce delay inside ViewModel

        assertEquals(sampleLocations.take(1), viewModel.uiState.value.locationSuggestions)
      }

  // ========== Loading State Tests ==========

  @Test
  fun setLocationQuery_setsLoadingStateDuringFetch() = runTest {
    coEvery { locationRepository.search(any()) } returns sampleLocations

    viewModel.setLocationQuery("Zürich")

    assertTrue(viewModel.uiState.value.isLoadingLocations)

    advanceUntilIdle()

    assertFalse(viewModel.uiState.value.isLoadingLocations)
  }

  // ========== Error Handling Tests ==========

  @Test
  fun setLocationQuery_onError_clearsSuggestionsAndStopsLoading() = runTest {
    coEvery { locationRepository.search(any()) } throws Exception("Network error")

    viewModel.setLocationQuery("Invalid")
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.locationSuggestions.isEmpty())
    assertFalse(viewModel.uiState.value.isLoadingLocations)
  }

  // ========== Location Selection Tests ==========

  @Test
  fun setLocation_updatesSelectedLocationAndQuery() {
    val location = sampleLocations[0]

    viewModel.setLocation(location)

    assertEquals(location, viewModel.uiState.value.selectedLocation)
    assertEquals(location.name, viewModel.uiState.value.locationQuery)
  }

  @Test
  fun setLocation_withMultipleLocations_lastOneWins() {
    viewModel.setLocation(sampleLocations[0])
    viewModel.setLocation(sampleLocations[1])

    assertEquals(sampleLocations[1], viewModel.uiState.value.selectedLocation)
    assertEquals(sampleLocations[1].name, viewModel.uiState.value.locationQuery)
  }

  // ========== Clear Suggestions Tests ==========

  @Test
  fun clearLocationSuggestions_removesOnlySuggestions() = runTest {
    coEvery { locationRepository.search("Test") } returns sampleLocations
    viewModel.setLocationQuery("Test")
    advanceUntilIdle()

    viewModel.clearLocationSuggestions()

    assertTrue(viewModel.uiState.value.locationSuggestions.isEmpty())
    assertEquals("Test", viewModel.uiState.value.locationQuery)
    assertNull(viewModel.uiState.value.selectedLocation)
  }

  // ========== Edge Cases ==========

  @Test
  fun edgeCases_specialCharactersAndLongQueries() = runTest {
    // Test special characters
    val specialQuery = "Zürich-Örlikon"
    coEvery { locationRepository.search(specialQuery) } returns sampleLocations
    viewModel.setLocationQuery(specialQuery)
    advanceUntilIdle()
    assertEquals(specialQuery, viewModel.uiState.value.locationQuery)
    assertEquals(sampleLocations, viewModel.uiState.value.locationSuggestions)

    // Test very long query
    val longQuery = "A".repeat(1000)
    coEvery { locationRepository.search(longQuery) } returns emptyList()
    viewModel.setLocationQuery(longQuery)
    advanceUntilIdle()
    assertEquals(longQuery, viewModel.uiState.value.locationQuery)
  }

  @Test
  fun multipleOperations_inSequence_maintainCorrectState() = runTest {
    coEvery { locationRepository.search(any()) } returns sampleLocations

    // Query -> Select -> Query again -> Clear
    viewModel.setLocationQuery("Zürich")
    advanceUntilIdle()
    assertEquals(sampleLocations, viewModel.uiState.value.locationSuggestions)

    viewModel.setLocation(sampleLocations[0])
    assertEquals(sampleLocations[0], viewModel.uiState.value.selectedLocation)

    viewModel.setLocationQuery("Lausanne")
    advanceUntilIdle()
    assertNull(viewModel.uiState.value.selectedLocation)
    assertEquals(sampleLocations, viewModel.uiState.value.locationSuggestions)

    viewModel.clearLocationSuggestions()
    assertTrue(viewModel.uiState.value.locationSuggestions.isEmpty())
  }

  // ========== Helper Method Tests ==========

  @Test
  fun setLocationSuggestions_updatesStateDirectly() {
    viewModel.setLocationSuggestions(sampleLocations)

    assertEquals(sampleLocations, viewModel.uiState.value.locationSuggestions)
  }

  @Test
  fun isLoadingLocations_returnsCorrectState() = runTest {
    assertFalse(viewModel.isLoadingLocations())

    coEvery { locationRepository.search(any()) } coAnswers
        {
          kotlinx.coroutines.delay(100)
          sampleLocations
        }

    viewModel.setLocationQuery("Test")
    assertTrue(viewModel.isLoadingLocations())

    advanceUntilIdle()
    assertFalse(viewModel.isLoadingLocations())
  }
}
