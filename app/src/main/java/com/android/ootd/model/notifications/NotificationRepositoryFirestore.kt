package com.android.ootd.model.notifications

import NotificationRepository
import android.util.Log
import androidx.annotation.Keep
import com.android.ootd.model.account.AccountRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.toObject
import java.util.UUID
import kotlinx.coroutines.tasks.await

const val NOTIFICATION_COLLECTION_PATH = "notifications"
const val SENDER_ID = "senderId"
const val RECEIVER_ID = "receiverId"

@Keep
data class NotificationDto(
    val uid: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val type: String = "",
    val content: String = "",
    val wasPushed: Boolean = false
)

private fun Notification.toDto(): NotificationDto {
  return NotificationDto(
      uid = this.uid,
      senderId = this.senderId,
      receiverId = this.receiverId,
      type = this.type,
      content = this.content,
      wasPushed = this.wasPushed)
}

private fun NotificationDto.toDomain(): Notification {
  return Notification(
      uid = this.uid,
      senderId = this.senderId,
      receiverId = this.receiverId,
      type = this.type,
      content = this.content,
      wasPushed = this.wasPushed)
}

class NotificationRepositoryFirestore(private val db: FirebaseFirestore) : NotificationRepository {

  /** Helper method to check notification data as firestore might add the default values */
  private fun checkNotificationData(notification: Notification): Notification? {
    if (notification.uid.isBlank() ||
        notification.senderId.isBlank() ||
        notification.receiverId.isBlank() ||
        notification.type.isBlank() ||
        notification.content.isBlank()) {
      Log.e(
          "NotificationRepositoryFirestore",
          "Invalid notification data: one or more fields are blank")
      return null
    }
    return notification
  }

  /** Helper method to transform a Firestore document into a Notification object */
  private fun transformNotificationDocument(document: DocumentSnapshot): Notification? {
    return try {
      val notificationDto = document.toObject<NotificationDto>()
      if (notificationDto == null) {
        Log.e(
            "NotificationRepositoryFirestore",
            "Failed to deserialize document ${document.id} to Notification object. Data: ${document.data}")
        return null
      }
      checkNotificationData(notificationDto.toDomain())
    } catch (e: Exception) {
      Log.e(
          "NotificationRepositoryFirestore",
          "Error transforming document ${document.id}: ${e.message}",
          e)
      return null
    }
  }

  override fun getNewUid(): String {
    return UUID.randomUUID().toString()
  }

  /**
   * Generate a predictable follow notification ID Format: senderId_follow_receiverId This allows
   * Firestore rules to check for notification existence
   */
  override fun getFollowNotificationId(senderId: String, receiverId: String): String {
    return "${senderId}_follow_${receiverId}"
  }

  /** Adds a listener directly to the firebase for notifications that have not been pushed yet. */
  override fun listenForUnpushedNotifications(
      receiverId: String,
      onNewNotification: (Notification) -> Unit
  ): ListenerRegistration {
    return db.collection(NOTIFICATION_COLLECTION_PATH)
        .whereEqualTo(RECEIVER_ID, receiverId)
        .whereEqualTo("wasPushed", false)
        .addSnapshotListener { snapshot, error ->
          if (error != null) {
            Log.e("TAG = NotificationRepo", "Listener error: ${error.message}")
            return@addSnapshotListener
          }

          snapshot?.documents?.forEach { doc ->
            val notification = transformNotificationDocument(doc)
            if (notification != null) {
              onNewNotification(notification)

              // Mark as pushed
              db.collection(NOTIFICATION_COLLECTION_PATH)
                  .document(notification.uid)
                  .update("wasPushed", true)
            }
          }
        }
  }

  override suspend fun addNotification(notification: Notification) {
    try {
      val existingDoc =
          db.collection(NOTIFICATION_COLLECTION_PATH)
              .whereEqualTo(SENDER_ID, notification.senderId)
              .whereEqualTo("uid", notification.uid)
              .get()
              .await()

      require(existingDoc.documents.isEmpty()) {
        "Notification with UID ${notification.uid} already exists"
      }

      db.collection(NOTIFICATION_COLLECTION_PATH)
          .document(notification.uid)
          .set(notification.toDto())
          .await()
    } catch (e: Exception) {
      Log.e("NotificationRepositoryFirestore", "Error adding notification: ${e.message}", e)
      throw e
    }
  }

  override suspend fun getNotificationsForReceiver(receiverId: String): List<Notification> {
    return try {
      val querySnapshot =
          db.collection(NOTIFICATION_COLLECTION_PATH)
              .whereEqualTo(RECEIVER_ID, receiverId)
              .get()
              .await()

      querySnapshot.documents.mapNotNull { document -> transformNotificationDocument(document) }
    } catch (e: Exception) {
      Log.e(
          "NotificationRepositoryFirestore",
          "Error getting notifications for receiver $receiverId: ${e.message}",
          e)
      throw e
    }
  }

  override suspend fun getNotificationsForSender(senderId: String): List<Notification> {
    return try {
      val querySnapshot =
          db.collection(NOTIFICATION_COLLECTION_PATH)
              .whereEqualTo(SENDER_ID, senderId)
              .get()
              .await()

      querySnapshot.documents.mapNotNull { document -> transformNotificationDocument(document) }
    } catch (e: Exception) {
      Log.e(
          "NotificationRepositoryFirestore",
          "Error getting notifications for sender $senderId: ${e.message}",
          e)
      throw e
    }
  }

  override suspend fun deleteNotification(notificationId: String, receiverId: String) {
    try {
      // At this point in time I consider that only receivers can delete notifications
      // This is because for following this is all that is needed.
      // Can be modified in future PRs.
      val documentList =
          db.collection(NOTIFICATION_COLLECTION_PATH)
              .whereEqualTo(RECEIVER_ID, receiverId)
              .whereEqualTo("uid", notificationId)
              .get()
              .await()

      if (documentList.documents.isEmpty()) {
        throw NoSuchElementException("Notification with ID $notificationId not found")
      }

      db.collection(NOTIFICATION_COLLECTION_PATH)
          .document(documentList.documents[0].id)
          .delete()
          .await()
    } catch (e: Exception) {
      Log.e("NotificationRepositoryFirestore", "Error deleting notification: ${e.message}", e)
      throw e
    }
  }

  override suspend fun acceptFollowNotification(
      senderId: String,
      notificationId: String,
      receiverId: String,
      accountRepository: AccountRepository
  ) {

    // Add the sender as a friend
    val wasAddedToBoth = accountRepository.addFriend(receiverId, senderId)

    // If I could not update both friend lists
    // I throw an exception such that the notification does not disappear.
    // This will also help with offline mode
    check(wasAddedToBoth) { "Could not update both friend lists" }
    // Delete the notification
    deleteNotification(notificationId = notificationId, receiverId = receiverId)
  }
}
