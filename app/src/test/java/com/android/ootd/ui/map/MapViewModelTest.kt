package com.android.ootd.ui.map

import com.android.ootd.model.account.Account
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.account.PublicLocation
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
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
          friendUids = listOf("friend1", "friend2"),
          isPrivate = false)

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
  fun getPostsWithAdjustedLocations_singlePostOrEmptyList_usesOriginalLocations() = runTest {
    val post =
        OutfitPost(
            postUID = "post1", ownerId = "friend1", name = "Friend One", location = testLocation)

    coEvery { mockAccountRepository.observeAccount(testUserId) } returns flowOf(testAccount)

    // Test with single post
    coEvery { mockFeedRepository.observeRecentFeedForUids(any()) } returns flowOf(listOf(post))
    viewModel = MapViewModel(mockFeedRepository, mockAccountRepository)
    advanceUntilIdle()

    var adjustedPosts = viewModel.getPostsWithAdjustedLocations(viewModel.uiState.value.posts)
    assertEquals(1, adjustedPosts.size)
    assertEquals(post, adjustedPosts[0].post)
    assertEquals(testLocation.latitude, adjustedPosts[0].position.latitude, 0.0001)
    assertEquals(testLocation.longitude, adjustedPosts[0].position.longitude, 0.0001)

    // Test with empty list
    coEvery { mockFeedRepository.observeRecentFeedForUids(any()) } returns flowOf(emptyList())
    viewModel = MapViewModel(mockFeedRepository, mockAccountRepository)
    advanceUntilIdle()

    adjustedPosts = viewModel.getPostsWithAdjustedLocations(viewModel.uiState.value.posts)
    assertTrue(adjustedPosts.isEmpty())
  }

  @Test
  fun getPostsWithAdjustedLocations_multiplePostsAtSameLocation_offsetsCorrectly() = runTest {
    val sameLocation = Location(46.5197, 6.6323, "Same Location")

    // Test with 2, 3, and 10 posts at same location to verify circular distribution
    val testCases = listOf(2, 3, 10)

    testCases.forEach { count ->
      val posts =
          (1..count).map {
            OutfitPost(
                postUID = "post$it",
                ownerId = "friend$it",
                name = "Friend $it",
                location = sameLocation)
          }

      coEvery { mockAccountRepository.observeAccount(testUserId) } returns flowOf(testAccount)
      coEvery { mockFeedRepository.observeRecentFeedForUids(any()) } returns flowOf(posts)

      viewModel = MapViewModel(mockFeedRepository, mockAccountRepository)
      advanceUntilIdle()

      val adjustedPosts = viewModel.getPostsWithAdjustedLocations(viewModel.uiState.value.posts)

      // Verify count
      assertEquals(count, adjustedPosts.size)

      // All adjusted locations should be unique
      val adjustedLocations =
          adjustedPosts.map { "${it.position.latitude},${it.position.longitude}" }
      assertEquals(count, adjustedLocations.distinct().size)

      // All adjusted locations should be close to original (within ~30 meters)
      adjustedPosts.forEach { adjusted ->
        val latDiff = kotlin.math.abs(adjusted.position.latitude - sameLocation.latitude)
        val lonDiff = kotlin.math.abs(adjusted.position.longitude - sameLocation.longitude)
        assertTrue("Latitude offset too large: $latDiff for count $count", latDiff < 0.0006)
        assertTrue("Longitude offset too large: $lonDiff for count $count", lonDiff < 0.0006)
      }
    }
  }

  @Test
  fun getPostsWithAdjustedLocations_multipleLocations_onlyAdjustsOverlapping() = runTest {
    val location1 = Location(46.5197, 6.6323, "Location 1")
    val location2 = Location(46.5198, 6.6324, "Location 2")
    val posts =
        listOf(
            OutfitPost(
                postUID = "post1", ownerId = "friend1", name = "Friend One", location = location1),
            OutfitPost(
                postUID = "post2", ownerId = "friend2", name = "Friend Two", location = location1),
            OutfitPost(
                postUID = "post3",
                ownerId = "friend3",
                name = "Friend Three",
                location = location2))

    coEvery { mockAccountRepository.observeAccount(testUserId) } returns flowOf(testAccount)
    coEvery { mockFeedRepository.observeRecentFeedForUids(any()) } returns flowOf(posts)

    viewModel = MapViewModel(mockFeedRepository, mockAccountRepository)
    advanceUntilIdle()

    val adjustedPosts = viewModel.getPostsWithAdjustedLocations(viewModel.uiState.value.posts)

    assertEquals(3, adjustedPosts.size)

    // First two posts should have different adjusted locations
    val pos1 = adjustedPosts[0].position
    val pos2 = adjustedPosts[1].position
    assertTrue(
        "Locations should be different",
        pos1.latitude != pos2.latitude || pos1.longitude != pos2.longitude)

    // Third post should use original location
    assertEquals(location2.latitude, adjustedPosts[2].position.latitude, 0.0001)
    assertEquals(location2.longitude, adjustedPosts[2].position.longitude, 0.0001)
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

  @Test
  fun viewModel_withFocusLocation_setsInitialFocusCorrectly() = runTest {
    val focusLocation = Location(46.5197, 6.6323, "EPFL")
    coEvery { mockAccountRepository.observeAccount(testUserId) } returns flowOf(testAccount)
    coEvery { mockFeedRepository.observeRecentFeedForUids(any()) } returns flowOf(emptyList())

    viewModel = MapViewModel(mockFeedRepository, mockAccountRepository, focusLocation)
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertEquals(focusLocation, uiState.focusLocation)
    assertFalse(uiState.isLoading)
  }

  @Test
  fun getFocusLatLng_prefersFocusLocationOverUserLocation() = runTest {
    val focusLocation = Location(47.5, 7.5, "Focus")
    val userLocation = Location(46.5, 6.5, "User")
    val accountWithLocation = testAccount.copy(location = userLocation)

    coEvery { mockAccountRepository.observeAccount(testUserId) } returns flowOf(accountWithLocation)
    coEvery { mockFeedRepository.observeRecentFeedForUids(any()) } returns flowOf(emptyList())

    viewModel = MapViewModel(mockFeedRepository, mockAccountRepository, focusLocation)
    advanceUntilIdle()

    val latLng = viewModel.getFocusLatLng()
    // Should return focus location, not user location
    assertEquals(47.5, latLng.latitude, 0.0001)
    assertEquals(7.5, latLng.longitude, 0.0001)
  }

  @Test
  fun viewModel_withNullFocusLocation_usesFallbackUserLocation() = runTest {
    coEvery { mockAccountRepository.observeAccount(testUserId) } returns flowOf(testAccount)
    coEvery { mockFeedRepository.observeRecentFeedForUids(any()) } returns flowOf(emptyList())

    viewModel = MapViewModel(mockFeedRepository, mockAccountRepository, focusLocation = null)
    advanceUntilIdle()

    val latLng = viewModel.getFocusLatLng()
    // Should return user location when focus is null
    assertEquals(testLocation.latitude, latLng.latitude, 0.0001)
    assertEquals(testLocation.longitude, latLng.longitude, 0.0001)
  }

  @Test
  fun initialMapType_isFriendsPosts() = runTest {
    coEvery { mockAccountRepository.observeAccount(testUserId) } returns flowOf(testAccount)
    coEvery { mockFeedRepository.observeRecentFeedForUids(any()) } returns flowOf(emptyList())

    viewModel = MapViewModel(mockFeedRepository, mockAccountRepository)
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertEquals(MapType.FRIENDS_POSTS, uiState.selectedMapType)
  }

  @Test
  fun setMapType_togglesBetweenMaps() = runTest {
    coEvery { mockAccountRepository.observeAccount(testUserId) } returns flowOf(testAccount)
    coEvery { mockFeedRepository.observeRecentFeedForUids(any()) } returns flowOf(emptyList())

    viewModel = MapViewModel(mockFeedRepository, mockAccountRepository)
    advanceUntilIdle()

    // Initial state
    assertEquals(MapType.FRIENDS_POSTS, viewModel.uiState.value.selectedMapType)

    // Toggle to FIND_FRIENDS
    viewModel.setMapType(MapType.FIND_FRIENDS)
    assertEquals(MapType.FIND_FRIENDS, viewModel.uiState.value.selectedMapType)

    // Toggle back to FRIENDS_POSTS
    viewModel.setMapType(MapType.FRIENDS_POSTS)
    assertEquals(MapType.FRIENDS_POSTS, viewModel.uiState.value.selectedMapType)

    // Toggle again
    viewModel.setMapType(MapType.FIND_FRIENDS)
    assertEquals(MapType.FIND_FRIENDS, viewModel.uiState.value.selectedMapType)
  }

  @Test
  fun setMapType_doesNotAffectOtherUIState() = runTest {
    coEvery { mockAccountRepository.observeAccount(testUserId) } returns flowOf(testAccount)
    coEvery { mockFeedRepository.observeRecentFeedForUids(any()) } returns flowOf(emptyList())

    viewModel = MapViewModel(mockFeedRepository, mockAccountRepository)
    advanceUntilIdle()

    val initialLocation = viewModel.uiState.value.userLocation
    val initialAccount = viewModel.uiState.value.currentAccount
    val initialPosts = viewModel.uiState.value.posts
    val initialLoading = viewModel.uiState.value.isLoading

    viewModel.setMapType(MapType.FIND_FRIENDS)

    val updatedState = viewModel.uiState.value
    assertEquals(initialLocation, updatedState.userLocation)
    assertEquals(initialAccount, updatedState.currentAccount)
    assertEquals(initialPosts, updatedState.posts)
    assertEquals(initialLoading, updatedState.isLoading)
    // Only selectedMapType should change
    assertEquals(MapType.FIND_FRIENDS, updatedState.selectedMapType)
  }

  @Test
  fun observePublicLocations_filtersValidLocations() = runTest {
    val validPublicLocation = PublicLocation("user1", "Alice", testLocation)
    val invalidPublicLocation = PublicLocation("user2", "Bob", emptyLocation)

    coEvery { mockAccountRepository.observeAccount(testUserId) } returns flowOf(testAccount)
    coEvery { mockFeedRepository.observeRecentFeedForUids(any()) } returns flowOf(emptyList())
    coEvery { mockAccountRepository.observePublicLocations() } returns
        flowOf(listOf(validPublicLocation, invalidPublicLocation))

    viewModel = MapViewModel(mockFeedRepository, mockAccountRepository)
    advanceUntilIdle()

    assertEquals(1, viewModel.uiState.value.publicLocations.size)
    assertEquals("user1", viewModel.uiState.value.publicLocations[0].ownerId)
  }

  @Test
  fun getPublicLocationsWithAdjusted_handlesOverlappingLocations() = runTest {
    val location = Location(46.5, 6.6, "Same")
    val publicLocs =
        listOf(
            PublicLocation("user1", "Alice", location),
            PublicLocation("user2", "Bob", location),
            PublicLocation("user3", "Charlie", Location(47.0, 7.0, "Different")))

    coEvery { mockAccountRepository.observeAccount(testUserId) } returns flowOf(testAccount)
    coEvery { mockFeedRepository.observeRecentFeedForUids(any()) } returns flowOf(emptyList())
    coEvery { mockAccountRepository.observePublicLocations() } returns flowOf(publicLocs)

    viewModel = MapViewModel(mockFeedRepository, mockAccountRepository)
    advanceUntilIdle()

    val adjusted = viewModel.getPublicLocationsWithAdjusted(viewModel.uiState.value.publicLocations)
    assertEquals(3, adjusted.size)
    // First two should have different positions
    val pos1 = adjusted[0].position
    val pos2 = adjusted[1].position
    assert(pos1.latitude != pos2.latitude || pos1.longitude != pos2.longitude)
  }

  @Test
  fun hasUserPostedToday_returnsTrue_whenUserHasPosted() = runTest {
    coEvery { mockAccountRepository.observeAccount(testUserId) } returns flowOf(testAccount)
    coEvery { mockFeedRepository.observeRecentFeedForUids(any()) } returns flowOf(emptyList())
    coEvery { mockFeedRepository.hasPostedToday(testUserId) } returns true

    viewModel = MapViewModel(mockFeedRepository, mockAccountRepository)
    advanceUntilIdle()

    val hasPosted = viewModel.hasUserPostedToday()
    assertTrue(hasPosted)
  }

  @Test
  fun hasUserPostedToday_returnsFalse_whenUserHasNotPosted() = runTest {
    coEvery { mockAccountRepository.observeAccount(testUserId) } returns flowOf(testAccount)
    coEvery { mockFeedRepository.observeRecentFeedForUids(any()) } returns flowOf(emptyList())
    coEvery { mockFeedRepository.hasPostedToday(testUserId) } returns false

    viewModel = MapViewModel(mockFeedRepository, mockAccountRepository)
    advanceUntilIdle()

    val hasPosted = viewModel.hasUserPostedToday()
    assertFalse(hasPosted)
  }

  @Test
  fun hasUserPostedToday_returnsFalse_whenNoUserIsLoggedIn() = runTest {
    every { mockFirebaseAuth.currentUser } returns null

    coEvery { mockAccountRepository.observeAccount(any()) } returns flowOf(testAccount)
    coEvery { mockFeedRepository.observeRecentFeedForUids(any()) } returns flowOf(emptyList())

    viewModel = MapViewModel(mockFeedRepository, mockAccountRepository)
    advanceUntilIdle()

    val hasPosted = viewModel.hasUserPostedToday()
    assertFalse(hasPosted)
  }

  @Test
  fun hasUserPostedToday_returnsFalse_onException() = runTest {
    coEvery { mockAccountRepository.observeAccount(testUserId) } returns flowOf(testAccount)
    coEvery { mockFeedRepository.observeRecentFeedForUids(any()) } returns flowOf(emptyList())
    coEvery { mockFeedRepository.hasPostedToday(testUserId) } throws Exception("Network error")

    viewModel = MapViewModel(mockFeedRepository, mockAccountRepository)
    advanceUntilIdle()

    val hasPosted = viewModel.hasUserPostedToday()
    assertFalse(hasPosted)
  }

  @Test
  fun showSnackbar_setsSnackbarMessage() = runTest {
    coEvery { mockAccountRepository.observeAccount(testUserId) } returns flowOf(testAccount)
    coEvery { mockFeedRepository.observeRecentFeedForUids(any()) } returns flowOf(emptyList())

    viewModel = MapViewModel(mockFeedRepository, mockAccountRepository)
    advanceUntilIdle()

    val testMessage = "Test snackbar message"
    viewModel.showSnackbar(testMessage)

    assertEquals(testMessage, viewModel.uiState.value.snackbarMessage)
  }

  @Test
  fun snackbarMessage_lifecycle_worksCorrectly() = runTest {
    coEvery { mockAccountRepository.observeAccount(testUserId) } returns flowOf(testAccount)
    coEvery { mockFeedRepository.observeRecentFeedForUids(any()) } returns flowOf(emptyList())

    viewModel = MapViewModel(mockFeedRepository, mockAccountRepository)
    advanceUntilIdle()

    // Initially null
    assertEquals(null, viewModel.uiState.value.snackbarMessage)

    // Set a message
    viewModel.showSnackbar("Test message")
    assertEquals("Test message", viewModel.uiState.value.snackbarMessage)

    // Clear the message
    viewModel.clearSnackbar()
    assertEquals(null, viewModel.uiState.value.snackbarMessage)

    // Can be called multiple times
    viewModel.showSnackbar("Message 1")
    assertEquals("Message 1", viewModel.uiState.value.snackbarMessage)
    viewModel.showSnackbar("Message 2")
    assertEquals("Message 2", viewModel.uiState.value.snackbarMessage)

    // Does not affect other state
    val finalLocation = viewModel.uiState.value.userLocation
    val finalAccount = viewModel.uiState.value.currentAccount
    val finalMapType = viewModel.uiState.value.selectedMapType

    viewModel.clearSnackbar()
    viewModel.showSnackbar("Final message")

    val updatedState = viewModel.uiState.value
    assertEquals(finalLocation, updatedState.userLocation)
    assertEquals(finalAccount, updatedState.currentAccount)
    assertEquals(finalMapType, updatedState.selectedMapType)
    assertEquals("Final message", updatedState.snackbarMessage)
  }

  @Test
  fun observePublicLocations_excludesCurrentUserAndFriends() = runTest {
    val currentUserLoc = PublicLocation(testUserId, "Current User", testLocation)
    val friend1Loc = PublicLocation("friend1", "Friend One", Location(46.6, 6.7, "Loc1"))
    val friend2Loc = PublicLocation("friend2", "Friend Two", Location(47.0, 7.0, "Loc2"))
    val publicUser1 = PublicLocation("user1", "Alice", Location(48.0, 8.0, "Loc3"))
    val publicUser2 = PublicLocation("user2", "Bob", Location(49.0, 9.0, "Loc4"))
    val invalidPublic = PublicLocation("user3", "Charlie", emptyLocation)

    coEvery { mockAccountRepository.observeAccount(testUserId) } returns flowOf(testAccount)
    coEvery { mockFeedRepository.observeRecentFeedForUids(any()) } returns flowOf(emptyList())
    coEvery { mockAccountRepository.observePublicLocations() } returns
        flowOf(
            listOf(currentUserLoc, friend1Loc, friend2Loc, publicUser1, publicUser2, invalidPublic))

    viewModel = MapViewModel(mockFeedRepository, mockAccountRepository)
    advanceUntilIdle()

    val publicLocations = viewModel.uiState.value.publicLocations
    // Should only show valid non-friend users (user1 and user2)
    assertEquals(2, publicLocations.size)
    assertTrue(publicLocations.all { it.ownerId in setOf("user1", "user2") })
    assertFalse(
        publicLocations.any { it.ownerId in setOf(testUserId, "friend1", "friend2", "user3") })
  }

  @Test
  fun observePublicLocations_whenAccountIsNull_showsAllValidLocations() = runTest {
    val validLoc1 = PublicLocation("user1", "Alice", testLocation)
    val validLoc2 = PublicLocation("user2", "Bob", Location(47.0, 7.0, "Other"))
    val invalidLoc = PublicLocation("user3", "Charlie", emptyLocation)

    coEvery { mockAccountRepository.observeAccount(testUserId) } throws Exception("No account")
    coEvery { mockAccountRepository.observePublicLocations() } returns
        flowOf(listOf(validLoc1, validLoc2, invalidLoc))

    viewModel = MapViewModel(mockFeedRepository, mockAccountRepository)
    advanceUntilIdle()

    // Without account, should show all valid locations (no UID filtering)
    val publicLocations = viewModel.uiState.value.publicLocations
    assertEquals(2, publicLocations.size)
    assertTrue(publicLocations.all { it.ownerId in setOf("user1", "user2") })
  }

  @Test
  fun observePublicLocations_filtersCurrentUserEvenWithRaceCondition() = runTest {
    val currentUserLoc = PublicLocation(testUserId, "Current User", testLocation)
    val otherUserLoc = PublicLocation("user1", "Alice", Location(47.0, 7.0, "Other"))

    // Simulate race condition: public locations arrive before account is loaded
    val accountFlow = MutableStateFlow<Account?>(null)

    coEvery { mockAccountRepository.observeAccount(testUserId) } returns accountFlow.filterNotNull()
    coEvery { mockFeedRepository.observeRecentFeedForUids(any()) } returns flowOf(emptyList())
    coEvery { mockAccountRepository.observePublicLocations() } returns
        flowOf(listOf(currentUserLoc, otherUserLoc))

    viewModel = MapViewModel(mockFeedRepository, mockAccountRepository)
    advanceUntilIdle()

    // Now load the account
    accountFlow.value = testAccount
    advanceUntilIdle()

    // After account loads, current user should be filtered out
    val finalPublicLocations = viewModel.uiState.value.publicLocations
    assertEquals(1, finalPublicLocations.size)
    assertEquals("user1", finalPublicLocations[0].ownerId)
    assertFalse(finalPublicLocations.any { it.ownerId == testUserId })
  }
}
