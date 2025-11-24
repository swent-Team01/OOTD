package com.android.ootd

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.android.ootd.model.account.AccountRepositoryProvider
import com.android.ootd.model.notifications.NotificationRepositoryProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    val notificationId = intent.getStringExtra("notificationUid") ?: return
    val receiverId = intent.getStringExtra("receiverId") ?: return
    val senderId = intent.getStringExtra("senderId") ?: return

    when (intent.action) {
      NOTIFICATION_ACTION_ACCEPT -> {
        Log.d("NotifActions", "Accept tapped for $notificationId")
        CoroutineScope(Dispatchers.IO).launch {
          NotificationRepositoryProvider.repository.acceptFollowNotification(
              notificationId = notificationId,
              senderId = senderId,
              receiverId = receiverId,
              accountRepository = AccountRepositoryProvider.repository)
        }
      }

      NOTIFICATION_ACTION_DELETE -> {
        Log.d("NotifActions", "Delete tapped for $notificationId")
        CoroutineScope(Dispatchers.IO).launch {
          NotificationRepositoryProvider.repository.deleteNotification(
              notificationId = notificationId, receiverId = receiverId)
        }
      }
    }
  }
}

const val NOTIFICATION_ACTION_ACCEPT = "com.android.ootd.NOTIFICATION_ACCEPT"
const val NOTIFICATION_ACTION_DELETE = "com.android.ootd.NOTIFICATION_DELETE"
