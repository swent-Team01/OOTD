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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.ootd.R
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.ui.theme.Bodoni
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Secondary

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
    ViewUserLoadingOverlay()
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
        ViewUserBackButton(onBackButton, uiState.username, Modifier.align(Alignment.Start))

        Spacer(modifier = Modifier.height(36.dp))

        // Avatar
        ViewUserAvatar(uiState)

        Spacer(modifier = Modifier.height(18.dp))

        // Follow Button
        ViewUserFollowButton(uiState.isFriend, onFollowClick = {})

        Spacer(modifier = Modifier.height(9.dp))

        ViewUserText(text = friendStatusText, style = typography.bodyLarge)

        Spacer(modifier = Modifier.height(9.dp))

        // Friend count
        ViewUserText(
            text = "${uiState.friendCount} friends",
            style = typography.bodyLarge,
            testTag = ViewUserScreenTags.FRIEND_COUNT_TAG)

        Spacer(modifier = Modifier.height(30.dp))

        // Posts section
        if (uiState.isFriend) {
          ViewUserText(
              text = "Posts :",
              style = typography.bodyLarge,
              textAlign = TextAlign.Left,
              fontFamily = Bodoni,
              testTag = ViewUserScreenTags.POSTS_SECTION_TAG)

          Spacer(modifier = Modifier.height(16.dp))

          ViewUserPosts(uiState.friendPosts, onPostClick)
        } else {
          ViewUserText(
              text = "Add this user as a friend to see their posts",
              style = typography.bodyMedium,
              textAlign = TextAlign.Center,
              testTag = ViewUserScreenTags.POSTS_SECTION_TAG)
        }
      }
}

/**
 * Reusable text component for displaying styled text in the user profile.
 *
 * A helper composable that provides consistent text styling across the profile screen with
 * customizable properties.
 *
 * @param text The text content to display
 * @param style The text style to apply
 * @param modifier Additional modifier for the text component
 * @param color The color of the text, defaults to onSurface from the theme
 * @param textAlign Text alignment, defaults to center
 * @param fontFamily Optional custom font family
 * @param testTag Optional test tag for UI testing
 */
@Composable
private fun ViewUserText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = colorScheme.onSurface,
    textAlign: TextAlign = TextAlign.Center,
    fontFamily: androidx.compose.ui.text.font.FontFamily? = null,
    testTag: String = ""
) {
  Text(
      text = text,
      style = style,
      color = color,
      textAlign = textAlign,
      fontFamily = fontFamily,
      modifier = modifier.fillMaxWidth().testTag(testTag))
}

/**
 * Displays a grid of the user's outfit posts.
 *
 * Shows posts in a 3-column grid layout with consistent spacing. Each post image is clickable and
 * navigates to the full post view. The grid height is calculated based on the number of posts to
 * ensure proper scrolling behavior.
 *
 * @param posts List of outfit posts to display
 * @param onPostClick Callback invoked when a post is clicked, receives the post ID
 */
@Composable
private fun ViewUserPosts(posts: List<OutfitPost>, onPostClick: (String) -> Unit) {
  val color = colorScheme
  val defaultPainter = remember(color.tertiary) { ColorPainter(color.tertiary) }
  val configuration = LocalConfiguration.current
  val screenWidth = configuration.screenWidthDp.dp
  val itemWidth = (screenWidth - 44.dp - 16.dp) / 3 // subtract padding and spacing
  val itemHeight = itemWidth
  val rowCount = (posts.size + 2) / 3
  val totalHeight = rowCount * itemHeight.value + (rowCount - 1) * 8

  LazyVerticalGrid(
      columns = GridCells.Fixed(3),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth().height(totalHeight.dp)) {
        items(posts) { post ->
          AsyncImage(
              model = post.outfitURL,
              contentDescription = "Post image",
              placeholder = defaultPainter,
              error = defaultPainter,
              contentScale = ContentScale.Crop,
              modifier =
                  Modifier.size(itemWidth)
                      .clip(RoundedCornerShape(8.dp))
                      .clickable(onClick = { onPostClick(post.postUID) })
                      .background(color.surfaceVariant)
                      .testTag(ViewUserScreenTags.POST_TAG))
        }
      }
}

/**
 * Displays a loading overlay while the user profile data is being fetched.
 *
 * Shows a branded loading indicator with the app's hanger icon and a circular progress indicator.
 * The overlay covers the entire screen with a semi-transparent background.
 */
@Composable
private fun ViewUserLoadingOverlay() {
  Box(
      modifier =
          Modifier.fillMaxSize()
              .background(colorScheme.background.copy(alpha = 0.95f))
              .testTag(ViewUserScreenTags.LOADING_TAG),
      contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Image(
              painter = painterResource(id = R.drawable.hanger),
              contentDescription = "Loading profile",
              modifier = Modifier.size(72.dp))
          Spacer(modifier = Modifier.height(16.dp))
          CircularProgressIndicator(color = colorScheme.primary)
        }
      }
}

/**
 * Displays the user's profile avatar.
 *
 * Shows the user's profile picture if available, otherwise displays a circular avatar with the
 * first letter of their username. The avatar is rendered as a 150dp circle with appropriate
 * styling.
 *
 * @param uiState The current UI state containing user profile information
 */
@Composable
private fun ViewUserAvatar(uiState: ViewUserData) {
  val profilePicture = uiState.profilePicture
  val username = uiState.username
  val color = colorScheme
  val defaultAvatarPainter = remember(color.tertiary) { ColorPainter(color.tertiary) }

  Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    if (profilePicture.isNotBlank() && !uiState.isLoading) {
      AsyncImage(
          model = profilePicture,
          contentDescription = "Profile Picture",
          placeholder = defaultAvatarPainter,
          error = defaultAvatarPainter,
          contentScale = ContentScale.Crop,
          modifier =
              Modifier.size(150.dp)
                  .clip(CircleShape)
                  .testTag(ViewUserScreenTags.PROFILE_PICTURE_TAG))
    } else {
      Box(
          modifier = Modifier.size(150.dp).clip(CircleShape).background(Primary),
          contentAlignment = Alignment.Center) {
            Text(
                text = username.firstOrNull()?.uppercase() ?: "",
                style = typography.headlineLarge,
                color = Secondary,
                modifier = Modifier.testTag(ViewUserScreenTags.AVATAR_LETTER_TAG))
          }
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

/**
 * Displays a back button and the username title.
 *
 * The back button is positioned on the left side, while the username (prefixed with @) is displayed
 * as a centered title using the app's primary color and Bodoni font.
 *
 * @param onBackButton Callback invoked when the back button is pressed
 * @param username The username to display as the title
 * @param modifier Modifier to apply to the back button icon
 */
@Composable
private fun ViewUserBackButton(onBackButton: () -> Unit, username: String, modifier: Modifier) {
  IconButton(
      onClick = onBackButton, modifier = modifier.testTag(ViewUserScreenTags.BACK_BUTTON_TAG)) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = colorScheme.onBackground,
            modifier = Modifier.size(32.dp))
      }

  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "@$username",
            style = typography.displayMedium,
            fontFamily = Bodoni,
            color = colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(ViewUserScreenTags.USERNAME_TAG))
      }
}
