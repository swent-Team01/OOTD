package com.android.ootd.ui.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.ootd.R
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.ui.feed.FeedScreenTestTags.NAVIGATE_TO_NOTIFICATIONS_SCREEN
import com.android.ootd.ui.theme.OOTDTheme

object FeedScreenTestTags {
  const val SCREEN = "feedScreen"
  const val TOP_BAR = "feedTopBar"
  const val LOCKED_MESSAGE = "feedLockedMessage"
  const val ADD_POST_FAB = "addPostFab"
  const val FEED_LIST = "feedList"
  const val NAVIGATE_TO_NOTIFICATIONS_SCREEN = "navigateToNotificationsScreen"
  const val LOADING_OVERLAY = "feedLoadingOverlay"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    feedViewModel: FeedViewModel = viewModel(),
    onAddPostClick: () -> Unit,
    onNotificationIconClick: () -> Unit = {},
    onSeeFitClick: (String) -> Unit = {}
) {
  val uiState by feedViewModel.uiState.collectAsState()
  val hasPostedToday = uiState.hasPostedToday
  val posts = uiState.feedPosts

  LaunchedEffect(uiState.currentAccount?.uid, uiState.hasPostedToday) {
    feedViewModel.refreshFeedFromFirestore()
  }

  FeedScaffold(
      hasPostedToday = hasPostedToday,
      posts = posts,
      isLoading = uiState.isLoading,
      errorMessage = uiState.errorMessage,
      onClearError = { feedViewModel.setErrorMessage(null) },
      onAddPostClick = onAddPostClick,
      onNotificationIconClick = onNotificationIconClick,
      onSeeFitClick = onSeeFitClick,
      likes = uiState.likes,
      likeCounts = uiState.likeCounts,
      onLikeClick = { post -> feedViewModel.onToggleLike(post.postUID) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedScaffold(
    hasPostedToday: Boolean,
    posts: List<OutfitPost>,
    isLoading: Boolean,
    errorMessage: String?,
    onClearError: () -> Unit,
    onAddPostClick: () -> Unit,
    onNotificationIconClick: () -> Unit = {},
    onSeeFitClick: (String) -> Unit = {},
    likes: Map<String, Boolean> = emptyMap(),
    likeCounts: Map<String, Int> = emptyMap(),
    onLikeClick: (OutfitPost) -> Unit = {}
) {
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(errorMessage) {
    errorMessage?.let { message ->
      snackbarHostState.showSnackbar(message)
      onClearError()
    }
  }

  Scaffold(
      modifier = Modifier.testTag(FeedScreenTestTags.SCREEN),
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      topBar = {
        CenterAlignedTopAppBar(
            modifier = Modifier.testTag(FeedScreenTestTags.TOP_BAR),
            title = {
              Text(
                  text = "OOTD",
                  style =
                      MaterialTheme.typography.displayLarge.copy(
                          fontWeight = FontWeight.ExtraBold,
                          color = MaterialTheme.colorScheme.primary))
            },
            actions = {
              IconButton(
                  onClick = onNotificationIconClick,
                  modifier = Modifier.testTag(NAVIGATE_TO_NOTIFICATIONS_SCREEN)) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_notification),
                        contentDescription = "Notifications",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp))
                  }
            },
            colors =
                TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background))
      },
      floatingActionButton = {
        if (!isLoading && !hasPostedToday) {
          Button(
              onClick = onAddPostClick,
              colors =
                  ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
              modifier = Modifier.testTag(FeedScreenTestTags.ADD_POST_FAB)) {
                Text(
                    "Do a Fit Check",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge)
              }
        }
      }) { paddingValues ->
        // Overlay the locked message when needed
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding())
                    .background(MaterialTheme.colorScheme.background)) {
              // Renders the list of posts when user has posted.
              FeedList(
                  isBlurred = !hasPostedToday,
                  posts = posts,
                  likes = likes,
                  likeCounts = likeCounts,
                  onSeeFitClick = { post -> onSeeFitClick(post.postUID) },
                  onLikeClick = onLikeClick)

              // Loading overlay
              if (isLoading) {
                AnimatedVisibility(visible = isLoading) { FeedLoadingOverlay() }
              }

              if (!isLoading && !hasPostedToday && posts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().testTag(FeedScreenTestTags.LOCKED_MESSAGE),
                    contentAlignment = Alignment.Center) {
                      Text(
                          "Do a fit check to unlock today’s feed",
                          style =
                              MaterialTheme.typography.titleLarge.copy(
                                  fontWeight = FontWeight.ExtraBold),
                          color = MaterialTheme.colorScheme.primary)
                    }
              }
            }
      }
}

@Composable
fun FeedList(
    posts: List<OutfitPost>,
    isBlurred: Boolean,
    likes: Map<String, Boolean> = emptyMap(),
    likeCounts: Map<String, Int> = emptyMap(),
    onSeeFitClick: (OutfitPost) -> Unit = {},
    onLikeClick: (OutfitPost) -> Unit = {}
) {
  LazyColumn(modifier = Modifier.fillMaxSize().testTag(FeedScreenTestTags.FEED_LIST)) {
    items(posts) { post ->
      val isLiked = likes[post.postUID] ?: false
      val count = likeCounts[post.postUID] ?: 0

      OutfitPostCard(
          post = post,
          isBlurred = isBlurred,
          isLiked = isLiked,
          likeCount = count,
          onLikeClick = { onLikeClick(post) },
          onSeeFitClick = { onSeeFitClick(post) })
    }
  }
}

@Composable
fun FeedLoadingOverlay() {
  Box(
      modifier =
          Modifier.fillMaxSize()
              .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
              .testTag(FeedScreenTestTags.LOADING_OVERLAY),
      contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Image(
              painter = painterResource(id = R.drawable.hanger),
              contentDescription = "Loading feed",
              modifier = Modifier.size(72.dp))
          Spacer(modifier = Modifier.height(16.dp))
          CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
      }
}

@Preview(showBackground = true)
@Composable
@Suppress("UnusedPrivateMember")
fun FeedScreenPreview() {
  val samplePosts =
      listOf(
          OutfitPost(
              postUID = "1",
              ownerId = "user1",
              name = "Alice",
              userProfilePicURL = "",
              outfitURL = "",
              description = "Casual fit",
              itemsID = emptyList(),
              timestamp = System.currentTimeMillis()),
          OutfitPost(
              postUID = "2",
              ownerId = "user2",
              name = "Bob",
              userProfilePicURL = "",
              outfitURL = "",
              description = "Streetwear",
              itemsID = emptyList(),
              timestamp = System.currentTimeMillis()))

  OOTDTheme {
    FeedScaffold(
        hasPostedToday = false,
        posts = samplePosts,
        isLoading = false,
        errorMessage = null,
        onClearError = {},
        onAddPostClick = {},
        onNotificationIconClick = {},
        likes = emptyMap(),
        likeCounts = emptyMap(),
        onLikeClick = {})
  }
}
