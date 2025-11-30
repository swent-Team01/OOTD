package com.android.ootd.model.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class NotificationSyncWorker(
    context: Context,
    workerParams: WorkerParameters,
    val testing: Boolean = false
) : CoroutineWorker(context, workerParams) {
  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
  override suspend fun doWork(): Result {
    val userId = Firebase.auth.currentUser?.uid ?: return Result.success()

    // Check notification permission (Android 13+)
    val hasPermission =
        ContextCompat.checkSelfPermission(
            applicationContext, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    if (!hasPermission && !testing) {
      return Result.success()
    }

    val notifications = NotificationRepositoryProvider.repository.getUnpushedNotifications(userId)

    notifications.forEach { notification ->
      sendLocalNotification(applicationContext, notification)
      NotificationRepositoryProvider.repository.markNotificationAsPushed(notification.uid)
    }

    return Result.success()
  }
}
