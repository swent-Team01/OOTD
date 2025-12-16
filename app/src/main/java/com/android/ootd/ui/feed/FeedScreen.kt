package com.android.ootd.ui.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.ootd.model.map.Location
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.ui.comments.CommentBottomSheet
import com.android.ootd.ui.comments.CommentViewModel
import com.android.ootd.ui.feed.FeedScreenTestTags.NAVIGATE_TO_NOTIFICATIONS_SCREEN
import com.android.ootd.ui.theme.Background
import com.android.ootd.ui.theme.OOTDTheme
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Typography
import com.android.ootd.utils.composables.LoadingScreen
import com.android.ootd.utils.composables.NotificationButton
import com.android.ootd.utils.composables.OOTDTabRow
import com.android.ootd.utils.composables.OOTDTopBar
import com.android.ootd.utils.composables.PullToRefresh
import com.android.ootd.utils.composables.ShowText

object FeedScreenTestTags {
  const val SCREEN = "feedScreen"
  const val TOP_BAR = "feedTopBar"
  const val LOCKED_MESSAGE = "feedLockedMessage"
  const val ADD_POST_FAB = "addPostFab"
  const val FEED_LIST = "feedList"
  const val NAVIGATE_TO_NOTIFICATIONS_SCREEN = "navigateToNotificationsScreen"
  const val LOADING_OVERLAY = "feedLoadingOverlay"
  const val REFRESHER = "feedRefresher"
}

/**
 * Main Feed Screen composable that displays the user's feed of outfit posts.
 *
 * @param feedViewModel ViewModel for managing feed state and actions.
 * @param commentViewModel ViewModel for managing comments state and actions.
 * @param onAddPostClick Callback when the add post button is clicked.
 * @param onNotificationIconClick Callback when the notification icon is clicked.
 * @param onOpenPost Callback when a post is opened.
 * @param onLocationClick Callback when a location is clicked.
 * @param onProfileClick Callback when a profile is clicked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    feedViewModel: FeedViewModel = viewModel(),
    commentViewModel: CommentViewModel = viewModel(),
    onAddPostClick: () -> Unit,
    onNotificationIconClick: () -> Unit = {},
    onOpenPost: (String) -> Unit = {},
    onLocationClick: (Location) -> Unit = {},
    onProfileClick: (String) -> Unit = {}
) {
  val uiState by feedViewModel.uiState.collectAsState()
  val hasPostedToday = uiState.hasPostedToday
  val posts = uiState.feedPosts
  // Tracks which post's comments are being viewed, set back to null when comments sheet is closed
  var selectedPostForComments by remember { mutableStateOf<OutfitPost?>(null) }
  val isRefreshing by feedViewModel.isRefreshing.collectAsState()

  LaunchedEffect(uiState.currentAccount?.uid, uiState.hasPostedToday, uiState.isPublicFeed) {
    // Refresh feed when account changes or feed type changes
    feedViewModel.doRefreshFeed()
  }

  FeedScaffold(
      hasPostedToday = hasPostedToday,
      posts = posts,
      isLoading = uiState.isLoading,
      errorMessage = uiState.errorMessage,
      onClearError = { feedViewModel.setErrorMessage(null) },
      onAddPostClick = onAddPostClick,
      onNotificationIconClick = onNotificationIconClick,
      onOpenPost = onOpenPost,
      onLocationClick = onLocationClick,
      likes = uiState.likes,
      likeCounts = uiState.likeCounts,
      onLikeClick = { post -> feedViewModel.onToggleLike(post.postUID) },
      isPublicFeed = uiState.isPublicFeed,
      onToggleFeed = { feedViewModel.toggleFeedType() },
      onProfileClick = onProfileClick,
      isRefreshing = isRefreshing,
      onRefresh = { feedViewModel.onPullToRefreshTrigger() },
      onCommentClick = { post -> selectedPostForComments = post })

  // Comments Bottom Sheet
  selectedPostForComments?.let { selectedPost ->
    val currentUserId = uiState.currentAccount?.uid ?: return@let
    val latestPosts = uiState.feedPosts
    val currentPost = latestPosts.find { it.postUID == selectedPost.postUID } ?: selectedPost

    CommentBottomSheet(
        post = currentPost,
        currentUserId = currentUserId,
        onDismiss = { selectedPostForComments = null },
        onCommentAdded = {
          // Refresh this specific post to show new comments
          feedViewModel.refreshPost(currentPost.postUID)
        },
        onProfileClick = onProfileClick,
        viewModel = commentViewModel)
  }
}

/**
 * Scaffold for the Feed screen, contains the top bar, FAB and the list of posts.
 *
 * @param hasPostedToday Whether the user has posted today.
 * @param posts The list of outfit posts to display.
 * @param isLoading Whether the feed is currently loading.
 * @param errorMessage An optional error message to display in a snackbar.
 * @param onClearError Callback to clear the error message.
 * @param onAddPostClick Callback when the add post FAB is clicked.
 * @param onNotificationIconClick Callback when the notification icon is clicked.
 * @param onOpenPost Callback when a post is opened.
 * @param onLocationClick Callback when a location is clicked.
 * @param likes Map of post IDs to like status.
 * @param likeCounts Map of post IDs to like counts.
 * @param onLikeClick Callback when a post is liked or unliked.
 * @param isPublicFeed Whether the current feed is the public feed.
 * @param onCommentClick Callback when the comment button is clicked.
 * @param onToggleFeed Callback to toggle between friends and public feed.
 * @param onProfileClick Callback when a profile is clicked.
 * @param isRefreshing Whether the feed is currently refreshing.
 * @param onRefresh Callback to refresh the feed.
 */
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
    onOpenPost: (String) -> Unit,
    onLocationClick: (Location) -> Unit = {},
    likes: Map<String, Boolean> = emptyMap(),
    likeCounts: Map<String, Int> = emptyMap(),
    onLikeClick: (OutfitPost) -> Unit = {},
    isPublicFeed: Boolean = false,
    onCommentClick: (OutfitPost) -> Unit = {},
    onToggleFeed: () -> Unit = {},
    onProfileClick: (String) -> Unit,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
  val snackBarHostState = remember { SnackbarHostState() }

  LaunchedEffect(errorMessage) {
    errorMessage?.let { message ->
      snackBarHostState.showSnackbar(message)
      onClearError()
    }
  }

  Scaffold(
      modifier = Modifier.testTag(FeedScreenTestTags.SCREEN),
      snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
      topBar = {
        Column {
          OOTDTopBar(
              modifier = Modifier.testTag(FeedScreenTestTags.TOP_BAR),
              rightComposable = {
                NotificationButton(
                    onNotificationIconClick,
                    Modifier.testTag(NAVIGATE_TO_NOTIFICATIONS_SCREEN),
                    size = 64.dp)
              })
          OOTDTabRow(
              selectedTabIndex = if (isPublicFeed) 1 else 0,
              tabs = listOf("Friends", "Public"),
              onTabClick = { index ->
                if (index == 0 && isPublicFeed) onToggleFeed()
                if (index == 1 && !isPublicFeed) onToggleFeed()
              })
        }
      },
      floatingActionButton = {
        if (!isLoading && !hasPostedToday) {

          ExtendedFloatingActionButton(
              onClick = onAddPostClick,
              containerColor = Primary,
              icon = { Icon(Icons.Filled.Add, "Add a new fit check") },
              text = { Text(text = "Do a Fit check", color = White) },
              modifier = Modifier.testTag(FeedScreenTestTags.ADD_POST_FAB))
        }
      }) { paddingValues ->
        // Overlay the locked message when needed
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding())
                    .background(Background)) {

              // Renders the list of posts when user has posted.
              FeedList(
                  isBlurred = !hasPostedToday,
                  posts = posts,
                  likes = likes,
                  likeCounts = likeCounts,
                  onPostClick = onOpenPost,
                  onLocationClick = onLocationClick,
                  onLikeClick = onLikeClick,
                  onCommentClick = onCommentClick,
                  onProfileClick = onProfileClick,
                  isRefreshing = isRefreshing,
                  onRefresh = onRefresh)

              // Loading overlay and hides it while refreshing
              if (isLoading && !isRefreshing) {
                AnimatedVisibility(visible = true) {
                  LoadingScreen(
                      modifier = Modifier.testTag(FeedScreenTestTags.LOADING_OVERLAY),
                      contentDescription = "Loading feed")
                }
              }

              if (!isLoading && !hasPostedToday && posts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().testTag(FeedScreenTestTags.LOCKED_MESSAGE),
                    contentAlignment = Alignment.Center) {
                      ShowText(
                          text = "Do a fit check to unlock today’s feed",
                          style = Typography.titleLarge)
                    }
              }
            }
      }
}

