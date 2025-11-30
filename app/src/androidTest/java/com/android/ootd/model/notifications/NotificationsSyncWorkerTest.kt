package com.android.ootd.model.notifications

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.android.ootd.utils.FirebaseEmulator
import com.android.ootd.utils.FirestoreTest
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationSyncWorkerInstrumentedTest : FirestoreTest() {

  private lateinit var context: Context

  @Before
  override fun setUp() {
    super.setUp()
    context = ApplicationProvider.getApplicationContext()
  }

  @Test
  fun testWorkerExecutesSuccessfullyWithAuthenticatedUser() = runTest {
    // Given: User is already signed in from FirestoreTest.setUp()
    val userId = FirebaseEmulator.auth.currentUser?.uid
    assert(userId != null) { "User should be authenticated" }

    val notif =
        Notification(
            uid = "notif123",
            senderId = currentUser.uid,
            receiverId = currentUser.uid,
            type = "FOLLOW_REQUEST",
            content = "hello",
            senderName = "")

    notificationsRepository.addNotification(notif)

    // Create the worker with testing enabled
    val worker =
        TestListenableWorkerBuilder<NotificationSyncWorker>(context)
            .setWorkerFactory(
                object : androidx.work.WorkerFactory() {
                  override fun createWorker(
                      appContext: Context,
                      workerClassName: String,
                      workerParameters: WorkerParameters
                  ): ListenableWorker {
                    return NotificationSyncWorker(appContext, workerParameters, testing = true)
                  }
                })
            .build()

    // When: Execute the worker
    val result = worker.doWork()

    // Then: Worker completes successfully
    assert(ListenableWorker.Result.success() == result)

    assert(notificationsRepository.getNotificationsForReceiver(currentUser.uid)[0].wasPushed)
  }
}
