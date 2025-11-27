package com.android.ootd.model.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.android.ootd.model.account.AccountRepositoryProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class NotificationActionReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    val notificationId = intent.getStringExtra("notificationUid") ?: return
    val receiverId = intent.getStringExtra("receiverId") ?: return
    val senderId = intent.getStringExtra("senderId") ?: return

    when (intent.action) {
      NOTIFICATION_ACTION_ACCEPT -> {
        runBlocking(Dispatchers.IO) {
          NotificationRepositoryProvider.repository.acceptFollowNotification(
              notificationId = notificationId,
              senderId = senderId,
              receiverId = receiverId,
              accountRepository = AccountRepositoryProvider.repository)
        }
      }

      NOTIFICATION_ACTION_DELETE -> {
        runBlocking(Dispatchers.IO) {
          NotificationRepositoryProvider.repository.deleteNotification(
              notificationId = notificationId, receiverId = receiverId)
        }
      }
    }
    NotificationManagerCompat.from(context).cancel(notificationId.hashCode())
  }
}

const val NOTIFICATION_ACTION_ACCEPT = "com.android.ootd.NOTIFICATION_ACCEPT"
const val NOTIFICATION_ACTION_DELETE = "com.android.ootd.NOTIFICATION_DELETE"
