package com.android.ootd.model.notifications

import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import com.android.ootd.model.account.Account
import com.android.ootd.model.user.User
import com.android.ootd.utils.FirestoreTest
import io.mockk.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class NotificationActionReceiverInstrumentedTest : FirestoreTest() {
  private lateinit var mockManager: NotificationManagerCompat
  private lateinit var receiver: NotificationActionReceiver
  private lateinit var mockContext: Context

  @Before
  override fun setUp() = runBlocking {
    super.setUp()
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
    super.tearDown()
    unmockkAll()
  }

  @Test
  fun testAcceptActionTriggersRepositoryCall() = runTest {
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
    Thread.sleep(1500)

    val result = notificationsRepository.getNotificationsForReceiver(currentUser.uid)
    assert(result.isEmpty())

    verify { mockManager.cancel(notif.uid.hashCode()) }
  }

  @Test
  fun testDeleteActionTriggersRepositoryCall() = runTest {
    val notif =
        Notification(
            uid = "notif555",
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
    Thread.sleep(1500)

    val result = notificationsRepository.getNotificationsForReceiver(currentUser.uid)
    assert(result.isEmpty())

    verify { mockManager.cancel(notif.uid.hashCode()) }
  }

  @Test
  fun testInvalidIntentDoesNothing() = runTest {
    val intent = Intent()
    intent.putExtra("notificationUid", "id") // missing senderId and receiverId

    receiver.onReceive(mockContext, intent)

    verify(exactly = 0) { mockManager.cancel(any()) }
  }
}
