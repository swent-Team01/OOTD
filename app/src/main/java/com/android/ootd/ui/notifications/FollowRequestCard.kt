package com.android.ootd.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.ootd.utils.ProfilePicture

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
                    ProfilePicture(
                        size = 48.dp,
                        profilePicture = followRequestItem.senderUser.profilePicture,
                        username = followRequestItem.senderUser.username,
                        textStyle = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp))
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