/**
 * Composable that displays a list of outfit posts with pull-to-refresh functionality.
 *
 * @param posts The list of outfit posts to display.
 * @param isBlurred Whether the posts should be displayed in a blurred state.
 * @param likes Map of post IDs to like status.
 * @param likeCounts Map of post IDs to like counts.
 * @param onLikeClick Callback when a post is liked or unliked.
 * @param onPostClick Callback when a post is clicked.
 * @param onLocationClick Callback when a location is clicked.
 * @param onCommentClick Callback when the comment button is clicked.
 * @param onProfileClick Callback when a profile is clicked.
 * @param isRefreshing Whether the feed is currently refreshing.
 * @param onRefresh Callback to refresh the feed.
 */
@Composable
fun FeedList(
    posts: List<OutfitPost>,
    isBlurred: Boolean,
    likes: Map<String, Boolean> = emptyMap(),
    likeCounts: Map<String, Int> = emptyMap(),
    onLikeClick: (OutfitPost) -> Unit = {},
    onPostClick: (String) -> Unit,
    onLocationClick: (Location) -> Unit = {},
    onCommentClick: (OutfitPost) -> Unit = {},
    onProfileClick: (String) -> Unit = {},
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
  // Pull to refresh layout
  PullToRefresh(
      modifier = Modifier.fillMaxSize().testTag(FeedScreenTestTags.REFRESHER),
      isRefreshing = isRefreshing,
      onRefresh = { onRefresh() },
      content = {
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
                onCardClick = { onPostClick(post.postUID) },
                onLocationClick = onLocationClick,
                onProfileClick = onProfileClick,
                onCommentClick = onCommentClick)
          }
        }
      })
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
        onLikeClick = {},
        onOpenPost = {},
        onProfileClick = {})
  }
}
