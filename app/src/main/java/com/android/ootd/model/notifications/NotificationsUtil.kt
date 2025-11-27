package com.android.ootd.model.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.android.ootd.MainActivity
import com.android.ootd.NOTIFICATION_CLICK_ACTION
import com.android.ootd.OOTD_CHANNEL_ID
import com.android.ootd.R
import java.util.concurrent.TimeUnit

/**
 * Pushes given notification
 *
 * This function is useful for defining the properties of a push notification. For example, this
 * could entail some notifications need to be clicked, or deleted from the notification bar etc.
 */
@RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
fun sendLocalNotification(context: Context, notification: Notification) {
  val manager = NotificationManagerCompat.from(context)

  val mainIntent =
      Intent(context, MainActivity::class.java).apply {
        action = NOTIFICATION_CLICK_ACTION
        putExtra("senderId", notification.senderId)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }

  val mainPendingIntent =
      PendingIntent.getActivity(
          context,
          notification.uid.hashCode(),
          mainIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

  // --- ACCEPT ACTION ---
  val acceptIntent =
      Intent(context, NotificationActionReceiver::class.java).apply {
        action = NOTIFICATION_ACTION_ACCEPT
        putExtra("notificationUid", notification.uid)
        putExtra("senderId", notification.senderId)
        putExtra("receiverId", notification.receiverId)
      }

  val acceptPendingIntent =
      PendingIntent.getBroadcast(
          context,
          ("${notification.uid}_accept").hashCode(),
          acceptIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

  // --- DELETE ACTION ---
  val deleteIntent =
      Intent(context, NotificationActionReceiver::class.java).apply {
        action = NOTIFICATION_ACTION_DELETE
        putExtra("notificationUid", notification.uid)
        putExtra("senderId", notification.senderId)
        putExtra("receiverId", notification.receiverId)
      }

  val deletePendingIntent =
      PendingIntent.getBroadcast(
          context,
          ("${notification.uid}_delete").hashCode(),
          deleteIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

  // BUILD NOTIFICATION
  val builder =
      NotificationCompat.Builder(context, OOTD_CHANNEL_ID)
          .setSmallIcon(R.drawable.ic_notification)
          .setContentTitle(notification.getNotificationMessage())
          .setPriority(NotificationCompat.PRIORITY_HIGH)
          .setContentIntent(mainPendingIntent)
          .setAutoCancel(true)
          .addAction(
              R.drawable.ic_check, // accept icon
              "Accept",
              acceptPendingIntent)
          .addAction(
              R.drawable.ic_delete, // delete icon
              "Delete",
              deletePendingIntent)

  manager.notify(notification.uid.hashCode(), builder.build())
}

fun scheduleBackgroundNotificationSync(context: Context) {
  val workRequest =
      PeriodicWorkRequestBuilder<NotificationSyncWorker>(15, TimeUnit.MINUTES)
          .setConstraints(
              Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
          .build()

  WorkManager.getInstance(context)
      .enqueueUniquePeriodicWork("notification_sync", ExistingPeriodicWorkPolicy.KEEP, workRequest)
}
