package com.android.ootd.ui.feed

import com.android.ootd.model.account.Account
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.feed.FeedRepository
import com.android.ootd.model.post.OutfitPostRepository
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
import kotlin.time.Clock.System.now
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
  private lateinit var postRepo: OutfitPostRepository
  private lateinit var likesRepository: LikesRepository
  private lateinit var firebaseAuth: FirebaseAuth
  private lateinit var firebaseFirestore: FirebaseFirestore
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    feedRepository = mockk(relaxed = true)
    postRepo = mockk(relaxed = true)
    accountRepository = mockk(relaxed = true)
    likesRepository = mockk(relaxed = true)
    firebaseAuth = mockk(relaxed = true)
    firebaseFirestore = mockk(relaxed = true)

    // Mock Firebase Firestore to prevent Provider initialization crash
    mockkStatic(FirebaseFirestore::class)
    every { FirebaseFirestore.getInstance() } returns firebaseFirestore

    viewModel =
        FeedViewModel(feedRepository, postRepo, accountRepository, likesRepository, firebaseAuth)
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
    coEvery { feedRepository.hasPostedToday(any()) } returns false
    coEvery { likesRepository.isPostLikedByUser(any(), any()) } returns false
    coEvery { likesRepository.getLikeCount(any()) } returns 0

    // Set current account
    viewModel.setCurrentAccount(Account(uid = "testUser"))

    // Switch to public feed
    viewModel.toggleFeedType()

    // Advance time to allow both initial fetch and background refresh to complete
    testDispatcher.scheduler.advanceTimeBy(2100)
    testDispatcher.scheduler.advanceUntilIdle()

    // Verify getPublicFeed was called (at least once for initial fetch, possibly twice with
    // background refresh)
    coVerify(atLeast = 1) { feedRepository.getPublicFeed() }
    // Verify getRecentFeedForUids was NOT called since we're in public feed mode
    coVerify(exactly = 0) { feedRepository.getRecentFeedForUids(any()) }
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

  @OptIn(kotlin.time.ExperimentalTime::class)
  @Test
  fun `refreshPost updates specific post in feed`() = runTest {
    val now = System.currentTimeMillis()
    // Setup - create initial posts
    val post1 =
        OutfitPost(postUID = "post1", ownerId = "user1", description = "Original", timestamp = now)
    val post2 =
        OutfitPost(postUID = "post2", ownerId = "user2", description = "Another", timestamp = now)

    val account = Account(uid = "user1", friendUids = listOf("user2"))
    viewModel.setCurrentAccount(account)

    // Mock all necessary repository calls for initial refresh
    coEvery { feedRepository.hasPostedToday("user1") } returns true
    coEvery { feedRepository.getRecentFeedForUids(listOf("user2", "user1")) } returns
        listOf(post1, post2)
    coEvery { likesRepository.isPostLikedByUser("post1", "user1") } returns false
    coEvery { likesRepository.getLikeCount("post1") } returns 0
    coEvery { likesRepository.isPostLikedByUser("post2", "user1") } returns false
    coEvery { likesRepository.getLikeCount("post2") } returns 0

    // Initial refresh to populate feed
    viewModel.refreshFeedFromFirestore()
    testDispatcher.scheduler.advanceUntilIdle()

    // Verify initial state
    assertEquals(2, viewModel.uiState.value.feedPosts.size)

    // Setup - mock updated post with comments
    val updatedPost1 =
        post1.copy(
            description = "Updated",
            comments =
                listOf(
                    com.android.ootd.model.posts.Comment(
                        commentId = "c1",
                        ownerId = "user2",
                        text = "Nice!",
                        timestamp = System.currentTimeMillis(),
                        reactionImage = "")))
    coEvery { postRepo.getPostById("post1") } returns updatedPost1

    // Act
    viewModel.refreshPost("post1")
    testDispatcher.scheduler.advanceUntilIdle()

    // Assert
    val state = viewModel.uiState.value
    assertEquals(2, state.feedPosts.size)
    assertEquals("Updated", state.feedPosts.find { it.postUID == "post1" }?.description)
    assertEquals(1, state.feedPosts.find { it.postUID == "post1" }?.comments?.size)

    coVerify { postRepo.getPostById("post1") }
  }

  @Test
  fun `onPullToRefreshTrigger sets isRefreshing state and triggers feed refresh`() = runTest {
    // Setup
    val account = Account(uid = "testUser", friendUids = listOf("friend1"))
    val posts = listOf(OutfitPost(postUID = "post1", ownerId = "friend1"))

    viewModel.setCurrentAccount(account)

    // Mock repository calls
    coEvery { feedRepository.getCachedFriendFeed(any()) } returns emptyList()
    coEvery { feedRepository.getRecentFeedForUids(any()) } returns posts
    coEvery { feedRepository.hasPostedToday(any()) } returns false
    coEvery { likesRepository.isPostLikedByUser(any(), any()) } returns false
    coEvery { likesRepository.getLikeCount(any()) } returns 0

    // Initially isRefreshing should be false
    assertFalse(viewModel.isRefreshing.value)

    // Trigger pull to refresh
    viewModel.onPullToRefreshTrigger()

    // Advance time slightly to let the coroutine start
    testDispatcher.scheduler.advanceTimeBy(100)

    // isRefreshing should be true during the refresh
    assertTrue(viewModel.isRefreshing.value)

    // Advance time to complete the delay and refresh
    testDispatcher.scheduler.advanceTimeBy(2100)
    testDispatcher.scheduler.advanceUntilIdle()

    // isRefreshing should be false after refresh completes
    assertFalse(viewModel.isRefreshing.value)

    // Verify that feed was refreshed
    coVerify { feedRepository.getRecentFeedForUids(any()) }
  }
}
