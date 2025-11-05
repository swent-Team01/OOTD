package com.android.ootd.ui.map

import com.android.ootd.model.account.Account
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.authentication.AccountService
import com.android.ootd.model.map.Location
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for MapViewModel.
 *
 * Tests cover:
 * - Loading user location from account
 * - Converting location to LatLng
 * - Error handling when account loading fails
 *
 * Disclaimer: This test was written with the assistance of AI.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

  private lateinit var viewModel: MapViewModel
  private lateinit var mockAccountService: AccountService
  private lateinit var mockAccountRepository: AccountRepository
  private val testDispatcher = StandardTestDispatcher()

  private val testUserId = "testUser123"
  private val testLocation = Location(46.5197, 6.6323, "Lausanne")
  private val testAccount =
      Account(
          uid = testUserId, ownerId = testUserId, username = "testUser", location = testLocation)

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    mockAccountService = mockk(relaxed = true)
    mockAccountRepository = mockk(relaxed = true)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun initialState_isLoading() {
    coEvery { mockAccountService.currentUserId } returns testUserId
    coEvery { mockAccountRepository.getAccount(testUserId) } returns testAccount

    viewModel = MapViewModel(mockAccountService, mockAccountRepository)

    assertTrue(viewModel.uiState.value.isLoading)
  }

  @Test
  fun loadUserLocation_setsLocationFromAccount() = runTest {
    coEvery { mockAccountService.currentUserId } returns testUserId
    coEvery { mockAccountRepository.getAccount(testUserId) } returns testAccount

    viewModel = MapViewModel(mockAccountService, mockAccountRepository)
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertFalse(uiState.isLoading)
    assertEquals(testLocation.latitude, uiState.userLocation.latitude, 0.0001)
    assertEquals(testLocation.longitude, uiState.userLocation.longitude, 0.0001)
    assertEquals(testLocation.name, uiState.userLocation.name)
  }

  @Test
  fun getUserLatLng_returnsCorrectLatLng() = runTest {
    coEvery { mockAccountService.currentUserId } returns testUserId
    coEvery { mockAccountRepository.getAccount(testUserId) } returns testAccount

    viewModel = MapViewModel(mockAccountService, mockAccountRepository)
    advanceUntilIdle()

    val latLng = viewModel.getUserLatLng()
    assertEquals(testLocation.latitude, latLng.latitude, 0.0001)
    assertEquals(testLocation.longitude, latLng.longitude, 0.0001)
  }

  @Test
  fun loadUserLocation_onError_usesEmptyLocation() = runTest {
    coEvery { mockAccountService.currentUserId } returns testUserId
    coEvery { mockAccountRepository.getAccount(testUserId) } throws Exception("Network error")

    viewModel = MapViewModel(mockAccountService, mockAccountRepository)
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertFalse(uiState.isLoading)
    assertEquals(emptyLocation, uiState.userLocation)
  }

  @Test
  fun getUserLatLng_withEmptyLocation_returnsZeroCoordinates() = runTest {
    coEvery { mockAccountService.currentUserId } returns testUserId
    coEvery { mockAccountRepository.getAccount(testUserId) } throws Exception("Error")

    viewModel = MapViewModel(mockAccountService, mockAccountRepository)
    advanceUntilIdle()

    val latLng = viewModel.getUserLatLng()
    assertEquals(0.0, latLng.latitude, 0.0001)
    assertEquals(0.0, latLng.longitude, 0.0001)
  }
}
