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
  fun getPostsWithAdjustedLocations_singlePost_noOffset() = runTest {
    val post =
        OutfitPost(
            postUID = "post1",
            ownerId = "friend1",
            name = "User One",
            location = Location(46.5197, 6.6323, "EPFL"))

    coEvery { mockAccountRepository.observeAccount(testUserId) } returns flowOf(testAccount)
    coEvery { mockFeedRepository.observeRecentFeedForUids(any()) } returns flowOf(listOf(post))

    viewModel = MapViewModel(mockFeedRepository, mockAccountRepository)
    advanceUntilIdle()

    val adjustedPosts = viewModel.getPostsWithAdjustedLocations()

    assertEquals(1, adjustedPosts.size)
    assertEquals(post.location, adjustedPosts[0].adjustedLocation)
    assertEquals(1, adjustedPosts[0].overlappingCount)
  }

  @Test
  fun getPostsWithAdjustedLocations_twoPostsSameLocation_offsetsApplied() = runTest {
    val location = Location(46.5197, 6.6323, "EPFL")
    val post1 =
        OutfitPost(postUID = "post1", ownerId = "friend1", name = "User One", location = location)
    val post2 =
        OutfitPost(postUID = "post2", ownerId = "friend2", name = "User Two", location = location)

    coEvery { mockAccountRepository.observeAccount(testUserId) } returns flowOf(testAccount)
    coEvery { mockFeedRepository.observeRecentFeedForUids(any()) } returns
        flowOf(listOf(post1, post2))

    viewModel = MapViewModel(mockFeedRepository, mockAccountRepository)
    advanceUntilIdle()

    val adjustedPosts = viewModel.getPostsWithAdjustedLocations()

    assertEquals(2, adjustedPosts.size)
    // Both should have overlapping count of 2
    assertEquals(2, adjustedPosts[0].overlappingCount)
    assertEquals(2, adjustedPosts[1].overlappingCount)
    // Locations should be different (offset applied)
    assertFalse(
        adjustedPosts[0].adjustedLocation.latitude == adjustedPosts[1].adjustedLocation.latitude &&
            adjustedPosts[0].adjustedLocation.longitude ==
                adjustedPosts[1].adjustedLocation.longitude)
  }

  @Test
  fun getPostsWithAdjustedLocations_threePostsSameLocation_circularOffset() = runTest {
    val location = Location(46.5197, 6.6323, "EPFL")
    val post1 =
        OutfitPost(postUID = "post1", ownerId = "friend1", name = "User One", location = location)
    val post2 =
        OutfitPost(postUID = "post2", ownerId = "friend2", name = "User Two", location = location)
    val post3 =
        OutfitPost(postUID = "post3", ownerId = "friend3", name = "User Three", location = location)

    coEvery { mockAccountRepository.observeAccount(testUserId) } returns flowOf(testAccount)
    coEvery { mockFeedRepository.observeRecentFeedForUids(any()) } returns
        flowOf(listOf(post1, post2, post3))

    viewModel = MapViewModel(mockFeedRepository, mockAccountRepository)
    advanceUntilIdle()

    val adjustedPosts = viewModel.getPostsWithAdjustedLocations()

    assertEquals(3, adjustedPosts.size)
    // All should have overlapping count of 3
    adjustedPosts.forEach { assertEquals(3, it.overlappingCount) }
    // All locations should be different
    val locations =
        adjustedPosts.map { "${it.adjustedLocation.latitude},${it.adjustedLocation.longitude}" }
    assertEquals(3, locations.distinct().size)
  }

  @Test
  fun getPostsWithAdjustedLocations_mixedLocations_correctGrouping() = runTest {
    val location1 = Location(46.5197, 6.6323, "EPFL")
    val location2 = Location(46.5198, 6.6324, "Lausanne")

    val post1 =
        OutfitPost(postUID = "post1", ownerId = "friend1", name = "User One", location = location1)
    val post2 =
        OutfitPost(postUID = "post2", ownerId = "friend2", name = "User Two", location = location1)
    val post3 =
        OutfitPost(
            postUID = "post3", ownerId = "friend3", name = "User Three", location = location2)

    coEvery { mockAccountRepository.observeAccount(testUserId) } returns flowOf(testAccount)
    coEvery { mockFeedRepository.observeRecentFeedForUids(any()) } returns
        flowOf(listOf(post1, post2, post3))

    viewModel = MapViewModel(mockFeedRepository, mockAccountRepository)
    advanceUntilIdle()

    val adjustedPosts = viewModel.getPostsWithAdjustedLocations()

    assertEquals(3, adjustedPosts.size)

    // Posts at location1 should have overlapping count of 2
    val postsAtLocation1 =
        adjustedPosts.filter { it.post.postUID == "post1" || it.post.postUID == "post2" }
    postsAtLocation1.forEach { assertEquals(2, it.overlappingCount) }

    // Post at location2 should have overlapping count of 1
    val postAtLocation2 = adjustedPosts.first { it.post.postUID == "post3" }
    assertEquals(1, postAtLocation2.overlappingCount)
    assertEquals(location2, postAtLocation2.adjustedLocation)
  }

  @Test
  fun getPostsWithAdjustedLocations_offsetStaysNearOriginal() = runTest {
    val location = Location(46.5197, 6.6323, "EPFL")
    val post1 =
        OutfitPost(postUID = "post1", ownerId = "friend1", name = "User One", location = location)
    val post2 =
        OutfitPost(postUID = "post2", ownerId = "friend2", name = "User Two", location = location)

    coEvery { mockAccountRepository.observeAccount(testUserId) } returns flowOf(testAccount)
    coEvery { mockFeedRepository.observeRecentFeedForUids(any()) } returns
        flowOf(listOf(post1, post2))

    viewModel = MapViewModel(mockFeedRepository, mockAccountRepository)
    advanceUntilIdle()

    val adjustedPosts = viewModel.getPostsWithAdjustedLocations()

    // Verify offsets are small (within 0.001 degrees, approximately 100m)
    adjustedPosts.forEach { adjusted ->
      val latDiff = kotlin.math.abs(adjusted.adjustedLocation.latitude - location.latitude)
      val lonDiff = kotlin.math.abs(adjusted.adjustedLocation.longitude - location.longitude)
      assertTrue(latDiff < 0.001)
      assertTrue(lonDiff < 0.001)
    }
  }

  @Test
  fun getPostsWithAdjustedLocations_verifyBadgeCountForOverlappingMarkers() = runTest {
    val location = Location(46.5197, 6.6323, "EPFL")
    val post1 =
        OutfitPost(postUID = "post1", ownerId = "friend1", name = "User One", location = location)
    val post2 =
        OutfitPost(postUID = "post2", ownerId = "friend2", name = "User Two", location = location)
    val post3 =
        OutfitPost(postUID = "post3", ownerId = "friend3", name = "User Three", location = location)

    coEvery { mockAccountRepository.observeAccount(testUserId) } returns flowOf(testAccount)
    coEvery { mockFeedRepository.observeRecentFeedForUids(any()) } returns
        flowOf(listOf(post1, post2, post3))

    viewModel = MapViewModel(mockFeedRepository, mockAccountRepository)
    advanceUntilIdle()

    val adjustedPosts = viewModel.getPostsWithAdjustedLocations()

    // All posts at the same location should have overlapping count of 3
    // This ensures the badge will display "3" on all three markers
    adjustedPosts.forEach { assertEquals(3, it.overlappingCount) }
  }

  @Test
  fun getPostsWithAdjustedLocations_ensuresBadgeVisibilityForAllOverlappingPosts() = runTest {
    val sharedLocation = Location(46.5197, 6.6323, "EPFL")
    val posts =
        (1..5).map { i ->
          OutfitPost(
              postUID = "post$i", ownerId = "friend$i", name = "User $i", location = sharedLocation)
        }

    coEvery { mockAccountRepository.observeAccount(testUserId) } returns flowOf(testAccount)
    coEvery { mockFeedRepository.observeRecentFeedForUids(any()) } returns flowOf(posts)

    viewModel = MapViewModel(mockFeedRepository, mockAccountRepository)
    advanceUntilIdle()

    val adjustedPosts = viewModel.getPostsWithAdjustedLocations()

    assertEquals(5, adjustedPosts.size)
    // All 5 posts should have overlapping count of 5 for badge display
    adjustedPosts.forEach { assertEquals(5, it.overlappingCount) }

    // All adjusted locations should be unique (circular offset applied)
    val uniqueLocations =
        adjustedPosts
            .map { "${it.adjustedLocation.latitude},${it.adjustedLocation.longitude}" }
            .distinct()
    assertEquals(5, uniqueLocations.size)
  }

  @Test
  fun getPostsWithAdjustedLocations_multipleGroups_correctBadgeCounts() = runTest {
    val location1 = Location(46.5197, 6.6323, "EPFL")
    val location2 = Location(46.5198, 6.6324, "Lausanne")

    // 3 posts at location1
    val post1 =
        OutfitPost(postUID = "post1", ownerId = "friend1", name = "User One", location = location1)
    val post2 =
        OutfitPost(postUID = "post2", ownerId = "friend2", name = "User Two", location = location1)
    val post3 =
        OutfitPost(
            postUID = "post3", ownerId = "friend3", name = "User Three", location = location1)

    // 2 posts at location2
    val post4 =
        OutfitPost(postUID = "post4", ownerId = "friend4", name = "User Four", location = location2)
    val post5 =
        OutfitPost(postUID = "post5", ownerId = "friend5", name = "User Five", location = location2)

    coEvery { mockAccountRepository.observeAccount(testUserId) } returns flowOf(testAccount)
    coEvery { mockFeedRepository.observeRecentFeedForUids(any()) } returns
        flowOf(listOf(post1, post2, post3, post4, post5))

    viewModel = MapViewModel(mockFeedRepository, mockAccountRepository)
    advanceUntilIdle()

    val adjustedPosts = viewModel.getPostsWithAdjustedLocations()

    assertEquals(5, adjustedPosts.size)

    // Posts at location1 should have badge count of 3
    val postsAtLocation1 =
        adjustedPosts.filter { it.post.postUID in listOf("post1", "post2", "post3") }
    postsAtLocation1.forEach { assertEquals(3, it.overlappingCount) }

    // Posts at location2 should have badge count of 2
    val postsAtLocation2 = adjustedPosts.filter { it.post.postUID in listOf("post4", "post5") }
    postsAtLocation2.forEach { assertEquals(2, it.overlappingCount) }
  }

  @Test
  fun getPostsWithAdjustedLocations_singlePostNoBadge_correctCount() = runTest {
    val location1 = Location(46.5197, 6.6323, "EPFL")
    val location2 = Location(46.5198, 6.6324, "Lausanne")

    // 2 posts at location1
    val post1 =
        OutfitPost(postUID = "post1", ownerId = "friend1", name = "User One", location = location1)
    val post2 =
        OutfitPost(postUID = "post2", ownerId = "friend2", name = "User Two", location = location1)

    // 1 post at location2 (should not show badge)
    val post3 =
        OutfitPost(
            postUID = "post3", ownerId = "friend3", name = "User Three", location = location2)

    coEvery { mockAccountRepository.observeAccount(testUserId) } returns flowOf(testAccount)
    coEvery { mockFeedRepository.observeRecentFeedForUids(any()) } returns
        flowOf(listOf(post1, post2, post3))

    viewModel = MapViewModel(mockFeedRepository, mockAccountRepository)
    advanceUntilIdle()

    val adjustedPosts = viewModel.getPostsWithAdjustedLocations()

    // Posts at location1 should have count of 2 (badge shown)
    val postsAtLocation1 = adjustedPosts.filter { it.post.postUID in listOf("post1", "post2") }
    postsAtLocation1.forEach { assertEquals(2, it.overlappingCount) }

    // Post at location2 should have count of 1 (no badge shown)
    val postAtLocation2 = adjustedPosts.first { it.post.postUID == "post3" }
    assertEquals(1, postAtLocation2.overlappingCount)
  }
}
