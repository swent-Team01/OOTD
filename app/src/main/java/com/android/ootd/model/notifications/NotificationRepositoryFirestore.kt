package com.android.ootd.model.notifications

import NotificationRepository
import android.util.Log
import androidx.annotation.Keep
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import java.util.UUID
import kotlinx.coroutines.tasks.await

const val NOTIFICATION_COLLECTION_PATH = "notifications"

@Keep
private data class NotificationDto(
    val uid: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val type: String = "",
    val content: String = ""
)

private fun Notification.toDto(): NotificationDto {
  return NotificationDto(
      uid = this.uid,
      senderId = this.senderId,
      receiverId = this.receiverId,
      type = this.type,
      content = this.content)
}

private fun NotificationDto.toDomain(): Notification {
  return Notification(
      uid = this.uid,
      senderId = this.senderId,
      receiverId = this.receiverId,
      type = this.type,
      content = this.content)
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

  override suspend fun addNotification(notification: Notification) {
    try {
      val existingDoc =
          db.collection(NOTIFICATION_COLLECTION_PATH)
              .whereEqualTo("senderId", notification.senderId)
              .whereEqualTo("uid", notification.uid)
              .get()
              .await()

      if (!existingDoc.documents.isEmpty()) {
        throw IllegalArgumentException("Notification with UID ${notification.uid} already exists")
      }

      db.collection(NOTIFICATION_COLLECTION_PATH)
          .document(notification.uid)
          .set(notification.toDto())
          .await()

      Log.d(
          "NotificationRepositoryFirestore",
          "Successfully added notification with UID: ${notification.uid}")
    } catch (e: Exception) {
      Log.e("NotificationRepositoryFirestore", "Error adding notification: ${e.message}", e)
      throw e
    }
  }

  override suspend fun getNotification(notificationId: String): Notification {
    return try {
      val documentList =
          db.collection(NOTIFICATION_COLLECTION_PATH)
              .whereEqualTo("uid", notificationId)
              .get()
              .await()

      if (documentList.documents.isEmpty()) {
        throw NoSuchElementException("Notification with ID $notificationId not found")
      }

      transformNotificationDocument(documentList.documents[0])
          ?: throw IllegalStateException("Failed to transform document with ID $notificationId")
    } catch (e: Exception) {
      Log.e(
          "NotificationRepositoryFirestore",
          "Error getting notification $notificationId: ${e.message}",
          e)
      throw e
    }
  }

  override suspend fun getNotificationsForReceiver(receiverId: String): List<Notification> {
    return try {
      val querySnapshot =
          db.collection(NOTIFICATION_COLLECTION_PATH)
              .whereEqualTo("receiverId", receiverId)
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
              .whereEqualTo("senderId", senderId)
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

  override suspend fun deleteNotification(notification: Notification) {
    try {
      // At this point in time I consider that only receivers can delete notifications
      // This is because for following this is all that is needed.
      // Can be modified in future PRs.
      val documentList =
          db.collection(NOTIFICATION_COLLECTION_PATH)
              .whereEqualTo("receiverId", notification.receiverId)
              .whereEqualTo("uid", notification.uid)
              .get()
              .await()

      if (documentList.documents.isEmpty()) {
        throw NoSuchElementException("Notification with ID ${notification.uid} not found")
      }
      Log.d("NotificationRepositoryFirestore", "Successfully found the necessary document")
      db.collection(NOTIFICATION_COLLECTION_PATH)
          .document(documentList.documents[0].id)
          .delete()
          .await()

      Log.d(
          "NotificationRepositoryFirestore",
          "Successfully deleted notification with UID: ${notification.uid}")
    } catch (e: Exception) {
      Log.e("NotificationRepositoryFirestore", "Error deleting notification: ${e.message}", e)
      throw e
    }
  }

  override suspend fun notificationExists(notificationId: String): Boolean {
    return try {
      val querySnapshot =
          db.collection(NOTIFICATION_COLLECTION_PATH)
              .whereEqualTo("uid", notificationId)
              .get()
              .await()

      querySnapshot.documents.isNotEmpty()
    } catch (e: Exception) {
      Log.e(
          "NotificationRepositoryFirestore",
          "Error checking notification existence: ${e.message}",
          e)
      throw e
    }
  }
}
