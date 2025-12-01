package com.android.ootd.ui.feed

import com.android.ootd.model.account.Account
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.feed.FeedRepository
import com.android.ootd.model.feed.FeedRepositoryProvider
import com.android.ootd.model.posts.OutfitPost
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModelUnitTest {

  private lateinit var viewModel: FeedViewModel
  private lateinit var feedRepository: FeedRepository
  private lateinit var accountRepository: AccountRepository
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    feedRepository = mockk(relaxed = true)
    accountRepository = mockk(relaxed = true)

    // Mock the provider to return our mocked repository
    mockkObject(FeedRepositoryProvider)
    every { FeedRepositoryProvider.repository } returns feedRepository

    viewModel = FeedViewModel(feedRepository, accountRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
  }

  @Test
  fun `toggleFeedType switches isPublicFeed state`() = runTest {
    // Initial state should be false (Friends feed)
    assertFalse(viewModel.uiState.value.isPublicFeed)

    // Toggle to Public
    viewModel.toggleFeedType()
    assertTrue(viewModel.uiState.value.isPublicFeed)

    // Toggle back to Friends
    viewModel.toggleFeedType()
    assertFalse(viewModel.uiState.value.isPublicFeed)
  }

  @Test
  fun `refreshFeedFromFirestore calls getPublicFeed when isPublicFeed is true`() = runTest {
    // Setup
    val publicPosts = listOf(OutfitPost(postUID = "public1", isPublic = true))
    coEvery { feedRepository.getPublicFeed() } returns publicPosts

    // Switch to public feed
    viewModel.toggleFeedType()

    // Trigger refresh
    viewModel.refreshFeedFromFirestore()
    testDispatcher.scheduler.advanceUntilIdle()

    // Verify
    coVerify { feedRepository.getPublicFeed() }
    assertEquals(publicPosts, viewModel.uiState.value.feedPosts)
  }

  @Test
  fun `refreshFeedFromFirestore calls getRecentFeedForUids when isPublicFeed is false`() = runTest {
    // Setup
    val friendPosts = listOf(OutfitPost(postUID = "friend1"))
    val account = Account(uid = "me", friendUids = listOf("friend1"))

    viewModel.refreshFeedFromFirestore()
    testDispatcher.scheduler.advanceUntilIdle()

    // Should NOT call getPublicFeed
    coVerify(exactly = 0) { feedRepository.getPublicFeed() }
    // Should call getRecentFeedForUids (even with empty list if no account)
    coVerify { feedRepository.getRecentFeedForUids(any()) }
  }
}
