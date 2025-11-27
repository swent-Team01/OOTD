package com.android.ootd.ui.post

import com.android.ootd.model.authentication.AccountService
import com.android.ootd.model.post.OutfitPostRepository
import com.android.ootd.model.posts.LikesRepository
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.model.user.User
import com.android.ootd.model.user.UserRepository
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PostViewViewModelTest {

  private lateinit var mockPostRepository: OutfitPostRepository
  private lateinit var mockUserRepo: UserRepository
  private lateinit var mockLikesRepo: LikesRepository
  private lateinit var mockAccountService: AccountService
  private lateinit var viewModel: PostViewViewModel

  private val testDispatcher = StandardTestDispatcher()

  private val testPost =
      OutfitPost(
          postUID = "test-post-id",
          name = "Test User",
          ownerId = "test-owner-id",
          userProfilePicURL = "https://example.com/profile.jpg",
          outfitURL = "https://example.com/outfit.jpg",
          description = "Test outfit description",
          itemsID = listOf("item1", "item2"),
          timestamp = 1700000000000L)

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockPostRepository = mockk(relaxed = true)
    mockUserRepo = mockk(relaxed = true)
    mockLikesRepo = mockk(relaxed = true)
    mockAccountService = mockk(relaxed = true)

    every { mockAccountService.currentUserId } returns "currentUser"
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    clearAllMocks()
    unmockkAll()
  }

  @Test
  fun uiState_initializes_with_default_values() {
    viewModel =
        PostViewViewModel("", mockPostRepository, mockUserRepo, mockLikesRepo, mockAccountService)

    val state = viewModel.uiState.value
    assertNull(state.post)
    assertFalse(state.isLoading)
    assertNull(state.error)
    assertTrue(state.likedByUsers.isEmpty())
  }

  @Test
  fun loadPost_updates_uiState_with_loading_true() = runTest {
    coEvery { mockPostRepository.getPostById("test-post-id") } coAnswers
        {
          kotlinx.coroutines.delay(100)
          testPost
        }

    viewModel = PostViewViewModel("", mockPostRepository)
    viewModel.loadPost("test-post-id")

    // Check loading state before completion
    assertTrue(viewModel.uiState.value.isLoading)
  }

  @Test
  fun loadPost_successfully_loads_post() = runTest {
    // Arrange: post from post repository
    coEvery { mockPostRepository.getPostById("test-post-id") } returns testPost

    // Arrange: owner user from user repository
    val ownerUser =
        User(
            uid = "test-owner-id",
            username = "Test User",
            profilePicture = "https://example.com/profile.jpg")
    coEvery { mockUserRepo.getUser("test-owner-id") } returns ownerUser

    // Arrange: no likes for this post
    coEvery { mockLikesRepo.getLikesForPost("test-post-id") } returns emptyList()

    // Act
    viewModel =
        PostViewViewModel(
            "",
            postRepository = mockPostRepository,
            userRepository = mockUserRepo,
            likesRepository = mockLikesRepo)

    viewModel.loadPost("test-post-id")
    advanceUntilIdle()

    // Assert
    val state = viewModel.uiState.value
    assertNotNull(state.post)
    assertEquals("test-post-id", state.post?.postUID)
    assertEquals("Test User", state.post?.name)
    assertEquals("https://example.com/outfit.jpg", state.post?.outfitURL)
    assertEquals("Test outfit description", state.post?.description)

    // New fields from updated ViewModel:
    assertEquals("Test User", state.ownerUsername)
    assertEquals("https://example.com/profile.jpg", state.ownerProfilePicture)
    assertTrue(state.likedByUsers.isEmpty())
    assertFalse(state.isLikedByCurrentUser)

    assertFalse(state.isLoading)
    assertNull(state.error)
  }

  @Test
  fun loadPost_handles_null_post_from_repository() = runTest {
    // Mock post returns null
    coEvery { mockPostRepository.getPostById("non-existent-id") } returns null

    viewModel =
        PostViewViewModel(
            "",
            postRepository = mockPostRepository,
            userRepository = mockUserRepo,
            likesRepository = mockLikesRepo,
            accountService = mockAccountService)

    viewModel.loadPost("non-existent-id")
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertNull(state.post)
    assertFalse(state.isLoading)
    assertEquals("Post View: Post not found", state.error)
  }

  @Test
  fun loadPost_handles_repository_exception() = runTest {
    val errorMessage = "Failed to load post"
    coEvery { mockPostRepository.getPostById("test-post-id") } throws Exception(errorMessage)

    viewModel = PostViewViewModel("", mockPostRepository)
    viewModel.loadPost("test-post-id")
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertNull(state.post)
    assertFalse(state.isLoading)
    assertEquals(errorMessage, state.error)
  }

  @Test
  fun init_does_not_load_post_when_postId_is_empty() = runTest {
    viewModel = PostViewViewModel("", mockPostRepository)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertNull(state.post)
    assertFalse(state.isLoading)
    assertNull(state.error)

    // Verify repository was never called
    coVerify(exactly = 0) { mockPostRepository.getPostById(any()) }
  }

  @Test
  fun loadPost_clears_previous_error_on_new_load() = runTest {
    // First load fails
    coEvery { mockPostRepository.getPostById("test-post-id") } throws
        Exception("Failed to load post")

    viewModel =
        PostViewViewModel(
            "",
            postRepository = mockPostRepository,
            userRepository = mockUserRepo,
            likesRepository = mockLikesRepo)

    viewModel.loadPost("test-post-id")
    advanceUntilIdle()

    assertEquals("Failed to load post", viewModel.uiState.value.error)

    // Second load succeeds: now we must also mock user + likes
    coEvery { mockPostRepository.getPostById("test-post-id") } returns testPost

    // Owner user for the post
    val ownerUser =
        User(
            uid = "test-owner-id",
            username = "Test User",
            profilePicture = "https://example.com/profile.jpg")
    coEvery { mockUserRepo.getUser("test-owner-id") } returns ownerUser

    // No likes
    coEvery { mockLikesRepo.getLikesForPost("test-post-id") } returns emptyList()

    // Trigger second load
    viewModel.loadPost("test-post-id")
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertNull(state.error)
    assertNotNull(state.post)
    assertEquals("test-post-id", state.post?.postUID)

    // (Optional) also verify new fields are sane
    assertEquals("Test User", state.ownerUsername)
    assertEquals("https://example.com/profile.jpg", state.ownerProfilePicture)
  }

  @Test
  fun loadPost_multiple_times_updates_state_correctly() = runTest {
    val post1 = testPost.copy(postUID = "post-1", description = "First post")
    val post2 = testPost.copy(postUID = "post-2", description = "Second post")

    // Mock post repository
    coEvery { mockPostRepository.getPostById("post-1") } returns post1
    coEvery { mockPostRepository.getPostById("post-2") } returns post2

    // Mock owner user (same owner for both posts)
    val ownerUser = User(uid = "test-owner-id", username = "Owner", profilePicture = "pfp.jpg")
    coEvery { mockUserRepo.getUser("test-owner-id") } returns ownerUser

    // Mock likes
    coEvery { mockLikesRepo.getLikesForPost(any()) } returns emptyList()

    viewModel =
        PostViewViewModel(
            "",
            postRepository = mockPostRepository,
            userRepository = mockUserRepo,
            likesRepository = mockLikesRepo)

    // Load first post
    viewModel.loadPost("post-1")
    advanceUntilIdle()
    assertEquals("post-1", viewModel.uiState.value.post?.postUID)
    assertEquals("First post", viewModel.uiState.value.post?.description)

    // Load second post
    viewModel.loadPost("post-2")
    advanceUntilIdle()
    assertEquals("post-2", viewModel.uiState.value.post?.postUID)
    assertEquals("Second post", viewModel.uiState.value.post?.description)
  }

  @Test
  fun uiState_maintains_post_data_integrity() = runTest {
    coEvery { mockPostRepository.getPostById("test-post-id") } returns testPost
    val ownerUser =
        User(
            uid = "test-owner-id",
            username = "Test User",
            profilePicture = "https://example.com/profile.jpg")
    coEvery { mockUserRepo.getUser("test-owner-id") } returns ownerUser
    coEvery { mockLikesRepo.getLikesForPost("test-post-id") } returns emptyList()

    viewModel =
        PostViewViewModel(
            postId = "test-post-id",
            postRepository = mockPostRepository,
            userRepository = mockUserRepo,
            likesRepository = mockLikesRepo)

    advanceUntilIdle()

    val state = viewModel.uiState.value
    val post = state.post
    assertNotNull(post)
    assertEquals("test-post-id", post?.postUID)
    assertEquals("Test User", post?.name)
    assertEquals("test-owner-id", post?.ownerId)
    assertEquals("https://example.com/profile.jpg", post?.userProfilePicURL)
    assertEquals("https://example.com/outfit.jpg", post?.outfitURL)
    assertEquals("Test outfit description", post?.description)
    assertEquals(listOf("item1", "item2"), post?.itemsID)
    assertEquals(1700000000000L, post?.timestamp)

    // Sanity check new fields
    assertEquals("Test User", state.ownerUsername)
    assertEquals("https://example.com/profile.jpg", state.ownerProfilePicture)
    assertTrue(state.likedByUsers.isEmpty())
    assertFalse(state.isLikedByCurrentUser)
  }
}
