package com.android.ootd.ui.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.android.ootd.R

// This was AI generated , human approved.

private fun createNotificationChannel(context: Context) {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    val name = "followNotifications"
    val descriptionText = "This is the channel through which follow notifications are sent"
    val importance = NotificationManager.IMPORTANCE_DEFAULT
    val channel =
        NotificationChannel("FollowNotifications", name, importance).apply {
          description = descriptionText
        }
    val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.createNotificationChannel(channel)
  }
}

@Composable
fun NotificationsScreen() {
  val context = LocalContext.current
  var hasNotificationPermission by remember {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      mutableStateOf(
          ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
              PackageManager.PERMISSION_GRANTED)
    } else {
      mutableStateOf(true)
    }
  }

  val launcher =
      rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestPermission(),
          onResult = { isGranted -> hasNotificationPermission = isGranted })

  LaunchedEffect(Unit) { createNotificationChannel(context) }

  Column(
      modifier = Modifier.fillMaxSize().padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center) {
        Text("These are the notifications!")

        Spacer(modifier = Modifier.height(16.dp))

        if (hasNotificationPermission) {
          Text("✅ Notification permission granted")

          Spacer(modifier = Modifier.height(16.dp))

          Button(
              onClick = {
                val builder =
                    NotificationCompat.Builder(context, "FollowNotifications")
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle("Hello there, this is a new notification!")
                        .setContentText("Click me if you dare!")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)

                with(NotificationManagerCompat.from(context)) { notify(1, builder.build()) }
              }) {
                Text("Send Test Notification")
              }
        } else {
          Text("❌ Notification permission not granted")

          Spacer(modifier = Modifier.height(16.dp))

          Button(
              onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                  launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
              }) {
                Text("Request Notification Permission")
              }
        }
      }
}
