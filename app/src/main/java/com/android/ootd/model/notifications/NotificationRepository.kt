import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.notifications.Notification
import com.google.firebase.firestore.ListenerRegistration

interface NotificationRepository {
  fun getNewUid(): String

  suspend fun addNotification(notification: Notification)

  suspend fun getNotificationsForReceiver(receiverId: String): List<Notification>

  suspend fun getNotificationsForSender(senderId: String): List<Notification>

  suspend fun deleteNotification(notificationId: String, receiverId: String)

  /** Accepts follow notification from sender* */
  suspend fun acceptFollowNotification(
      senderId: String,
      notificationId: String,
      receiverId: String,
      accountRepository: AccountRepository
  )

  suspend fun getUnpushedNotifications(receiverId: String): List<Notification>

  suspend fun markNotificationAsPushed(notificationId: String)

  fun getFollowNotificationId(senderId: String, receiverId: String): String

  fun listenForUnpushedNotifications(
      receiverId: String,
      onNewNotification: (Notification) -> Unit
  ): ListenerRegistration
}
