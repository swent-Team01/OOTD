/**
 * ViewUserScreen displays another user's profile information.
 *
 * This composable screen shows:
 * - User's profile picture or avatar with first letter of username
 * - Username as title
 * - Follow/Unfollow button
 * - Friend status text
 * - Friend count
 * - User's outfit posts (only visible if the user is a friend)
 * - Loading state with a branded loading indicator
 * - Error handling with toast messages
 *
 * The screen respects privacy by only showing posts if the viewed user is a friend of the current
 * user.
 */
package com.android.ootd.ui.account

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.ootd.ui.theme.Typography
import com.android.ootd.utils.composables.BackArrow
import com.android.ootd.utils.composables.DisplayUserPosts
import com.android.ootd.utils.composables.LoadingScreen
import com.android.ootd.utils.composables.OOTDTopBar
import com.android.ootd.utils.composables.ProfilePicture
import com.android.ootd.utils.composables.ShowText

/**
 * Test tags for ViewUserScreen components.
 *
 * These tags are used for UI testing to identify and interact with specific components on the view
 * user profile screen.
 */
object ViewUserScreenTags {
  const val PROFILE_PICTURE_TAG = "viewUserProfilePicture"
  const val AVATAR_LETTER_TAG = "viewUserAvatarLetter"
  const val USERNAME_TAG = "viewUserUsername"
  const val FRIEND_COUNT_TAG = "viewUserFriendCount"
  const val POSTS_SECTION_TAG = "viewUserPostsSection"
  const val POST_TAG = "viewUserPost"
  const val LOADING_TAG = "viewUserLoading"
  const val BACK_BUTTON_TAG = "viewUserBackButton"
  const val FOLLOW_BUTTON_TAG = "viewUserFollowButton"
}

/**
 * Main composable for viewing another user's profile.
 *
 * @param viewModel The ViewModel that manages the profile data and state
 * @param userId The unique identifier of the user whose profile is being viewed
 * @param onBackButton Callback invoked when the back button is pressed
 * @param onPostClick Callback invoked when a post is clicked, receives the post ID
 */
@Composable
fun ViewUserProfile(
    viewModel: ViewUserViewModel = viewModel(),
    userId: String,
    onBackButton: () -> Unit = {},
    onPostClick: (String) -> Unit = {}
) {
  val uiState by viewModel.uiState.collectAsState()
  val context = LocalContext.current

  LaunchedEffect(userId) { viewModel.update(userId) }

  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
  }
  if (uiState.isLoading) {
    LoadingScreen(modifier = Modifier.testTag(ViewUserScreenTags.LOADING_TAG))
  } else {
    ViewUserProfileContent(uiState, onBackButton, onPostClick)
  }
}

/**
 * Displays the main content of the user profile when data is loaded.
 *
 * @param uiState The current UI state containing user profile data
 * @param onBackButton Callback invoked when the back button is pressed
 * @param onPostClick Callback invoked when a post is clicked, receives the post ID
 */
@Composable
private fun ViewUserProfileContent(
    uiState: ViewUserData,
    onBackButton: () -> Unit,
    onPostClick: (String) -> Unit
) {
  val scrollState = rememberScrollState()

  val isFriendText = if (uiState.isFriend) "your" else "not your"
  val friendStatusText = "This user is $isFriendText friend"

  Column(
      modifier =
          Modifier.fillMaxSize()
              .background(colorScheme.background)
              .verticalScroll(scrollState)
              .padding(horizontal = 22.dp, vertical = 10.dp)) {
        // Back button with username as title
        OOTDTopBar(
            textModifier = Modifier.testTag(ViewUserScreenTags.USERNAME_TAG),
            centerText = "@${uiState.username}",
            leftComposable = {
              BackArrow(onBackButton, Modifier.testTag(ViewUserScreenTags.BACK_BUTTON_TAG))
            })

        Spacer(modifier = Modifier.height(36.dp))

        // Avatar
        val profilePic = uiState.profilePicture
        val tag =
            ViewUserScreenTags.PROFILE_PICTURE_TAG.takeIf { profilePic.isNotBlank() }
                ?: ViewUserScreenTags.AVATAR_LETTER_TAG
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
          ProfilePicture(
              modifier = Modifier.testTag(tag),
              size = 150.dp,
              profilePicture = uiState.profilePicture,
              username = uiState.username,
              textStyle = Typography.headlineLarge)
        }
        Spacer(modifier = Modifier.height(18.dp))

        // Follow Button
        ViewUserFollowButton(uiState.isFriend, onFollowClick = {})

        Spacer(modifier = Modifier.height(9.dp))

        ShowText(
            text = friendStatusText, style = Typography.bodyLarge, color = colorScheme.onSurface)

        Spacer(modifier = Modifier.height(9.dp))

        // Friend count
        ShowText(
            text = "${uiState.friendCount} friends",
            style = Typography.bodyLarge,
            color = colorScheme.onSurface,
            modifier = Modifier.testTag(ViewUserScreenTags.FRIEND_COUNT_TAG))

        Spacer(modifier = Modifier.height(30.dp))

        // Posts section
        if (uiState.isFriend) {
          val posts = uiState.friendPosts
          if (posts.isNotEmpty()) {
            ShowText(
                text = "${uiState.username}'s posts :",
                style = Typography.bodyLarge,
                textAlign = TextAlign.Left,
                modifier = Modifier.testTag(ViewUserScreenTags.POSTS_SECTION_TAG))

            Spacer(modifier = Modifier.height(16.dp))
          }

          DisplayUserPosts(
              posts = posts,
              onPostClick = onPostClick,
              modifier = Modifier.testTag(ViewUserScreenTags.POST_TAG),
              padding = 22.dp,
              spacing = 8.dp)
        } else {
          ShowText(
              text = "Add this user as a friend to see their posts",
              style = Typography.bodyMedium,
              textAlign = TextAlign.Center,
              color = colorScheme.onSurface,
              modifier = Modifier.testTag(ViewUserScreenTags.POSTS_SECTION_TAG))
        }
      }
}

/**
 * Displays a follow/unfollow button based on the current friend status.
 *
 * Shows "Unfollow" if the user is already a friend, "Follow" otherwise. The button is centered
 * horizontally on the screen.
 *
 * @param isFriend Whether the user is currently a friend
 * @param onFollowClick Callback invoked when the button is clicked
 */
@Composable
private fun ViewUserFollowButton(isFriend: Boolean, onFollowClick: () -> Unit) {
  Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    Button(
        onClick = onFollowClick,
        modifier = Modifier.testTag(ViewUserScreenTags.FOLLOW_BUTTON_TAG)) {
          Text(text = if (isFriend) "Unfollow" else "Follow")
        }
  }
}
