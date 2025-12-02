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
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.ui.feed.FeedScreenTestTags.NAVIGATE_TO_NOTIFICATIONS_SCREEN
import com.android.ootd.ui.theme.Background
import com.android.ootd.ui.theme.OOTDTheme
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Secondary
import com.android.ootd.ui.theme.Typography
import com.android.ootd.utils.composables.LoadingScreen
import com.android.ootd.utils.composables.NotificationButton
import com.android.ootd.utils.composables.OOTDTopBar
import com.android.ootd.utils.composables.ShowText

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
    onSeeFitClick: (String) -> Unit = {},
    onOpenPost: (String) -> Unit = {}
) {
  val uiState by feedViewModel.uiState.collectAsState()
  val hasPostedToday = uiState.hasPostedToday
  val posts = uiState.feedPosts

  LaunchedEffect(uiState.currentAccount?.uid, uiState.hasPostedToday, uiState.isPublicFeed) {
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
      onOpenPost = onOpenPost,
      likes = uiState.likes,
      likeCounts = uiState.likeCounts,
      onLikeClick = { post -> feedViewModel.onToggleLike(post.postUID) },
      isPublicFeed = uiState.isPublicFeed,
      onToggleFeed = { feedViewModel.toggleFeedType() },
      userDataMap = uiState.userDataMap)
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
    onOpenPost: (String) -> Unit,
    likes: Map<String, Boolean> = emptyMap(),
    likeCounts: Map<String, Int> = emptyMap(),
    onLikeClick: (OutfitPost) -> Unit = {},
    isPublicFeed: Boolean = false,
    onToggleFeed: () -> Unit = {},
    userDataMap: Map<String, UserFeedData> = emptyMap(),
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
        Column {
          OOTDTopBar(
              modifier = Modifier.testTag(FeedScreenTestTags.TOP_BAR),
              rightComposable = {
                NotificationButton(
                    onNotificationIconClick,
                    Modifier.testTag(NAVIGATE_TO_NOTIFICATIONS_SCREEN),
                    size = 64.dp)
              })
          PrimaryTabRow(
              selectedTabIndex = if (isPublicFeed) 1 else 0,
              containerColor = White,
              contentColor = Secondary,
              indicator = {
                TabRowDefaults.PrimaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(if (isPublicFeed) 1 else 0),
                    color = Primary)
              }) {
                Tab(
                    selected = !isPublicFeed,
                    onClick = { if (isPublicFeed) onToggleFeed() },
                    text = {
                      Text(
                          "Friends",
                          style = MaterialTheme.typography.titleLarge,
                          fontWeight = if (!isPublicFeed) FontWeight.Bold else FontWeight.Normal)
                    },
                    selectedContentColor = Primary,
                    unselectedContentColor = Black)
                Tab(
                    selected = isPublicFeed,
                    onClick = { if (!isPublicFeed) onToggleFeed() },
                    text = {
                      Text(
                          "Public",
                          style = MaterialTheme.typography.titleLarge,
                          fontWeight = if (isPublicFeed) FontWeight.Bold else FontWeight.Normal)
                    },
                    selectedContentColor = Primary,
                    unselectedContentColor = Black)
              }
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
                  userDataMap = userDataMap,
                  onSeeFitClick = { post -> onSeeFitClick(post.postUID) },
                  onPostClick = onOpenPost,
                  onLikeClick = onLikeClick)

              // Loading overlay
              if (isLoading) {
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
                          style = Typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold))
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
    userDataMap: Map<String, UserFeedData>,
    onSeeFitClick: (OutfitPost) -> Unit = {},
    onLikeClick: (OutfitPost) -> Unit = {},
    onPostClick: (String) -> Unit
) {
  LazyColumn(modifier = Modifier.fillMaxSize().testTag(FeedScreenTestTags.FEED_LIST)) {
    items(posts) { post ->
      val isLiked = likes[post.postUID] ?: false
      val count = likeCounts[post.postUID] ?: 0

      // Get user data for this post owner, fallback to post data if missing
      val userData = userDataMap[post.ownerId]
      val username = userData?.username ?: post.name
      val profilePic = userData?.profilePicUrl ?: post.userProfilePicURL

      OutfitPostCard(
          post = post,
          username = username,
          profilePicUrl = profilePic,
          isBlurred = isBlurred,
          isLiked = isLiked,
          likeCount = count,
          onLikeClick = { onLikeClick(post) },
          onSeeFitClick = { onSeeFitClick(post) },
          onCardClick = { onPostClick(post.postUID) })
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
        onLikeClick = {},
        onOpenPost = {})
  }
}
