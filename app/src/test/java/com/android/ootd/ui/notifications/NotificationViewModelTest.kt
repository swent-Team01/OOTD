package com.android.ootd.ui.notifications

import NotificationRepository
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.notifications.Notification
import com.android.ootd.model.user.User
import com.android.ootd.model.user.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class NotificationsViewModelTest {

  private lateinit var viewModel: NotificationsViewModel
  private lateinit var mockNotificationRepository: NotificationRepository
  private lateinit var mockUserRepository: UserRepository
  private lateinit var mockAccountRepository: AccountRepository
  private lateinit var mockFirebaseAuth: FirebaseAuth
  private lateinit var mockFirebaseUser: FirebaseUser
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)

    mockNotificationRepository = mockk(relaxed = true)
    mockUserRepository = mockk(relaxed = true)
    mockAccountRepository = mockk(relaxed = true)
    mockFirebaseAuth = mockk(relaxed = true)
    mockFirebaseUser = mockk(relaxed = true)

    mockkStatic(FirebaseAuth::class)
    mockkStatic("com.google.firebase.ktx.FirebaseKt")
    mockkStatic("com.google.firebase.auth.ktx.AuthKt")

    every { Firebase.auth } returns mockFirebaseAuth
    every { mockFirebaseAuth.currentUser } returns mockFirebaseUser
    every { mockFirebaseUser.uid } returns "testUserId"
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
  }

  @Test
  fun `loadFollowRequests handles user fetch error gracefully`() = runTest {
    val notification1 =
        Notification(
            uid = "notif1",
            senderId = "sender1",
            receiverId = "user1",
            type = "FOLLOW_REQUEST",
            content = "follows you")
    val notification2 =
        Notification(
            uid = "notif2",
            senderId = "sender2",
            receiverId = "user1",
            type = "FOLLOW_REQUEST",
            content = "follows you")
    val user2 = User(uid = "sender2", username = "user2")

    coEvery { mockNotificationRepository.getNotificationsForReceiver("user1") } returns
        listOf(notification1, notification2)
    coEvery { mockUserRepository.getUser("sender1") } throws Exception("User not found")
    coEvery { mockUserRepository.getUser("sender2") } returns user2

    viewModel =
        NotificationsViewModel(
            mockNotificationRepository,
            mockUserRepository,
            mockAccountRepository,
            overrideUser = true,
            testUserId = "user1")

    advanceUntilIdle()

    val state = viewModel.uiState.value
    // Should only have one follow request (the successful one)
    assertEquals(1, state.followRequests.size)
    assertEquals(notification2, state.followRequests[0].notification)
    assertEquals(user2, state.followRequests[0].senderUser)
    assertFalse(state.isLoading)
    assertNull(state.errorMessage)
  }

  @Test
  fun `loadFollowRequests sets error when user is not authenticated`() = runTest {
    every { mockFirebaseAuth.currentUser } returns null

    viewModel =
        NotificationsViewModel(
            mockNotificationRepository,
            mockUserRepository,
            mockAccountRepository,
            overrideUser = false)

    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("User not authenticated", state.errorMessage)
    assertFalse(state.isLoading)
    assertEquals(emptyList<FollowRequestItem>(), state.followRequests)
  }

  @Test
  fun `loadFollowRequests sets error when repository throws exception`() = runTest {
    coEvery { mockNotificationRepository.getNotificationsForReceiver("user1") } throws
        Exception("Network error")

    viewModel =
        NotificationsViewModel(
            mockNotificationRepository,
            mockUserRepository,
            mockAccountRepository,
            overrideUser = true,
            testUserId = "user1")

    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("Failed to load notifications. Please check your connection.", state.errorMessage)
    assertFalse(state.isLoading)
    assertEquals(emptyList<FollowRequestItem>(), state.followRequests)
  }

  @Test
  fun `acceptFollowRequest handles exception from deleteNotification`() = runTest {
    val notification =
        Notification(
            uid = "notif1",
            senderId = "sender1",
            receiverId = "user1",
            type = "FOLLOW_REQUEST",
            content = "follows you")
    val user = User(uid = "sender1", username = "sender")
    val followRequestItem = FollowRequestItem(notification, user)

    coEvery { mockNotificationRepository.getNotificationsForReceiver("user1") } returns
        listOf(notification)
    coEvery { mockUserRepository.getUser("sender1") } returns user
    coEvery { mockAccountRepository.addFriend("user1", "sender1") } returns true
    coEvery { mockNotificationRepository.deleteNotification(notification) } throws
        Exception("Delete failed")

    viewModel =
        NotificationsViewModel(
            mockNotificationRepository,
            mockUserRepository,
            mockAccountRepository,
            overrideUser = true,
            testUserId = "user1")

    advanceUntilIdle()

    viewModel.acceptFollowRequest(followRequestItem)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("Failed to accept request. Please try again.", state.errorMessage)
    assertEquals(1, state.followRequests.size)
  }

  @Test
  fun `deleteFollowRequest deletes notification and removes from UI`() = runTest {
    val notification =
        Notification(
            uid = "notif1",
            senderId = "sender1",
            receiverId = "user1",
            type = "FOLLOW_REQUEST",
            content = "follows you")
    val user = User(uid = "sender1", username = "sender")
    val followRequestItem = FollowRequestItem(notification, user)

    coEvery { mockNotificationRepository.getNotificationsForReceiver("user1") } returns
        listOf(notification)
    coEvery { mockUserRepository.getUser("sender1") } returns user
    coEvery { mockNotificationRepository.deleteNotification(notification) } returns Unit

    viewModel =
        NotificationsViewModel(
            mockNotificationRepository,
            mockUserRepository,
            mockAccountRepository,
            overrideUser = true,
            testUserId = "user1")

    advanceUntilIdle()

    viewModel.deleteFollowRequest(followRequestItem)
    advanceUntilIdle()

    coVerify { mockNotificationRepository.deleteNotification(notification) }
    coVerify(exactly = 0) { mockAccountRepository.addFriend(any(), any()) }

    val state = viewModel.uiState.value
    assertEquals(0, state.followRequests.size)
    assertNull(state.errorMessage)
  }

  @Test
  fun `deleteFollowRequest handles exception`() = runTest {
    val notification =
        Notification(
            uid = "notif1",
            senderId = "sender1",
            receiverId = "user1",
            type = "FOLLOW_REQUEST",
            content = "follows you")
    val user = User(uid = "sender1", username = "sender")
    val followRequestItem = FollowRequestItem(notification, user)

    coEvery { mockNotificationRepository.getNotificationsForReceiver("user1") } returns
        listOf(notification)
    coEvery { mockUserRepository.getUser("sender1") } returns user
    coEvery { mockNotificationRepository.deleteNotification(notification) } throws
        Exception("Delete failed")

    viewModel =
        NotificationsViewModel(
            mockNotificationRepository,
            mockUserRepository,
            mockAccountRepository,
            overrideUser = true,
            testUserId = "user1")

    advanceUntilIdle()

    viewModel.deleteFollowRequest(followRequestItem)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("Failed to delete request. Please try again.", state.errorMessage)
    assertEquals(1, state.followRequests.size)
  }

  @Test
  fun `clearError clears error message`() = runTest {
    coEvery { mockNotificationRepository.getNotificationsForReceiver("user1") } throws
        Exception("Network error")

    viewModel =
        NotificationsViewModel(
            mockNotificationRepository,
            mockUserRepository,
            mockAccountRepository,
            overrideUser = true,
            testUserId = "user1")

    advanceUntilIdle()

    assertEquals(
        "Failed to load notifications. Please check your connection.",
        viewModel.uiState.value.errorMessage)

    viewModel.clearError()

    assertNull(viewModel.uiState.value.errorMessage)
  }
}
