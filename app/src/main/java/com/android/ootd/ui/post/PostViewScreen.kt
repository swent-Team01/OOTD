package com.android.ootd.ui.post

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.ootd.model.user.User
import com.android.ootd.ui.theme.*
import com.android.ootd.ui.theme.Background
import com.android.ootd.utils.ProfilePicture
import com.android.ootd.utils.composables.BackArrow
import com.android.ootd.utils.composables.OOTDTopBar

object PostViewTestTags {
  const val SCREEN = "postViewScreen"
  const val TOP_BAR = "postViewTopBar"
  const val BACK_BUTTON = "postViewBackButton"
  const val POST_IMAGE = "postViewImage"
  const val LOADING_INDICATOR = "postViewLoading"
  const val ERROR_TEXT = "postViewError"
}

/**
 * Screen for viewing a single post in full detail
 *
 * @param postId The unique identifier of the post to display
 * @param onBack Callback to navigate back
 * @param viewModel ViewModel for managing post view state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostViewScreen(
    postId: String,
    onBack: () -> Unit,
    viewModel: PostViewViewModel = viewModel(factory = PostViewViewModelFactory(postId))
) {
  val uiState by viewModel.uiState.collectAsState()

  LaunchedEffect(postId) { viewModel.loadPost(postId) }

  Scaffold(
      modifier = Modifier.fillMaxSize().testTag(PostViewTestTags.SCREEN),
      containerColor = Background,
      topBar = {
        OOTDTopBar(
            modifier = Modifier.testTag(PostViewTestTags.TOP_BAR),
            centerText = "Post",
            leftComposable = {
              BackArrow(
                  onBackClick = onBack, modifier = Modifier.testTag(PostViewTestTags.BACK_BUTTON))
            })
      }) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues).background(Background)) {
          when {
            uiState.isLoading -> {
              CircularProgressIndicator(
                  color = Primary,
                  modifier =
                      Modifier.align(Alignment.Center).testTag(PostViewTestTags.LOADING_INDICATOR))
            }
            uiState.error != null -> {
              Text(
                  text = uiState.error ?: "Unknown error",
                  color = MaterialTheme.colorScheme.error,
                  modifier =
                      Modifier.align(Alignment.Center)
                          .padding(16.dp)
                          .testTag(PostViewTestTags.ERROR_TEXT))
            }
            uiState.post != null -> {
              PostDetailsContent(
                  uiState = uiState,
                  onToggleLike = { viewModel.toggleLike() },
                  modifier = Modifier.fillMaxSize())
            }
          }
        }
      }
}

/**
 * Composable displaying the details of a post
 *
 * @ param uiState The current UI state of the post view @ param onToggleLike Callback when the like
 * button is toggled @ param modifier Modifier for styling
 */
@Composable
fun PostDetailsContent(
    uiState: PostViewUiState,
    onToggleLike: () -> Unit,
    modifier: Modifier = Modifier
) {
  val post = uiState.post!!

  Column(modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
    PostOwnerSection(username = uiState.ownerUsername, profilePicture = uiState.ownerProfilePicture)

    Spacer(Modifier.height(16.dp))

    PostImage(imageUrl = post.outfitURL)

    Spacer(Modifier.height(16.dp))

    PostDescription(post.description)

    Spacer(Modifier.height(16.dp))

    PostLikeRow(
        isLiked = uiState.isLikedByCurrentUser,
        likeCount = uiState.likedByUsers.size,
        onToggleLike = onToggleLike)

    LikedUsersRow(likedUsers = uiState.likedByUsers)
  }
}

/**
 * Composable displaying the post owner's profile picture and username
 *
 * @ param username The username of the post owner @ param profilePicture The URL of the post
 * owner's profile picture
 */
@Composable
fun PostOwnerSection(
    username: String?,
    profilePicture: String?,
) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    ProfilePicture(
        size = 48.dp,
        profilePicture = profilePicture ?: "",
        username = username ?: "",
        textStyle = MaterialTheme.typography.titleMedium)

    Spacer(Modifier.width(12.dp))

    Text(
        text = username ?: "Unknown User",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary)
  }
}

/**
 * Composable displaying the post image
 *
 * @ param imageUrl The URL of the post image
 */
@Composable
fun PostImage(imageUrl: String) {
  AsyncImage(
      model = imageUrl,
      contentDescription = "Post image",
      contentScale = ContentScale.Fit, // show full image, no cropping
      modifier = Modifier.fillMaxWidth().height(300.dp).testTag(PostViewTestTags.POST_IMAGE))
}

/**
 * Composable displaying the like button and like count
 *
 * @ param isLiked Whether the post is liked by the current user @ param likeCount The total number
 * of likes on the post @ param onToggleLike Callback when the like button is toggled
 */
@Composable
fun PostLikeRow(isLiked: Boolean, likeCount: Int, onToggleLike: () -> Unit) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    IconButton(onClick = onToggleLike) {
      Icon(
          imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
          tint =
              if (isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
          contentDescription = if (isLiked) "Unlike" else "Like")
    }

    Text(
        text = "$likeCount likes",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface)
  }
}

/**
 * Composable displaying the post description
 *
 * @ param description The description text of the post
 */
@Composable
fun PostDescription(description: String) {
  if (description.isNotBlank()) {
    Text(
        text = description,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.primary)
  }
}

/**
 * Composable displaying a horizontal row of users who liked the post
 *
 * @ param likedUsers List of users who liked the post
 */
@Composable
fun LikedUsersRow(likedUsers: List<User>) {
  LazyRow(
      horizontalArrangement = Arrangement.spacedBy(16.dp),
      modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        items(likedUsers.size) { index ->
          val user = likedUsers[index]

          Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier.width(64.dp)) {
                ProfilePicture(
                    size = 48.dp,
                    profilePicture = user.profilePicture,
                    username = user.username,
                    textStyle = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = user.username,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center)
              }
        }
      }
}
