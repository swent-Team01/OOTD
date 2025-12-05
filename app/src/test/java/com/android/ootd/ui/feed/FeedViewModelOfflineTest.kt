package com.android.ootd.ui.feed

import com.android.ootd.model.account.Account
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.feed.FeedRepository
import com.android.ootd.model.post.OutfitPostRepository
import com.android.ootd.model.posts.LikesRepository
import com.android.ootd.model.posts.OutfitPost
import com.google.firebase.auth.FirebaseAuth
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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
import org.junit.Before
import org.junit.Test

/**
 * Tests for FeedViewModel offline behavior.
 *
 * Verifies that the ViewModel correctly falls back to cached data when network is unavailable.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModelOfflineTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var feedRepository: FeedRepository
  private lateinit var postRepository: OutfitPostRepository
  private lateinit var accountRepository: AccountRepository
  private lateinit var likesRepository: LikesRepository
  private lateinit var firebaseAuth: FirebaseAuth
  private lateinit var viewModel: FeedViewModel

  private val now = System.currentTimeMillis()
  private val testAccount =
      Account(uid = "user-123", username = "testUser", friendUids = listOf("friend1"))
  private val cachedPost =
      OutfitPost(
          postUID = "cached-post",
          ownerId = "friend1",
          timestamp = now,
          description = "Cached outfit",
          itemsID = emptyList(),
          name = "Friend",
          outfitURL = "",
          userProfilePicURL = "")

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)

    feedRepository = mockk(relaxed = true)
    postRepository = mockk(relaxed = true)
    accountRepository = mockk(relaxed = true)
    likesRepository = mockk(relaxed = true)
    firebaseAuth = mockk(relaxed = true)

    // Mock auth to return a user
    val mockUser = mockk<com.google.firebase.auth.FirebaseUser>(relaxed = true)
    every { mockUser.uid } returns "user-123"
    every { firebaseAuth.currentUser } returns mockUser

    // Network calls timeout (return null from withTimeoutOrNull) instead of throwing
    // This simulates the actual offline behavior where withTimeoutOrNull returns null
    coEvery { feedRepository.getRecentFeedForUids(any()) } coAnswers
        {
          kotlinx.coroutines.delay(3000) // Simulate timeout
          emptyList() // This line won't be reached due to withTimeoutOrNull(2000)
        }
    coEvery { feedRepository.getPublicFeed() } coAnswers
        {
          kotlinx.coroutines.delay(3000) // Simulate timeout
          emptyList() // This line won't be reached
        }
    coEvery { feedRepository.hasPostedToday(any()) } coAnswers
        {
          kotlinx.coroutines.delay(3000) // Simulate timeout
          false // This line won't be reached
        }

    // Cached data is available
    coEvery { feedRepository.getCachedFriendFeed(any()) } returns listOf(cachedPost)
    coEvery { feedRepository.getCachedPublicFeed() } returns listOf(cachedPost)

    viewModel =
        FeedViewModel(
            feedRepository, postRepository, accountRepository, likesRepository, firebaseAuth)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun refreshFeed_offline_usesCachedFriendFeed() = runTest {
    viewModel.setCurrentAccount(testAccount)

    viewModel.refreshFeedFromFirestore()
    testDispatcher.scheduler.advanceTimeBy(2100) // Past the 2s timeout
    advanceUntilIdle()

    val state = viewModel.uiState.value

    // Should display cached data
    assertEquals(1, state.feedPosts.size)
    assertEquals("cached-post", state.feedPosts.first().postUID)
    assertFalse(state.isLoading)

    // Verify it tried to get cached data and attempted network call
    coVerify(atLeast = 1) { feedRepository.getCachedFriendFeed(any()) }
    coVerify(atLeast = 1) { feedRepository.getRecentFeedForUids(any()) }
  }

  @Test
  fun refreshFeed_offline_publicFeed_usesCachedPublicFeed() = runTest {
    viewModel.setCurrentAccount(testAccount)

    // toggleFeedType calls refreshFeedFromFirestore internally
    viewModel.toggleFeedType() // Switch to public feed
    testDispatcher.scheduler.advanceTimeBy(2100) // Past the 2s timeout
    advanceUntilIdle()

    val state = viewModel.uiState.value

    // Should display cached public data
    assertEquals(1, state.feedPosts.size)
    assertEquals("cached-post", state.feedPosts.first().postUID)
    assertFalse(state.isLoading)

    coVerify(atLeast = 1) { feedRepository.getCachedPublicFeed() }
    coVerify(atLeast = 1) { feedRepository.getPublicFeed() }
  }

  @Test
  fun refreshFeed_offline_noCachedData_showsEmptyFeed() = runTest {
    // No cached data available
    coEvery { feedRepository.getCachedFriendFeed(any()) } returns emptyList()
    coEvery { feedRepository.getCachedPublicFeed() } returns emptyList()

    viewModel.setCurrentAccount(testAccount)

    // Wait for any initial auth listener processing
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.refreshFeedFromFirestore()
    testDispatcher.scheduler.advanceTimeBy(2100) // Past the 2s timeout
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value

    // Should show empty feed without crashing
    assertEquals(0, state.feedPosts.size)
    assertFalse("isLoading should be false after network timeout", state.isLoading)
  }

  @Test
  fun refreshFeed_offline_hasPostedTodayFallback() = runTest {
    // hasPostedToday network call fails, but cached data contains user's post
    val userPost = cachedPost.copy(postUID = "user-post", ownerId = "user-123", timestamp = now)
    coEvery { feedRepository.getCachedFriendFeed(any()) } returns listOf(cachedPost, userPost)

    viewModel.setCurrentAccount(testAccount)
    viewModel.refreshFeedFromFirestore()
    testDispatcher.scheduler.advanceTimeBy(2100) // Past the 2s timeout
    advanceUntilIdle()

    val state = viewModel.uiState.value

    // Should compute hasPostedToday from cached data
    assertEquals(true, state.hasPostedToday)
    assertEquals(2, state.feedPosts.size)
  }

  @Test
  fun toggleFeedType_offline_switchesBetweenCachedFeeds() = runTest {
    val friendCachedPost = cachedPost.copy(postUID = "friend-cached")
    val publicCachedPost = cachedPost.copy(postUID = "public-cached", isPublic = true)

    coEvery { feedRepository.getCachedFriendFeed(any()) } returns listOf(friendCachedPost)
    coEvery { feedRepository.getCachedPublicFeed() } returns listOf(publicCachedPost)

    viewModel.setCurrentAccount(testAccount)

    // Start with friend feed
    viewModel.refreshFeedFromFirestore()
    testDispatcher.scheduler.advanceTimeBy(2100)
    advanceUntilIdle()
    assertEquals("friend-cached", viewModel.uiState.value.feedPosts.first().postUID)

    // Toggle to public feed (calls refresh internally)
    viewModel.toggleFeedType()
    testDispatcher.scheduler.advanceTimeBy(2100)
    advanceUntilIdle()
    assertEquals("public-cached", viewModel.uiState.value.feedPosts.first().postUID)
  }
}
