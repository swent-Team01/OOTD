package com.android.ootd.ui.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.ootd.model.notifications.Notification
import com.android.ootd.model.user.User
import com.android.ootd.ui.notifications.NotificationsScreenTestTags.NOTIFICATION_LIST
import com.android.ootd.ui.theme.OOTDTheme

object NotificationsScreenTestTags {
  const val NOTIFICATIONS_SCREEN = "notificationsScreen"
  const val NOTIFICATIONS_TITLE = "notificationsTitle"
  const val NOTIFICATION_ITEM = "notificationItem"
  const val NOTIFICATION_LIST = "notificationList"
  const val ACCEPT_BUTTON = "acceptButton"
  const val DELETE_BUTTON = "deleteButton"
  const val REQUEST_PERMISSION_BUTTON = "requestPermissionButton"
  const val EMPTY_STATE_TEXT = "emptyStateText"
  const val ERROR_MESSAGE = "errorMessage"
}

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
fun NotificationsScreen(viewModel: NotificationsViewModel = viewModel()) {
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsState()

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
          onResult = { isGranted ->
            if (isGranted || uiState.overrideNotificationPopup) {
              viewModel.loadFollowRequests()
            }
          })

  LaunchedEffect(Unit) { createNotificationChannel(context) }

  Column(
      modifier =
          Modifier.fillMaxSize()
              .background(MaterialTheme.colorScheme.background)
              .padding(horizontal = 20.dp, vertical = 32.dp)
              .testTag(NotificationsScreenTestTags.NOTIFICATIONS_SCREEN)) {
        // Title
        Text(
            modifier =
                Modifier.fillMaxWidth().testTag(NotificationsScreenTestTags.NOTIFICATIONS_TITLE),
            text = "Notifications",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)

        Spacer(modifier = Modifier.height(24.dp))

        if (!hasNotificationPermission && !uiState.overrideNotificationPopup) {
          // Permission not granted state
          Column(
              modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center) {
                Text(
                    text = "Notification permission is required to see your follow requests",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 32.dp))

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    modifier =
                        Modifier.testTag(NotificationsScreenTestTags.REQUEST_PERMISSION_BUTTON),
                    onClick = {
                      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                      }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary)) {
                      Text("Enable Notifications")
                    }
              }
        } else {
          // Permission granted - show notifications
          when {
            uiState.isLoading -> {
              Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
              }
            }
            uiState.errorMessage != null -> {
              Column(
                  modifier = Modifier.fillMaxSize(),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.Center) {
                    Text(
                        modifier =
                            Modifier.padding(horizontal = 32.dp)
                                .testTag(NotificationsScreenTestTags.ERROR_MESSAGE),
                        text = uiState.errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge)

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = { viewModel.loadFollowRequests() }) { Text("Retry") }
                  }
            }
            uiState.followRequests.isEmpty() -> {
              Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center) {
                      Text(
                          modifier = Modifier.testTag(NotificationsScreenTestTags.EMPTY_STATE_TEXT),
                          text = "Welcome to the app!",
                          style = MaterialTheme.typography.bodyLarge,
                          color = MaterialTheme.colorScheme.onBackground)
                      Spacer(modifier = Modifier.height(8.dp))
                      Text(
                          text = "Stay drippy!",
                          style = MaterialTheme.typography.bodyLarge,
                          color = MaterialTheme.colorScheme.onBackground)
                    }
              }
            }
            else -> {
              LazyColumn(
                  modifier = Modifier.fillMaxSize().testTag(NOTIFICATION_LIST),
                  verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(items = uiState.followRequests, key = { it.notification.uid }) {
                        followRequestItem ->
                      FollowRequestCard(
                          followRequestItem = followRequestItem,
                          onAccept = { viewModel.acceptFollowRequest(followRequestItem) },
                          onDelete = { viewModel.deleteFollowRequest(followRequestItem) })
                    }
                  }
            }
          }
        }
      }
}

@Composable
fun FollowRequestCard(
    followRequestItem: FollowRequestItem,
    onAccept: () -> Unit,
    onDelete: () -> Unit
) {
  Card(
      modifier =
          Modifier.fillMaxWidth()
              .height(100.dp)
              .testTag(NotificationsScreenTestTags.NOTIFICATION_ITEM),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically) {
              // Profile picture placeholder
              Box(
                  modifier =
                      Modifier.size(48.dp)
                          .clip(CircleShape)
                          .background(MaterialTheme.colorScheme.primary),
                  contentAlignment = Alignment.Center) {
                    Text(
                        text =
                            followRequestItem.senderUser.username.firstOrNull()?.uppercase() ?: "?",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold)
                  }

              Spacer(modifier = Modifier.width(12.dp))

              // Username and message - now in a Column that takes available space
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(
                    text = followRequestItem.senderUser.username,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(
                    text = followRequestItem.notification.content,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
              }

              Spacer(modifier = Modifier.width(8.dp))

              // Buttons in a Column for better space management
              Column(
                  verticalArrangement = Arrangement.spacedBy(4.dp),
                  horizontalAlignment = Alignment.End) {
                    // Accept button
                    Button(
                        modifier =
                            Modifier.width(80.dp)
                                .height(36.dp)
                                .testTag(NotificationsScreenTestTags.ACCEPT_BUTTON),
                        onClick = onAccept,
                        shape = RoundedCornerShape(20.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(0.dp)) {
                          Text(
                              "Accept",
                              fontSize = 12.sp,
                              color = MaterialTheme.colorScheme.background)
                        }
                    // Delete button
                    Button(
                        modifier =
                            Modifier.width(80.dp)
                                .height(36.dp)
                                .testTag(NotificationsScreenTestTags.DELETE_BUTTON),
                        onClick = onDelete,
                        shape = RoundedCornerShape(20.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary),
                        contentPadding = PaddingValues(0.dp)) {
                          Text(
                              "Delete",
                              fontSize = 12.sp,
                              color = MaterialTheme.colorScheme.background)
                        }
                  }
            }
      }
}

@Preview(showBackground = true)
@Composable
fun NotificationsScreenPreview() {
  OOTDTheme { NotificationsScreen() }
}

@Preview(showBackground = true)
@Composable
fun FollowRequestCardPreview() {
  OOTDTheme {
    FollowRequestCard(
        followRequestItem =
            FollowRequestItem(
                notification =
                    Notification(
                        uid = "123",
                        senderId = "user123",
                        receiverId = "currentUser",
                        type = "FOLLOW_REQUEST",
                        content = "wants to follow you"),
                senderUser = User(uid = "user123", username = "PitBull")),
        onAccept = {},
        onDelete = {})
  }
}
