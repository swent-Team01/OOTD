package com.android.ootd.model.notifications

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NotificationRepositoryFirestoreTest {

  private lateinit var mockDb: FirebaseFirestore
  private lateinit var mockCollection: CollectionReference
  private lateinit var mockDocumentRef: DocumentReference
  private lateinit var mockQuery: Query
  private lateinit var repository: NotificationRepositoryFirestore

  @Before
  fun setup() {
    mockDb = mockk(relaxed = true)
    mockCollection = mockk(relaxed = true)
    mockDocumentRef = mockk(relaxed = true)
    mockQuery = mockk(relaxed = true)

    every { mockDb.collection(NOTIFICATION_COLLECTION_PATH) } returns mockCollection

    repository = NotificationRepositoryFirestore(mockDb)
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun getNewUidReturnsNonEmptyUuidString() {
    val uid = repository.getNewUid()

    assertNotNull(uid)
    assertFalse(uid.isEmpty())
    assertTrue(uid.contains("-"))
  }

  @Test
  fun getNewUidReturnsUniqueIds() {
    val uid1 = repository.getNewUid()
    val uid2 = repository.getNewUid()

    assertTrue(uid1 != uid2)
  }

  @Test
  fun getFollowNotificationIdReturnsCorrectFormat() {
    val senderId = "sender123"
    val receiverId = "receiver456"

    val result = repository.getFollowNotificationId(senderId, receiverId)

    assertEquals("sender123_follow_receiver456", result)
  }

  @Test
  fun addNotificationSuccessfullyAddsNotificationWhenItDoesNotExist() = runTest {
    val notification =
        Notification(
            uid = "notif123",
            senderId = "sender123",
            receiverId = "receiver456",
            type = "follow",
            content = "started following you",
            senderName = "")

    val mockQuerySnapshot: QuerySnapshot = mockk(relaxed = true)
    every { mockQuerySnapshot.documents } returns emptyList()
    every { mockCollection.whereEqualTo("senderId", notification.senderId) } returns mockQuery
    every { mockQuery.whereEqualTo("uid", notification.uid) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)

    every { mockCollection.document(notification.uid) } returns mockDocumentRef
    every { mockDocumentRef.set(any()) } returns Tasks.forResult(null)

    repository.addNotification(notification)

    verify { mockCollection.document(notification.uid) }
    verify { mockDocumentRef.set(any()) }
  }

  @Test(expected = IllegalArgumentException::class)
  fun addNotificationThrowsExceptionWhenNotificationAlreadyExists() = runTest {
    val notification =
        Notification(
            uid = "notif123",
            senderId = "sender123",
            receiverId = "receiver456",
            type = "follow",
            content = "started following you",
            senderName = "")

    val mockDocSnapshot: DocumentSnapshot = mockk(relaxed = true)
    val mockQuerySnapshot: QuerySnapshot = mockk(relaxed = true)
    every { mockQuerySnapshot.documents } returns listOf(mockDocSnapshot)
    every { mockCollection.whereEqualTo("senderId", notification.senderId) } returns mockQuery
    every { mockQuery.whereEqualTo("uid", notification.uid) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)

    repository.addNotification(notification)
  }

  @Test
  fun getNotificationsForReceiverReturnsListOfNotifications() = runTest {
    val receiverId = "receiver123"

    val mockDoc1: DocumentSnapshot = mockk(relaxed = true)
    val mockDoc2: DocumentSnapshot = mockk(relaxed = true)

    every { mockDoc1.toObject(NotificationDto::class.java) } returns
        NotificationDto(
            uid = "notif1",
            senderId = "sender1",
            receiverId = receiverId,
            type = "follow",
            content = "followed you")

    every { mockDoc2.toObject(NotificationDto::class.java) } returns
        NotificationDto(
            uid = "notif2",
            senderId = "sender2",
            receiverId = receiverId,
            type = "like",
            content = "liked your post")

    val mockQuerySnapshot: QuerySnapshot = mockk(relaxed = true)
    every { mockQuerySnapshot.documents } returns listOf(mockDoc1, mockDoc2)

    every { mockCollection.whereEqualTo("receiverId", receiverId) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)

    val result = repository.getNotificationsForReceiver(receiverId)

    assertEquals(2, result.size)
    assertEquals("notif1", result[0].uid)
    assertEquals("notif2", result[1].uid)
  }

  @Test
  fun getNotificationsForReceiverFiltersOutInvalidNotifications() = runTest {
    val receiverId = "receiver123"

    val mockDoc1: DocumentSnapshot = mockk(relaxed = true)
    val mockDoc2: DocumentSnapshot = mockk(relaxed = true)

    every { mockDoc1.toObject(NotificationDto::class.java) } returns
        NotificationDto(
            uid = "notif1",
            senderId = "sender1",
            receiverId = receiverId,
            type = "follow",
            content = "followed you")

    every { mockDoc2.toObject(NotificationDto::class.java) } returns
        NotificationDto(
            uid = "",
            senderId = "sender2",
            receiverId = receiverId,
            type = "like",
            content = "liked your post")

    val mockQuerySnapshot: QuerySnapshot = mockk(relaxed = true)
    every { mockQuerySnapshot.documents } returns listOf(mockDoc1, mockDoc2)

    every { mockCollection.whereEqualTo("receiverId", receiverId) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)

    val result = repository.getNotificationsForReceiver(receiverId)

    assertEquals(1, result.size)
    assertEquals("notif1", result[0].uid)
  }

  @Test
  fun getNotificationsForSenderReturnsListOfNotifications() = runTest {
    val senderId = "sender123"

    val mockDoc1: DocumentSnapshot = mockk(relaxed = true)
    every { mockDoc1.toObject(NotificationDto::class.java) } returns
        NotificationDto(
            uid = "notif1",
            senderId = senderId,
            receiverId = "receiver1",
            type = "follow",
            content = "followed you")

    val mockQuerySnapshot: QuerySnapshot = mockk(relaxed = true)
    every { mockQuerySnapshot.documents } returns listOf(mockDoc1)

    every { mockCollection.whereEqualTo("senderId", senderId) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)

    val result = repository.getNotificationsForSender(senderId)

    assertEquals(1, result.size)
    assertEquals(senderId, result[0].senderId)
  }

  @Test(expected = Exception::class)
  fun getNotificationsForReceiverThrowsExceptionOnFirestoreError() = runTest {
    val receiverId = "receiver123"

    every { mockCollection.whereEqualTo("receiverId", receiverId) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forException(Exception("Firestore error"))

    repository.getNotificationsForReceiver(receiverId)
  }

  @Test(expected = NoSuchElementException::class)
  fun deleteNotificationThrowsExceptionWhenNotificationNotFound() = runTest {
    val notification =
        Notification(
            uid = "notif123",
            senderId = "sender123",
            receiverId = "receiver456",
            type = "follow",
            content = "started following you",
            senderName = "")

    val mockQuerySnapshot: QuerySnapshot = mockk(relaxed = true)
    every { mockQuerySnapshot.documents } returns emptyList()

    every { mockCollection.whereEqualTo("receiverId", notification.receiverId) } returns mockQuery
    every { mockQuery.whereEqualTo("uid", notification.uid) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)

    repository.deleteNotification(
        notificationId = notification.uid, receiverId = notification.receiverId)
  }

  @Test
  fun deleteNotificationFindsNotificationSuccessfully() = runTest {
    val notification =
        Notification(
            uid = "notif123",
            senderId = "sender123",
            receiverId = "receiver456",
            type = "follow",
            content = "started following you",
            senderName = "")

    val mockDoc: DocumentSnapshot = mockk(relaxed = true)
    val mockQuerySnapshot: QuerySnapshot = mockk(relaxed = true)
    val mockDocumentReference: DocumentReference = mockk(relaxed = true)

    // Mock the document ID
    every { mockDoc.id } returns "firebaseDocId123"
    every { mockQuerySnapshot.documents } returns listOf(mockDoc)

    // Mock the query chain
    every { mockCollection.whereEqualTo("receiverId", notification.receiverId) } returns mockQuery
    every { mockQuery.whereEqualTo("uid", notification.uid) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)

    // Mock the delete operation
    every { mockCollection.document("firebaseDocId123") } returns mockDocumentReference
    every { mockDocumentReference.delete() } returns Tasks.forResult(null)

    repository.deleteNotification(
        notificationId = notification.uid, receiverId = notification.receiverId)

    verify { mockQuery.get() }
    verify { mockCollection.document("firebaseDocId123") }
    verify { mockDocumentReference.delete() }
  }

  @Test
  fun listenForUnpushedNotificationsInvokesCallbackAndMarksAsPushed() {
    val receiverId = "receiver123"

    // Mock components
    val listenerSlot = slot<EventListener<QuerySnapshot>>()

    val mockDocSnapshot = mockk<DocumentSnapshot>(relaxed = true)
    val mockSnapshot = mockk<QuerySnapshot>(relaxed = true)

    // Valid DTO from Firestore
    every { mockDocSnapshot.toObject(NotificationDto::class.java) } returns
        NotificationDto(
            uid = "notif123",
            senderId = "sender1",
            receiverId = receiverId,
            type = "follow",
            content = "hello",
            wasPushed = false)

    every { mockSnapshot.documents } returns listOf(mockDocSnapshot)

    val mockDocRef = mockk<DocumentReference>(relaxed = true)
    every { mockCollection.document("notif123") } returns mockDocRef
    every { mockDocRef.update("wasPushed", true) } returns Tasks.forResult(null)

    // Capture Java EventListener<QuerySnapshot>
    every {
      mockCollection
          .whereEqualTo(RECEIVER_ID, receiverId)
          .whereEqualTo("wasPushed", false)
          .addSnapshotListener(capture(listenerSlot))
    } returns mockk(relaxed = true)

    var callbackNotification: Notification? = null

    // Start listener (this registers the captured event listener)
    repository.listenForUnpushedNotifications(receiverId) { callbackNotification = it }

    // Simulate Firestore firing the listener
    listenerSlot.captured.onEvent(mockSnapshot, null)

    // Assertions
    assertNotNull(callbackNotification)
    assertEquals("notif123", callbackNotification!!.uid)

    verify { mockDocRef.update("wasPushed", true) }
  }

  // NEW TESTS for getUnpushedNotifications

  @Test
  fun getUnpushedNotificationsReturnsListOfNotifications() = runTest {
    val receiverId = "receiver123"

    val mockDoc1: DocumentSnapshot = mockk(relaxed = true)
    val mockDoc2: DocumentSnapshot = mockk(relaxed = true)

    every { mockDoc1.toObject(NotificationDto::class.java) } returns
        NotificationDto(
            uid = "notif1",
            senderId = "sender1",
            receiverId = receiverId,
            type = "follow",
            content = "followed you",
            wasPushed = false)

    every { mockDoc2.toObject(NotificationDto::class.java) } returns
        NotificationDto(
            uid = "notif2",
            senderId = "sender2",
            receiverId = receiverId,
            type = "like",
            content = "liked your post",
            wasPushed = false)

    val mockQuerySnapshot: QuerySnapshot = mockk(relaxed = true)
    every { mockQuerySnapshot.documents } returns listOf(mockDoc1, mockDoc2)

    every { mockCollection.whereEqualTo(RECEIVER_ID, receiverId) } returns mockQuery
    every { mockQuery.whereEqualTo("wasPushed", false) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)

    val result = repository.getUnpushedNotifications(receiverId)

    assertEquals(2, result.size)
    assertEquals("notif1", result[0].uid)
    assertEquals("notif2", result[1].uid)
    verify { mockCollection.whereEqualTo(RECEIVER_ID, receiverId) }
    verify { mockQuery.whereEqualTo("wasPushed", false) }
  }

  @Test
  fun getUnpushedNotificationsReturnsEmptyListWhenNoNotificationsFound() = runTest {
    val receiverId = "receiver123"
    val mockQuerySnapshot: QuerySnapshot = mockk(relaxed = true)

    every { mockQuerySnapshot.documents } returns emptyList()
    every { mockCollection.whereEqualTo(RECEIVER_ID, receiverId) } returns mockQuery
    every { mockQuery.whereEqualTo("wasPushed", false) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)

    val result = repository.getUnpushedNotifications(receiverId)

    assertTrue(result.isEmpty())
  }

  @Test
  fun getUnpushedNotificationsFiltersOutInvalidNotifications() = runTest {
    val receiverId = "receiver123"

    val mockDoc1: DocumentSnapshot = mockk(relaxed = true)
    val mockDoc2: DocumentSnapshot = mockk(relaxed = true)
    val mockDoc3: DocumentSnapshot = mockk(relaxed = true)

    every { mockDoc1.toObject(NotificationDto::class.java) } returns
        NotificationDto(
            uid = "notif1",
            senderId = "sender1",
            receiverId = receiverId,
            type = "follow",
            content = "followed you",
            wasPushed = false)

    every { mockDoc2.toObject(NotificationDto::class.java) } returns
        NotificationDto(
            uid = "",
            senderId = "sender2",
            receiverId = receiverId,
            type = "like",
            content = "liked your post",
            wasPushed = false)

    every { mockDoc3.toObject(NotificationDto::class.java) } returns null

    val mockQuerySnapshot: QuerySnapshot = mockk(relaxed = true)
    every { mockQuerySnapshot.documents } returns listOf(mockDoc1, mockDoc2, mockDoc3)

    every { mockCollection.whereEqualTo(RECEIVER_ID, receiverId) } returns mockQuery
    every { mockQuery.whereEqualTo("wasPushed", false) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)

    val result = repository.getUnpushedNotifications(receiverId)

    assertEquals(1, result.size)
    assertEquals("notif1", result[0].uid)
  }

  @Test
  fun getUnpushedNotificationsReturnsEmptyListOnException() = runTest {
    val receiverId = "receiver123"

    every { mockCollection.whereEqualTo(RECEIVER_ID, receiverId) } returns mockQuery
    every { mockQuery.whereEqualTo("wasPushed", false) } returns mockQuery
    every { mockQuery.get() } returns Tasks.forException(Exception("Firestore error"))

    val result = repository.getUnpushedNotifications(receiverId)

    assertTrue(result.isEmpty())
  }

  // NEW TESTS for markNotificationAsPushed

  @Test
  fun markNotificationAsPushedUpdatesFieldSuccessfully() = runTest {
    val notificationId = "notif123"

    every { mockCollection.document(notificationId) } returns mockDocumentRef
    every { mockDocumentRef.update("wasPushed", true) } returns Tasks.forResult(null)

    repository.markNotificationAsPushed(notificationId)

    verify { mockCollection.document(notificationId) }
    verify { mockDocumentRef.update("wasPushed", true) }
  }

  @Test
  fun markNotificationAsPushedHandlesExceptionGracefully() = runTest {
    val notificationId = "notif123"

    every { mockCollection.document(notificationId) } returns mockDocumentRef
    every { mockDocumentRef.update("wasPushed", true) } returns
        Tasks.forException(Exception("Update failed"))

    // Should not throw exception - it's caught internally
    repository.markNotificationAsPushed(notificationId)

    verify { mockCollection.document(notificationId) }
    verify { mockDocumentRef.update("wasPushed", true) }
  }
}
