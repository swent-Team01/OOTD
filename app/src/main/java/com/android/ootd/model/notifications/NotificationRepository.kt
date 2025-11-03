import com.android.ootd.model.notifications.Notification

interface NotificationRepository {
  fun getNewUid(): String

  suspend fun addNotification(notification: Notification)

  suspend fun getNotification(notificationId: String): Notification

  suspend fun getNotificationsForReceiver(receiverId: String): List<Notification>

  suspend fun getNotificationsForSender(senderId: String): List<Notification>

  suspend fun deleteNotification(notification: Notification)

  fun getFollowNotificationId(senderId: String, receiverId: String): String

  suspend fun notificationExists(notificationId: String): Boolean
}
