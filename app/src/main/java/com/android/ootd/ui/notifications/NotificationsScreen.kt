package com.android.ootd.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.ootd.ui.notifications.NotificationsScreenTestTags.NOTIFICATION_LIST
import com.android.ootd.ui.theme.OOTDTheme

object NotificationsScreenTestTags {
  const val NOTIFICATIONS_SCREEN = "notificationsScreen"
  const val NOTIFICATIONS_TITLE = "notificationsTitle"
  const val NOTIFICATION_ITEM = "notificationItem"
  const val NOTIFICATION_LIST = "notificationList"
  const val ACCEPT_BUTTON = "acceptButton"
  const val DELETE_BUTTON = "deleteButton"
  const val EMPTY_STATE_TEXT = "emptyStateText"
  const val ERROR_MESSAGE = "errorMessage"
}

@Composable
fun NotificationsScreen(viewModel: NotificationsViewModel = viewModel()) {
  val uiState by viewModel.uiState.collectAsState()

  LaunchedEffect(Unit) {}

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

@Preview(showBackground = true)
@Composable
fun NotificationsScreenPreview() {
  OOTDTheme { NotificationsScreen() }
}
