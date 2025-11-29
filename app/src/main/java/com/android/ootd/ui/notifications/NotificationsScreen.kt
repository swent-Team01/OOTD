package com.android.ootd.ui.notifications

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.ootd.ui.notifications.NotificationsScreenTestTags.NOTIFICATION_LIST
import com.android.ootd.ui.theme.Background
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Tertiary
import com.android.ootd.ui.theme.Typography
import com.android.ootd.utils.composables.BackArrow
import com.android.ootd.utils.composables.CenteredEmptyState
import com.android.ootd.utils.composables.OOTDTopBar
import com.android.ootd.utils.composables.ShowText
import javax.annotation.processing.Generated

object NotificationsScreenTestTags {
  const val NOTIFICATIONS_SCREEN = "notificationsScreen"
  const val NOTIFICATIONS_TITLE = "notificationsTitle"
  const val NOTIFICATION_ITEM = "notificationItem"
  const val NOTIFICATION_LIST = "notificationList"
  const val ACCEPT_BUTTON = "acceptButton"
  const val DELETE_BUTTON = "deleteButton"
  const val EMPTY_STATE_TEXT = "emptyStateText"
  const val ERROR_MESSAGE = "errorMessage"
  const val PUSH_NOTIFICATIONS_INSTRUCTIONS = "pushNotificationsInstructions"
  const val ENABLE_PUSH_NOTIFICATIONS = "enablePushNotifications"
  const val BACK_BUTTON = "backButton"
}

@Generated("jacoco ignore")
@Composable
fun rememberPermissionLauncher(onResult: (Boolean) -> Unit) =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), onResult)

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel = viewModel(),
    testMode: Boolean = false,
    onBackClick: () -> Unit = {}
) {
  val uiState by viewModel.uiState.collectAsState()
  val context = LocalContext.current

  // Track permission state to trigger recomposition
  var isNotificationsPermissionGranted by remember {
    mutableStateOf(
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED)
  }

  val permissionLauncher = rememberPermissionLauncher { isNotificationsPermissionGranted = it }

  Scaffold(
      modifier = Modifier.fillMaxSize().testTag(NotificationsScreenTestTags.NOTIFICATIONS_SCREEN),
      topBar = {
        OOTDTopBar(
            textModifier = Modifier.testTag(NotificationsScreenTestTags.NOTIFICATIONS_TITLE),
            centerText = "Notifications",
            leftComposable = {
              BackArrow(
                  onBackClick = onBackClick,
                  modifier = Modifier.testTag(NotificationsScreenTestTags.BACK_BUTTON))
            })
      },
      containerColor = Background) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 20.dp)) {
              Spacer(modifier = Modifier.height(24.dp))

              if (!isNotificationsPermissionGranted) {
                Text(
                    modifier =
                        Modifier.fillMaxWidth()
                            .testTag(NotificationsScreenTestTags.PUSH_NOTIFICATIONS_INSTRUCTIONS),
                    text =
                        "If you want push notifications in your inbox, enable them by clicking the button",
                    fontSize = 16.sp,
                    color = Primary)
                Button(
                    onClick = {
                      if (!testMode) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                      }
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                            .testTag(NotificationsScreenTestTags.ENABLE_PUSH_NOTIFICATIONS)) {
                      Text("Enable Push Notifications")
                    }

                Spacer(modifier = Modifier.height(16.dp))
              }

              when {
                uiState.isLoading -> {
                  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
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
                            style = Typography.bodyLarge)

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(onClick = { viewModel.loadFollowRequests() }) { Text("Retry") }
                      }
                }

                uiState.followRequests.isEmpty() -> {
                  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CenteredEmptyState(
                        text = {
                          ShowText(
                              modifier =
                                  Modifier.testTag(NotificationsScreenTestTags.EMPTY_STATE_TEXT),
                              text = "Welcome to the app!",
                              style = Typography.titleLarge,
                              color = Tertiary)
                          Spacer(modifier = Modifier.height(8.dp))
                          Text(
                              text = "Stay drippy!", style = Typography.bodyLarge, color = Tertiary)
                        })
                  }
                }

                else -> {
                  LazyColumn(
                      modifier = Modifier.fillMaxSize().testTag(NOTIFICATION_LIST),
                      verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(items = uiState.followRequests, key = { it.notification.uid }) {
                            followRequest ->
                          FollowRequestCard(
                              followRequestItem = followRequest,
                              onAccept = { viewModel.acceptFollowRequest(followRequest) },
                              onDelete = { viewModel.deleteFollowRequest(followRequest) })
                        }
                      }
                }
              }
            }
      }
}
