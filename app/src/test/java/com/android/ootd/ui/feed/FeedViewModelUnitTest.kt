package com.android.ootd.ui.feed

import com.android.ootd.model.account.Account
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.feed.FeedRepository
import com.android.ootd.model.posts.LikesRepository
import com.android.ootd.model.posts.OutfitPost
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
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
  private lateinit var likesRepository: LikesRepository
  private lateinit var firebaseAuth: FirebaseAuth
  private lateinit var firebaseFirestore: FirebaseFirestore
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    feedRepository = mockk(relaxed = true)
    accountRepository = mockk(relaxed = true)
    likesRepository = mockk(relaxed = true)
    firebaseAuth = mockk(relaxed = true)
    firebaseFirestore = mockk(relaxed = true)

    // Mock Firebase Firestore to prevent Provider initialization crash
    mockkStatic(FirebaseFirestore::class)
    every { FirebaseFirestore.getInstance() } returns firebaseFirestore

    viewModel = FeedViewModel(feedRepository, accountRepository, likesRepository, firebaseAuth)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
  }

  @Test
  fun `toggleFeedType switches isPublicFeed state`() = runTest {
    // Set a current account so refreshFeedFromFirestore doesn't return early
    viewModel.setCurrentAccount(Account(uid = "testUser"))

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

    // Set current account
    viewModel.setCurrentAccount(Account(uid = "testUser"))

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

    // Set current account
    viewModel.setCurrentAccount(account)

    viewModel.refreshFeedFromFirestore()
    testDispatcher.scheduler.advanceUntilIdle()

    // Should NOT call getPublicFeed
    coVerify(exactly = 0) { feedRepository.getPublicFeed() }
    // Should call getRecentFeedForUids (even with empty list if no account)
    coVerify { feedRepository.getRecentFeedForUids(any()) }
  }
}
