package com.android.ootd.ui.search

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.ootd.model.user.User
import com.android.ootd.ui.theme.OOTDTheme

object UserProfileCardTestTags {
  const val USER_FOLLOW_BUTTON = "userFollowButton"
  const val PROFILE_CARD = "profileCard"
  const val USERNAME_TEXT = "usernameText"
  const val ERROR_MESSAGE = "errorMessage"
  const val ERROR_DISMISS_BUTTON = "errorDismissButton"
}

@Preview
@Composable
fun UserProfileCardPreview() {
  OOTDTheme {
    UserProfileCard(
        selectedUser =
            User(uid = "Bob", username = "TheMostSuperNameofTheWorldTheThirdKingOfPeople"),
        modifier = Modifier.padding(16.dp),
        isSelectedUserFollowed = false,
        hasRequestPending = false,
        errorMessage = null,
        onFollowClick = {},
        onErrorDismiss = {})
  }
}

@Composable
fun UserProfileCard(
    modifier: Modifier,
    selectedUser: User?,
    isSelectedUserFollowed: Boolean,
    hasRequestPending: Boolean,
    errorMessage: String?,
    onFollowClick: () -> Unit,
    onErrorDismiss: () -> Unit
) {
  Card(
      modifier = modifier.testTag(UserProfileCardTestTags.PROFILE_CARD),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary)) {
        Column(
            modifier = Modifier.padding(start = 28.dp, top = 30.dp, end = 28.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Top),
            horizontalAlignment = Alignment.Start,
        ) {
          Text(
              modifier =
                  Modifier.testTag(UserProfileCardTestTags.USERNAME_TEXT)
                      .horizontalScroll(rememberScrollState()),
              text = selectedUser?.username ?: "",
              fontSize = 32.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
              maxLines = 1)

          Spacer(modifier = Modifier.width(16.dp))

          Button(
              modifier =
                  Modifier.width(128.dp)
                      .height(48.dp)
                      .testTag(UserProfileCardTestTags.USER_FOLLOW_BUTTON),
              onClick = { selectedUser?.let { onFollowClick() } },
              enabled = !hasRequestPending || isSelectedUserFollowed,
              shape = RoundedCornerShape(12.dp),
              colors =
                  ButtonDefaults.buttonColors(
                      containerColor = MaterialTheme.colorScheme.primary,
                      disabledContainerColor =
                          MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))) {
                Text(
                    text =
                        when {
                          isSelectedUserFollowed -> "Unfollow"
                          hasRequestPending -> "Request Sent"
                          else -> "Follow"
                        },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium)
              }

          // Error message display
          errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                modifier = Modifier.testTag(UserProfileCardTestTags.ERROR_MESSAGE),
                text = error,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
                textAlign = TextAlign.Start,
                fontWeight = FontWeight.Normal)

            TextButton(
                modifier = Modifier.testTag(UserProfileCardTestTags.ERROR_DISMISS_BUTTON),
                onClick = onErrorDismiss) {
                  Text(
                      text = "Dismiss", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                }
          }
        }
      }
}
