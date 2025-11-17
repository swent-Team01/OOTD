package com.android.ootd.ui.account

import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.authentication.AccountService
import com.android.ootd.model.feed.FeedRepository
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.model.user.User
import com.android.ootd.model.user.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ViewUserViewModelTest {

  private lateinit var viewModel: ViewUserViewModel
  private lateinit var mockAccountService: AccountService
  private lateinit var mockUserRepository: UserRepository
  private lateinit var mockAccountRepository: AccountRepository
  private lateinit var mockFeedRepository: FeedRepository
  private val testDispatcher = StandardTestDispatcher()

  private val testCurrentUserId = "currentUserId"
  private val testFriendId = "friendId"
  private val testUser =
      User(uid = testFriendId, username = "testuser", profilePicture = "http://example.com/pic.jpg")
  private val testPost1 =
      OutfitPost(
          postUID = "post1", ownerId = testFriendId, outfitURL = "http://example.com/outfit1.jpg")
  private val testPost2 =
      OutfitPost(
          postUID = "post2", ownerId = testFriendId, outfitURL = "http://example.com/outfit2.jpg")

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)

    mockAccountService = mockk(relaxed = true)
    mockUserRepository = mockk(relaxed = true)
    mockAccountRepository = mockk(relaxed = true)
    mockFeedRepository = mockk(relaxed = true)

    coEvery { mockAccountService.currentUserId } returns testCurrentUserId
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `initial state is loading with empty data`() = runTest {
    viewModel =
        ViewUserViewModel(
            mockAccountService, mockUserRepository, mockAccountRepository, mockFeedRepository)

    val state = viewModel.uiState.value
    assertTrue(state.isLoading)
    assertEquals("", state.username)
    assertEquals("", state.profilePicture)
    assertFalse(state.isFriend)
    assertEquals(emptyList<OutfitPost>(), state.friendPosts)
    assertEquals(0, state.friendCount)
    assertFalse(state.error)
    assertNull(state.errorMsg)
  }

  @Test
  fun `update with blank friendId does not trigger refresh`() = runTest {
    viewModel =
        ViewUserViewModel(
            mockAccountService, mockUserRepository, mockAccountRepository, mockFeedRepository)

    viewModel.update("")
    advanceUntilIdle()

    coVerify(exactly = 0) { mockUserRepository.getUser(any()) }
    coVerify(exactly = 0) { mockAccountRepository.isMyFriend(any(), any()) }
  }

  @Test
  fun `update with valid friendId loads user data when friend`() = runTest {
    coEvery { mockAccountRepository.isMyFriend(testCurrentUserId, testFriendId) } returns true
    coEvery { mockUserRepository.getUser(testFriendId) } returns testUser
    coEvery { mockFeedRepository.getFeedForUids(listOf(testFriendId)) } returns
        listOf(testPost1, testPost2)

    viewModel =
        ViewUserViewModel(
            mockAccountService, mockUserRepository, mockAccountRepository, mockFeedRepository)
    viewModel.update(testFriendId)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertEquals("testuser", state.username)
    assertEquals("http://example.com/pic.jpg", state.profilePicture)
    assertTrue(state.isFriend)
    assertEquals(2, state.friendPosts.size)
    assertEquals(testPost1, state.friendPosts[0])
    assertEquals(testPost2, state.friendPosts[1])
    assertFalse(state.error)
    assertNull(state.errorMsg)

    coVerify { mockAccountRepository.isMyFriend(testCurrentUserId, testFriendId) }
    coVerify { mockUserRepository.getUser(testFriendId) }
    coVerify { mockFeedRepository.getFeedForUids(listOf(testFriendId)) }
  }

  @Test
  fun `update with valid friendId loads user data when not friend`() = runTest {
    coEvery { mockAccountRepository.isMyFriend(testCurrentUserId, testFriendId) } returns false
    coEvery { mockUserRepository.getUser(testFriendId) } returns testUser

    viewModel =
        ViewUserViewModel(
            mockAccountService, mockUserRepository, mockAccountRepository, mockFeedRepository)
    viewModel.update(testFriendId)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertEquals("testuser", state.username)
    assertEquals("http://example.com/pic.jpg", state.profilePicture)
    assertFalse(state.isFriend)
    assertEquals(emptyList<OutfitPost>(), state.friendPosts)
    assertFalse(state.error)
    assertNull(state.errorMsg)

    coVerify { mockAccountRepository.isMyFriend(testCurrentUserId, testFriendId) }
    coVerify { mockUserRepository.getUser(testFriendId) }
    coVerify(exactly = 0) { mockFeedRepository.getFeedForUids(any()) }
  }

  @Test
  fun `update handles exception from userRepository`() = runTest {
    coEvery { mockAccountRepository.isMyFriend(testCurrentUserId, testFriendId) } returns true
    coEvery { mockUserRepository.getUser(testFriendId) } throws Exception("User not found")

    viewModel =
        ViewUserViewModel(
            mockAccountService, mockUserRepository, mockAccountRepository, mockFeedRepository)
    viewModel.update(testFriendId)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertTrue(state.error)
    assertEquals("User not found", state.errorMsg)
    assertEquals("", state.username)
    assertEquals("", state.profilePicture)

    coVerify { mockUserRepository.getUser(testFriendId) }
  }

  @Test
  fun `update handles exception from accountRepository isMyFriend`() = runTest {
    coEvery { mockAccountRepository.isMyFriend(testCurrentUserId, testFriendId) } throws
        Exception("Network error")

    viewModel =
        ViewUserViewModel(
            mockAccountService, mockUserRepository, mockAccountRepository, mockFeedRepository)
    viewModel.update(testFriendId)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertTrue(state.error)
    assertEquals("Network error", state.errorMsg)

    coVerify { mockAccountRepository.isMyFriend(testCurrentUserId, testFriendId) }
  }

  @Test
  fun `update handles exception from feedRepository`() = runTest {
    coEvery { mockAccountRepository.isMyFriend(testCurrentUserId, testFriendId) } returns true
    coEvery { mockUserRepository.getUser(testFriendId) } returns testUser
    coEvery { mockFeedRepository.getFeedForUids(listOf(testFriendId)) } throws
        Exception("Feed error")

    viewModel =
        ViewUserViewModel(
            mockAccountService, mockUserRepository, mockAccountRepository, mockFeedRepository)
    viewModel.update(testFriendId)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertTrue(state.error)
    assertEquals("Feed error", state.errorMsg)

    coVerify { mockFeedRepository.getFeedForUids(listOf(testFriendId)) }
  }

  @Test
  fun `postsToShow returns empty list when not friend`() = runTest {
    coEvery { mockAccountRepository.isMyFriend(testCurrentUserId, testFriendId) } returns false
    coEvery { mockUserRepository.getUser(testFriendId) } returns testUser

    viewModel =
        ViewUserViewModel(
            mockAccountService, mockUserRepository, mockAccountRepository, mockFeedRepository)
    viewModel.update(testFriendId)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(emptyList<OutfitPost>(), state.friendPosts)

    coVerify(exactly = 0) { mockFeedRepository.getFeedForUids(any()) }
  }

  @Test
  fun `postsToShow returns feed posts when friend`() = runTest {
    val posts = listOf(testPost1, testPost2)
    coEvery { mockAccountRepository.isMyFriend(testCurrentUserId, testFriendId) } returns true
    coEvery { mockUserRepository.getUser(testFriendId) } returns testUser
    coEvery { mockFeedRepository.getFeedForUids(listOf(testFriendId)) } returns posts

    viewModel =
        ViewUserViewModel(
            mockAccountService, mockUserRepository, mockAccountRepository, mockFeedRepository)
    viewModel.update(testFriendId)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(posts, state.friendPosts)

    coVerify { mockFeedRepository.getFeedForUids(listOf(testFriendId)) }
  }

  @Test
  fun `isFriend updates state correctly when true`() = runTest {
    coEvery { mockAccountRepository.isMyFriend(testCurrentUserId, testFriendId) } returns true
    coEvery { mockUserRepository.getUser(testFriendId) } returns testUser
    coEvery { mockFeedRepository.getFeedForUids(any()) } returns emptyList()

    viewModel =
        ViewUserViewModel(
            mockAccountService, mockUserRepository, mockAccountRepository, mockFeedRepository)
    viewModel.update(testFriendId)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertTrue(state.isFriend)
  }

  @Test
  fun `isFriend updates state correctly when false`() = runTest {
    coEvery { mockAccountRepository.isMyFriend(testCurrentUserId, testFriendId) } returns false
    coEvery { mockUserRepository.getUser(testFriendId) } returns testUser

    viewModel =
        ViewUserViewModel(
            mockAccountService, mockUserRepository, mockAccountRepository, mockFeedRepository)
    viewModel.update(testFriendId)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isFriend)
  }

  @Test
  fun `user with no profile picture loads successfully`() = runTest {
    val userNoProfilePic = User(uid = testFriendId, username = "testuser", profilePicture = "")
    coEvery { mockAccountRepository.isMyFriend(testCurrentUserId, testFriendId) } returns false
    coEvery { mockUserRepository.getUser(testFriendId) } returns userNoProfilePic

    viewModel =
        ViewUserViewModel(
            mockAccountService, mockUserRepository, mockAccountRepository, mockFeedRepository)
    viewModel.update(testFriendId)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertEquals("testuser", state.username)
    assertEquals("", state.profilePicture)
    assertFalse(state.error)
  }

  @Test
  fun `multiple updates work correctly`() = runTest {
    val friend1 = User(uid = "friend1", username = "user1", profilePicture = "pic1")
    val friend2 = User(uid = "friend2", username = "user2", profilePicture = "pic2")

    coEvery { mockAccountRepository.isMyFriend(testCurrentUserId, "friend1") } returns true
    coEvery { mockUserRepository.getUser("friend1") } returns friend1
    coEvery { mockFeedRepository.getFeedForUids(listOf("friend1")) } returns listOf(testPost1)

    coEvery { mockAccountRepository.isMyFriend(testCurrentUserId, "friend2") } returns false
    coEvery { mockUserRepository.getUser("friend2") } returns friend2

    viewModel =
        ViewUserViewModel(
            mockAccountService, mockUserRepository, mockAccountRepository, mockFeedRepository)

    viewModel.update("friend1")
    advanceUntilIdle()

    var state = viewModel.uiState.value
    assertEquals("user1", state.username)
    assertEquals("pic1", state.profilePicture)
    assertTrue(state.isFriend)
    assertEquals(1, state.friendPosts.size)

    viewModel.update("friend2")
    advanceUntilIdle()

    state = viewModel.uiState.value
    assertEquals("user2", state.username)
    assertEquals("pic2", state.profilePicture)
    assertFalse(state.isFriend)
    assertEquals(0, state.friendPosts.size)
  }
}
