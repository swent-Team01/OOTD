package com.android.ootd.ui.notifications

import NotificationRepository
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.android.ootd.model.account.AccountRepositoryInMemory
import com.android.ootd.model.notifications.Notification
import com.android.ootd.model.user.UserRepositoryInMemory
import com.android.ootd.ui.notifications.NotificationsScreenTestTags.EMPTY_STATE_TEXT
import com.android.ootd.ui.notifications.NotificationsScreenTestTags.REQUEST_PERMISSION_BUTTON
import com.android.ootd.utils.FirestoreTest
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NotificationsScreenTest : FirestoreTest() {
  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var notificationRepository: NotificationRepository
  private lateinit var userRepositoryInMemory: UserRepositoryInMemory
  private lateinit var accountRepositoryInMemory: AccountRepositoryInMemory

  @Before
  override fun setUp() {
    super.setUp()
    notificationRepository = notificationsRepository
    userRepositoryInMemory = UserRepositoryInMemory()
    accountRepositoryInMemory = AccountRepositoryInMemory()
    // userRepositoryInMemory.addUser(User(uid = currentUser.uid, username="stefanstefan",
    // profilePicture = ""))
    // accountRepositoryInMemory.addAccount(Account(uid = currentUser.uid, ownerId =
    // currentUser.uid, username = "stefanstefan"))
  }

  private fun buildComposeTestRule(
      composeTestRule: ComposeContentTestRule,
      overrideNotificationPopup: Boolean = true
  ) {

    val mockViewModel =
        NotificationsViewModel(
            notificationRepository = notificationRepository,
            userRepository = userRepositoryInMemory,
            accountRepository = accountRepositoryInMemory,
            overrideUser = false,
            testUserId = currentUser.uid,
            overrideNotificationPopup = overrideNotificationPopup)

    composeTestRule.setContent { NotificationsScreen(viewModel = mockViewModel) }

    composeTestRule.waitForIdle()
  }

  @Test
  fun testNoRequestsDisplayed() = runTest {
    buildComposeTestRule(composeTestRule)
    composeTestRule.onNodeWithTag(EMPTY_STATE_TEXT).assertIsDisplayed()
  }

  @Test
  fun testEnableNotificationsDisplayed() = runTest {
    buildComposeTestRule(composeTestRule, overrideNotificationPopup = false)
    composeTestRule.onNodeWithTag(REQUEST_PERMISSION_BUTTON).assertIsDisplayed().performClick()
  }

  @Test
  fun testFollowRequestsDisplayed() = runTest {
    val notification1 =
        Notification(
            uid = "notif1",
            senderId = currentUser.uid,
            receiverId = currentUser.uid,
            type = "FOLLOW_REQUEST",
            content = "wants to follow you")

    val notification2 =
        Notification(
            uid = "notif2",
            senderId = currentUser.uid,
            receiverId = currentUser.uid,
            type = "FOLLOW_REQUEST",
            content = "wants to follow you")

    notificationRepository.addNotification(notification1)
    notificationRepository.addNotification(notification2)

    buildComposeTestRule(composeTestRule)

    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(NotificationsScreenTestTags.NOTIFICATION_ITEM)
          .fetchSemanticsNodes()
          .size == 2
    }
  }

  @Test
  fun testAcceptAndDeleteNotification() = runTest {
    val notification1 =
        Notification(
            uid = "notif1",
            senderId = currentUser.uid,
            receiverId = currentUser.uid,
            type = "FOLLOW_REQUEST",
            content = "wants to follow you")

    val notification2 =
        Notification(
            uid = "notif2",
            senderId = currentUser.uid,
            receiverId = currentUser.uid,
            type = "FOLLOW_REQUEST",
            content = "wants to follow you")

    notificationRepository.addNotification(notification1)
    notificationRepository.addNotification(notification2)

    buildComposeTestRule(composeTestRule)

    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(NotificationsScreenTestTags.NOTIFICATION_ITEM)
          .fetchSemanticsNodes()
          .size == 2
    }

    composeTestRule.onAllNodesWithTag(NotificationsScreenTestTags.ACCEPT_BUTTON)[0].performClick()
    composeTestRule.onAllNodesWithTag(NotificationsScreenTestTags.DELETE_BUTTON)[0].performClick()
  }
}
