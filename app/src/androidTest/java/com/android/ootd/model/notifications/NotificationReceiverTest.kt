package com.android.ootd.model.notifications

import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import com.android.ootd.model.account.Account
import com.android.ootd.model.user.User
import com.android.ootd.utils.FirestoreTest
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationActionReceiverInstrumentedTest : FirestoreTest() {
  private lateinit var mockManager: NotificationManagerCompat
  private lateinit var receiver: NotificationActionReceiver
  private lateinit var mockContext: Context
  private val testDispatcher = StandardTestDispatcher()

  @Before
  override fun setUp() = runBlocking {
    super.setUp()
    Dispatchers.setMain(testDispatcher)

    accountRepository.addAccount(
        Account(uid = currentUser.uid, ownerId = currentUser.uid, username = "stefanstefan"))
    userRepository.addUser(
        User(
            uid = currentUser.uid,
            ownerId = currentUser.uid,
            username = "stefanstefan",
            profilePicture = ""))

    mockContext = ApplicationProvider.getApplicationContext()
    mockkStatic(NotificationManagerCompat::class)
    mockManager = mockk(relaxed = true)
    every { NotificationManagerCompat.from(any()) } returns mockManager

    receiver = NotificationActionReceiver()
  }

  @After
  override fun tearDown() {
    Dispatchers.resetMain()
    super.tearDown()
    unmockkAll()
  }

  @Test
  fun testAcceptAction() = runTest {
    val notif =
        Notification(
            uid = "notif123",
            senderId = currentUser.uid,
            receiverId = currentUser.uid,
            type = "FOLLOW_REQUEST",
            content = "hello",
            senderName = "")

    notificationsRepository.addNotification(notif)

    val intent =
        Intent().apply {
          action = NOTIFICATION_ACTION_ACCEPT
          putExtra("notificationUid", notif.uid)
          putExtra("senderId", notif.senderId)
          putExtra("receiverId", notif.receiverId)
        }

    receiver.onReceive(mockContext, intent)

    // Advance time to allow coroutines to complete
    advanceUntilIdle()

    val result = notificationsRepository.getNotificationsForReceiver(currentUser.uid)
    assert(result.isEmpty())

    verify { mockManager.cancel(notif.uid.hashCode()) }
  }

  @Test
  fun testDeleteAction() = runTest {
    val notif =
        Notification(
            uid = "notif123",
            senderId = currentUser.uid,
            receiverId = currentUser.uid,
            type = "FOLLOW_REQUEST",
            content = "hello",
            senderName = "")

    notificationsRepository.addNotification(notif)

    val intent =
        Intent().apply {
          action = NOTIFICATION_ACTION_DELETE
          putExtra("notificationUid", notif.uid)
          putExtra("senderId", notif.senderId)
          putExtra("receiverId", notif.receiverId)
        }

    receiver.onReceive(mockContext, intent)

    // Advance time to allow coroutines to complete
    advanceUntilIdle()
    val result = notificationsRepository.getNotificationsForReceiver(currentUser.uid)
    assert(result.isEmpty())
    verify {
      mockManager.cancel(notif.uid.hashCode())
    } // Make sure that the notification was canceled
  }
}
