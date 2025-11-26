package com.android.ootd.ui.post

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.ootd.ui.theme.*
import com.android.ootd.ui.theme.Background
import com.android.ootd.utils.ProfilePicture

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
  val colors = MaterialTheme.colorScheme

  Scaffold(
      modifier = Modifier.fillMaxSize().testTag(PostViewTestTags.SCREEN),
      containerColor = Background,
      topBar = {
        TopAppBar(
            title = { Text("Post", color = Primary) },
            navigationIcon = {
              IconButton(
                  onClick = onBack, modifier = Modifier.testTag(PostViewTestTags.BACK_BUTTON)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.onBackground)
                  }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Background,
                    titleContentColor = Primary,
                    navigationIconContentColor = Primary),
            modifier = Modifier.testTag(PostViewTestTags.TOP_BAR))
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
  }
}

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

@Composable
fun PostImage(imageUrl: String) {
  AsyncImage(
      model = imageUrl,
      contentDescription = "Post image",
      contentScale = ContentScale.Fit, // show full image, no cropping
      modifier = Modifier.fillMaxWidth().height(300.dp).testTag(PostViewTestTags.POST_IMAGE))
}

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

@Composable
fun PostDescription(description: String) {
  if (description.isNotBlank()) {
    Text(
        text = description,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.primary)
  }
}
