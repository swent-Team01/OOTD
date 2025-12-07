package com.android.ootd.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
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
import com.android.ootd.ui.search.UserProfileCardTestTags.AVATAR_IMAGE
import com.android.ootd.ui.search.UserProfileCardTestTags.AVATAR_LETTER
import com.android.ootd.ui.theme.OOTDTheme
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Typography
import com.android.ootd.utils.composables.ClickableProfileRow
import com.android.ootd.utils.composables.ProfilePicture

object UserProfileCardTestTags {
  const val USER_FOLLOW_BUTTON = "userFollowButton"
  const val PROFILE_CARD = "profileCard"
  const val USERNAME_TEXT = "usernameText"
  const val ERROR_MESSAGE = "errorMessage"
  const val ERROR_DISMISS_BUTTON = "errorDismissButton"
  const val AVATAR_IMAGE = "avatarImage"
  const val AVATAR_LETTER = "avatarLetter"
}

@Preview
@Composable
fun UserProfileCardPreview() {
  OOTDTheme {
    UserProfileCard(
        selectedUser =
            User(
                uid = "Bob",
                ownerId = "Bob",
                username = "TheMostSuperNameofTheWorldTheThirdKingOfPeople"),
        modifier = Modifier.padding(16.dp),
        isSelectedUserFollowed = false,
        hasRequestPending = false,
        errorMessage = null,
        onFollowClick = {},
        onUserClick = {},
        onErrorDismiss = {})
  }
}

@Preview
@Composable
fun UserProfileCardWithErrorPreview() {
  OOTDTheme {
    UserProfileCard(
        selectedUser = User(uid = "Bob", username = "TestUser"),
        modifier = Modifier.padding(16.dp),
        isSelectedUserFollowed = false,
        hasRequestPending = false,
        errorMessage = "Something went wrong. Please check your connection and try again.",
        onFollowClick = {},
        onUserClick = {},
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
    onUserClick: (String) -> Unit,
    onErrorDismiss: () -> Unit
) {
  val followText =
      when {
        isSelectedUserFollowed -> "Unfollow"
        hasRequestPending -> "Request Sent"
        else -> "Follow"
      }
  Card(
      modifier = modifier.testTag(UserProfileCardTestTags.PROFILE_CARD),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = colorScheme.secondary)) {
        Column(
            modifier = Modifier.padding(start = 28.dp, top = 30.dp, end = 28.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Top),
            horizontalAlignment = Alignment.Start,
        ) {
            if (selectedUser != null && errorMessage == null) {
                ClickableProfileRow(
                    userId = selectedUser.uid,
                    username = selectedUser.username,
                    profilePictureUrl = selectedUser.profilePicture,
                    profileSize = 50.dp,
                    onProfileClick = onUserClick,
                    enabled = true,
                    usernameStyle = Typography.headlineMedium.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    usernameColor = colorScheme.onSecondaryContainer,
                    usernameMaxLines = 1,
                    profileTestTag = if (selectedUser.profilePicture.isNotBlank())
                        AVATAR_IMAGE else AVATAR_LETTER,
                    usernameTestTag = UserProfileCardTestTags.USERNAME_TEXT,
                    usernameModifier = Modifier.horizontalScroll(rememberScrollState()),
                    modifier = Modifier.fillMaxWidth()
                )
            }

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
                      containerColor = Primary,
                      disabledContainerColor = Primary.copy(alpha = 0.6f))) {
                Text(text = followText, fontSize = 16.sp, fontWeight = FontWeight.Medium)
              }

          // Error message display
          errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                modifier = Modifier.testTag(UserProfileCardTestTags.ERROR_MESSAGE),
                text = error,
                color = colorScheme.error,
                fontSize = 14.sp,
                textAlign = TextAlign.Start,
                fontWeight = FontWeight.Normal)

            TextButton(
                modifier = Modifier.testTag(UserProfileCardTestTags.ERROR_DISMISS_BUTTON),
                onClick = onErrorDismiss) {
                  Text(text = "Dismiss", fontSize = 14.sp, color = Primary)
                }
          }
        }
      }
}
