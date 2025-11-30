package com.android.ootd.ui.map

import com.android.ootd.model.account.Account
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.feed.FeedRepository
import com.android.ootd.model.map.Location
import com.android.ootd.model.map.emptyLocation
import com.android.ootd.model.posts.OutfitPost
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
 * - Real-time observation of account and posts
 *
 * Disclaimer: This test was written with the assistance of AI.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

  private lateinit var viewModel: MapViewModel
  private lateinit var mockAccountRepository: AccountRepository
  private lateinit var mockFeedRepository: FeedRepository
  private lateinit var mockFirebaseAuth: FirebaseAuth
  private lateinit var mockFirebaseUser: FirebaseUser
  private val testDispatcher = StandardTestDispatcher()

  private val testUserId = "testUser123"
  private val testLocation = Location(46.5197, 6.6323, "Lausanne")
  private val testAccount =
      Account(
          uid = testUserId,
          ownerId = testUserId,
          username = "testUser",
          location = testLocation,
          friendUids = listOf("friend1", "friend2"))

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)

    mockAccountRepository = mockk(relaxed = true)
    mockFeedRepository = mockk(relaxed = true)
    mockFirebaseAuth = mockk(relaxed = true)
    mockFirebaseUser = mockk(relaxed = true)

    // Mock Firebase
    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns mockFirebaseAuth
    every { mockFirebaseAuth.currentUser } returns mockFirebaseUser
    every { mockFirebaseUser.uid } returns testUserId

    //  Mock addAuthStateListener to invoke the callback immediately
    every { mockFirebaseAuth.addAuthStateListener(any()) } answers
        {
          val listener = firstArg<FirebaseAuth.AuthStateListener>()
          listener.onAuthStateChanged(mockFirebaseAuth)
          mockk(relaxed = true) // Return a mock listener registration
        }
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun initialState_isLoading() {
    coEvery { mockAccountRepository.observeAccount(testUserId) } returns flowOf(testAccount)
    coEvery { mockFeedRepository.observeRecentFeedForUids(any()) } returns flowOf(emptyList())

    viewModel = MapViewModel(mockFeedRepository, mockAccountRepository)

    assertTrue(viewModel.uiState.value.isLoading)
  }

  @Test
  fun observeAccount_setsLocationFromAccount() = runTest {
    coEvery { mockAccountRepository.observeAccount(testUserId) } returns flowOf(testAccount)
    coEvery { mockFeedRepository.observeRecentFeedForUids(any()) } returns flowOf(emptyList())

    viewModel = MapViewModel(mockFeedRepository, mockAccountRepository)
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertFalse(uiState.isLoading)
    assertEquals(testLocation.latitude, uiState.userLocation.latitude, 0.0001)
    assertEquals(testLocation.longitude, uiState.userLocation.longitude, 0.0001)
    assertEquals(testLocation.name, uiState.userLocation.name)
    assertEquals(testAccount, uiState.currentAccount)
  }

  @Test
  fun getUserLatLng_returnsCorrectLatLng() = runTest {
    coEvery { mockAccountRepository.observeAccount(testUserId) } returns flowOf(testAccount)
    coEvery { mockFeedRepository.observeRecentFeedForUids(any()) } returns flowOf(emptyList())

    viewModel = MapViewModel(mockFeedRepository, mockAccountRepository)
    advanceUntilIdle()

    val latLng = viewModel.getUserLatLng()
    assertEquals(testLocation.latitude, latLng.latitude, 0.0001)
    assertEquals(testLocation.longitude, latLng.longitude, 0.0001)
  }

  @Test
  fun observeAccount_onError_usesEmptyLocation() = runTest {
    coEvery { mockAccountRepository.observeAccount(testUserId) } throws Exception("Network error")

    viewModel = MapViewModel(mockFeedRepository, mockAccountRepository)
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertFalse(uiState.isLoading)
    assertEquals(emptyLocation, uiState.userLocation)
  }

  @Test
  fun getUserLatLng_withEmptyLocation_returnsZeroCoordinates() = runTest {
    coEvery { mockAccountRepository.observeAccount(testUserId) } throws Exception("Error")

    viewModel = MapViewModel(mockFeedRepository, mockAccountRepository)
    advanceUntilIdle()

    val latLng = viewModel.getUserLatLng()
    assertEquals(0.0, latLng.latitude, 0.0001)
    assertEquals(0.0, latLng.longitude, 0.0001)
  }

  @Test
  fun observePosts_filtersPostsWithValidLocations() = runTest {
    val validPost =
        OutfitPost(
            postUID = "post1",
            ownerId = "friend1",
            name = "Friend One",
            location = Location(46.5, 6.5, "Valid Location"))
    val invalidPost =
        OutfitPost(
            postUID = "post2", ownerId = "friend2", name = "Friend Two", location = emptyLocation)

    coEvery { mockAccountRepository.observeAccount(testUserId) } returns flowOf(testAccount)
    coEvery { mockFeedRepository.observeRecentFeedForUids(any()) } returns
        flowOf(listOf(validPost, invalidPost))

    viewModel = MapViewModel(mockFeedRepository, mockAccountRepository)
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertEquals(1, uiState.posts.size)
    assertEquals(validPost, uiState.posts[0])
  }

  @Test
  fun observeLocalizablePosts_filtersValidLocations() = runTest {
    val validPost =
        OutfitPost(
            postUID = "post1",
            ownerId = testUserId,
            location = Location(46.5, 6.6, "Valid Location"))
    val invalidPost = OutfitPost(postUID = "post2", ownerId = testUserId, location = emptyLocation)

    coEvery { mockAccountRepository.observeAccount(testUserId) } returns flowOf(testAccount)
    coEvery { mockFeedRepository.observeRecentFeedForUids(any()) } returns
        flowOf(listOf(validPost, invalidPost))

    viewModel = MapViewModel(mockFeedRepository, mockAccountRepository)
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertEquals(1, uiState.posts.size)
    assertEquals("post1", uiState.posts[0].postUID)
  }

  @Test
  fun getFocusLatLng_withFocusLocation_returnsFocusLocation() = runTest {
    val focusLocation = Location(47.0, 7.0, "Focus Location")
    coEvery { mockAccountRepository.observeAccount(testUserId) } returns flowOf(testAccount)
    coEvery { mockFeedRepository.observeRecentFeedForUids(any()) } returns flowOf(emptyList())

    viewModel = MapViewModel(mockFeedRepository, mockAccountRepository, focusLocation)
    advanceUntilIdle()

    val latLng = viewModel.getFocusLatLng()
    assertEquals(47.0, latLng.latitude, 0.0001)
    assertEquals(7.0, latLng.longitude, 0.0001)
  }

  @Test
  fun getFocusLatLng_withoutFocusLocation_returnsUserLocation() = runTest {
    coEvery { mockAccountRepository.observeAccount(testUserId) } returns flowOf(testAccount)
    coEvery { mockFeedRepository.observeRecentFeedForUids(any()) } returns flowOf(emptyList())

    viewModel = MapViewModel(mockFeedRepository, mockAccountRepository, focusLocation = null)
    advanceUntilIdle()

    val latLng = viewModel.getFocusLatLng()
    assertEquals(testLocation.latitude, latLng.latitude, 0.0001)
    assertEquals(testLocation.longitude, latLng.longitude, 0.0001)
  }

  @Test
  fun focusLocation_isPreservedWhenAccountUpdates() = runTest {
    val focusLocation = Location(47.0, 7.0, "Focus Location")
    val updatedAccount = testAccount.copy(location = Location(48.0, 8.0, "Updated Location"))

    coEvery { mockAccountRepository.observeAccount(testUserId) } returns
        flowOf(testAccount, updatedAccount)
    coEvery { mockFeedRepository.observeRecentFeedForUids(any()) } returns flowOf(emptyList())

    viewModel = MapViewModel(mockFeedRepository, mockAccountRepository, focusLocation)
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    // Focus location should remain unchanged
    assertEquals(47.0, uiState.focusLocation?.latitude)
    assertEquals(7.0, uiState.focusLocation?.longitude)
    // User location should be updated
    assertEquals(48.0, uiState.userLocation.latitude, 0.0001)
  }
}
