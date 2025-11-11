package com.android.ootd.ui.account

import com.android.ootd.model.account.Account
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.authentication.AccountService
import com.android.ootd.model.feed.FeedRepository
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.model.user.User
import com.android.ootd.model.user.UserRepository
import io.mockk.clearAllMocks
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountPageViewModelTest {

  private lateinit var mockAccountService: AccountService
  private lateinit var mockAccountRepository: AccountRepository
  private lateinit var mockUserRepository: UserRepository
  private lateinit var mockFeedRepository: FeedRepository
  private lateinit var viewModel: AccountPageViewModel

  private val testDispatcher = StandardTestDispatcher()

  private val testUser =
      User(
          uid = "test-uid",
          username = "testuser",
          profilePicture = "https://example.com/profile.jpg")

  private val testAccount =
      Account(
          uid = "test-uid",
          ownerId = "test-uid",
          username = "testuser",
          profilePicture = "https://example.com/profile.jpg",
          friendUids = listOf("friend1", "friend2", "friend3"),
          isPrivate = false)

  private val testPosts =
      listOf(
          OutfitPost(
              postUID = "post1",
              name = "testuser",
              ownerId = "test-uid",
              outfitURL = "https://example.com/post1.jpg",
              description = "First post"),
          OutfitPost(
              postUID = "post2",
              name = "testuser",
              ownerId = "test-uid",
              outfitURL = "https://example.com/post2.jpg",
              description = "Second post"))

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)

    mockAccountService = mockk(relaxed = true)
    mockAccountRepository = mockk(relaxed = true)
    mockUserRepository = mockk(relaxed = true)
    mockFeedRepository = mockk(relaxed = true)

    every { mockAccountService.currentUserId } returns "test-uid"
    coEvery { mockUserRepository.getUser("test-uid") } returns testUser
    coEvery { mockAccountRepository.getAccount("test-uid") } returns testAccount
    coEvery { mockFeedRepository.getFeedForUids(listOf("test-uid")) } returns testPosts
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    clearAllMocks()
  }

  @Test
  fun uiState_initializes_with_default_values() {
    viewModel =
        AccountPageViewModel(
            mockAccountService, mockAccountRepository, mockUserRepository, mockFeedRepository)

    val state = viewModel.uiState.value
    // Initial state should be default before data loads
    assertTrue(state.username.isEmpty() || state.username == "testuser")
  }

  @Test
  fun init_loads_user_data_successfully() = runTest {
    viewModel =
        AccountPageViewModel(
            mockAccountService, mockAccountRepository, mockUserRepository, mockFeedRepository)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("testuser", state.username)
    assertEquals("https://example.com/profile.jpg", state.profilePicture)
    assertEquals(2, state.posts.size)
    assertEquals(3, state.friends.size)
    assertFalse(state.isLoading)
    assertNull(state.errorMsg)
  }

  @Test
  fun retrieveUserData_calls_all_repositories() = runTest {
    viewModel =
        AccountPageViewModel(
            mockAccountService, mockAccountRepository, mockUserRepository, mockFeedRepository)
    advanceUntilIdle()

    coVerify(exactly = 1) { mockUserRepository.getUser("test-uid") }
    coVerify(exactly = 1) { mockAccountRepository.getAccount("test-uid") }
    coVerify(exactly = 1) { mockFeedRepository.getFeedForUids(listOf("test-uid")) }
  }

  @Test
  fun uiState_sets_loading_true_initially() = runTest {
    coEvery { mockUserRepository.getUser("test-uid") } coAnswers
        {
          kotlinx.coroutines.delay(100)
          testUser
        }

    viewModel =
        AccountPageViewModel(
            mockAccountService, mockAccountRepository, mockUserRepository, mockFeedRepository)

    // Should be loading before data arrives
    assertTrue(viewModel.uiState.value.isLoading)
  }

  @Test
  fun uiState_sets_loading_false_after_data_loads() = runTest {
    viewModel =
        AccountPageViewModel(
            mockAccountService, mockAccountRepository, mockUserRepository, mockFeedRepository)
    advanceUntilIdle()

    assertFalse(viewModel.uiState.value.isLoading)
  }

  @Test
  fun retrieveUserData_handles_user_repository_error() = runTest {
    val errorMessage = "Failed to load account data"
    coEvery { mockUserRepository.getUser("test-uid") } throws Exception(errorMessage)

    viewModel =
        AccountPageViewModel(
            mockAccountService, mockAccountRepository, mockUserRepository, mockFeedRepository)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertNotNull(state.errorMsg)
    assertEquals(errorMessage, state.errorMsg)
    assertFalse(state.isLoading)
  }

  @Test
  fun retrieveUserData_handles_account_repository_error() = runTest {
    val errorMessage = "Failed to load account data"
    coEvery { mockAccountRepository.getAccount("test-uid") } throws Exception(errorMessage)

    viewModel =
        AccountPageViewModel(
            mockAccountService, mockAccountRepository, mockUserRepository, mockFeedRepository)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertNotNull(state.errorMsg)
    assertEquals(errorMessage, state.errorMsg)
    assertFalse(state.isLoading)
  }

  @Test
  fun retrieveUserData_handles_feed_repository_error() = runTest {
    val errorMessage = "Failed to load account data"
    coEvery { mockFeedRepository.getFeedForUids(listOf("test-uid")) } throws Exception(errorMessage)

    viewModel =
        AccountPageViewModel(
            mockAccountService, mockAccountRepository, mockUserRepository, mockFeedRepository)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertNotNull(state.errorMsg)
    assertEquals(errorMessage, state.errorMsg)
    assertFalse(state.isLoading)
  }

  @Test
  fun retrieveUserData_handles_generic_exception() = runTest {
    coEvery { mockUserRepository.getUser("test-uid") } throws RuntimeException()

    viewModel =
        AccountPageViewModel(
            mockAccountService, mockAccountRepository, mockUserRepository, mockFeedRepository)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertNotNull(state.errorMsg)
    assertEquals("Failed to load account data", state.errorMsg)
  }

  @Test
  fun uiState_contains_correct_user_information() = runTest {
    viewModel =
        AccountPageViewModel(
            mockAccountService, mockAccountRepository, mockUserRepository, mockFeedRepository)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("testuser", state.username)
    assertEquals("https://example.com/profile.jpg", state.profilePicture)
  }

  @Test
  fun uiState_contains_correct_posts() = runTest {
    viewModel =
        AccountPageViewModel(
            mockAccountService, mockAccountRepository, mockUserRepository, mockFeedRepository)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(2, state.posts.size)
    assertEquals("post1", state.posts[0].postUID)
    assertEquals("post2", state.posts[1].postUID)
  }

  @Test
  fun uiState_contains_correct_friends_list() = runTest {
    viewModel =
        AccountPageViewModel(
            mockAccountService, mockAccountRepository, mockUserRepository, mockFeedRepository)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(3, state.friends.size)
    assertEquals(listOf("friend1", "friend2", "friend3"), state.friends)
  }

  @Test
  fun clearErrorMsg_clears_error_message() = runTest {
    coEvery { mockUserRepository.getUser("test-uid") } throws Exception("Test error")

    viewModel =
        AccountPageViewModel(
            mockAccountService, mockAccountRepository, mockUserRepository, mockFeedRepository)
    advanceUntilIdle()

    assertNotNull(viewModel.uiState.value.errorMsg)

    viewModel.clearErrorMsg()

    assertNull(viewModel.uiState.value.errorMsg)
  }

  @Test
  fun uiState_handles_empty_posts_list() = runTest {
    coEvery { mockFeedRepository.getFeedForUids(listOf("test-uid")) } returns emptyList()

    viewModel =
        AccountPageViewModel(
            mockAccountService, mockAccountRepository, mockUserRepository, mockFeedRepository)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertTrue(state.posts.isEmpty())
    assertNull(state.errorMsg)
  }

  @Test
  fun uiState_handles_empty_friends_list() = runTest {
    coEvery { mockAccountRepository.getAccount("test-uid") } returns
        testAccount.copy(friendUids = emptyList())

    viewModel =
        AccountPageViewModel(
            mockAccountService, mockAccountRepository, mockUserRepository, mockFeedRepository)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertTrue(state.friends.isEmpty())
    assertNull(state.errorMsg)
  }

  @Test
  fun uiState_handles_user_without_profile_picture() = runTest {
    coEvery { mockUserRepository.getUser("test-uid") } returns testUser.copy(profilePicture = "")

    viewModel =
        AccountPageViewModel(
            mockAccountService, mockAccountRepository, mockUserRepository, mockFeedRepository)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("", state.profilePicture)
    assertNull(state.errorMsg)
  }

  @Test
  fun uiState_maintains_data_integrity_across_multiple_fields() = runTest {
    viewModel =
        AccountPageViewModel(
            mockAccountService, mockAccountRepository, mockUserRepository, mockFeedRepository)
    advanceUntilIdle()

    val state = viewModel.uiState.value

    // Verify all fields are correctly populated
    assertEquals("testuser", state.username)
    assertEquals("https://example.com/profile.jpg", state.profilePicture)
    assertEquals(2, state.posts.size)
    assertEquals(3, state.friends.size)
    assertFalse(state.isLoading)
    assertNull(state.errorMsg)

    // Verify post details
    assertEquals("post1", state.posts[0].postUID)
    assertEquals("https://example.com/post1.jpg", state.posts[0].outfitURL)
    assertEquals("First post", state.posts[0].description)
  }

  @Test
  fun retrieveUserData_uses_correct_user_id() = runTest {
    every { mockAccountService.currentUserId } returns "specific-user-id"
    coEvery { mockUserRepository.getUser("specific-user-id") } returns testUser
    coEvery { mockAccountRepository.getAccount("specific-user-id") } returns testAccount
    coEvery { mockFeedRepository.getFeedForUids(listOf("specific-user-id")) } returns testPosts

    viewModel =
        AccountPageViewModel(
            mockAccountService, mockAccountRepository, mockUserRepository, mockFeedRepository)
    advanceUntilIdle()

    coVerify(exactly = 1) { mockUserRepository.getUser("specific-user-id") }
    coVerify(exactly = 1) { mockAccountRepository.getAccount("specific-user-id") }
    coVerify(exactly = 1) { mockFeedRepository.getFeedForUids(listOf("specific-user-id")) }
  }

  @Test
  fun uiState_handles_large_number_of_posts() = runTest {
    val manyPosts =
        (1..50).map { index ->
          OutfitPost(
              postUID = "post$index",
              name = "testuser",
              ownerId = "test-uid",
              outfitURL = "https://example.com/post$index.jpg",
              description = "Post $index")
        }

    coEvery { mockFeedRepository.getFeedForUids(listOf("test-uid")) } returns manyPosts

    viewModel =
        AccountPageViewModel(
            mockAccountService, mockAccountRepository, mockUserRepository, mockFeedRepository)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(50, state.posts.size)
    assertNull(state.errorMsg)
  }

  @Test
  fun uiState_handles_large_number_of_friends() = runTest {
    val manyFriends = (1..100).map { "friend$it" }
    coEvery { mockAccountRepository.getAccount("test-uid") } returns
        testAccount.copy(friendUids = manyFriends)

    viewModel =
        AccountPageViewModel(
            mockAccountService, mockAccountRepository, mockUserRepository, mockFeedRepository)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(100, state.friends.size)
    assertNull(state.errorMsg)
  }

  @Test
  fun clearErrorMsg_does_not_affect_other_state_fields() = runTest {
    coEvery { mockUserRepository.getUser("test-uid") } throws Exception("Error")

    viewModel =
        AccountPageViewModel(
            mockAccountService, mockAccountRepository, mockUserRepository, mockFeedRepository)
    advanceUntilIdle()

    val stateBeforeClear = viewModel.uiState.value
    assertNotNull(stateBeforeClear.errorMsg)

    viewModel.clearErrorMsg()

    val stateAfterClear = viewModel.uiState.value
    assertNull(stateAfterClear.errorMsg)
    assertEquals(stateBeforeClear.isLoading, stateAfterClear.isLoading)
  }
}
