package com.android.ootd.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.ootd.R
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.ui.feed.FeedScreenTestTags.NAVIGATE_TO_NOTIFICATIONS_SCREEN
import com.android.ootd.ui.feed.FeedScreenTestTags.NAVIGATE_TO_SEARCH_SCREEN

object FeedScreenTestTags {
  const val SCREEN = "feedScreen"
  const val TOP_BAR = "feedTopBar"
  const val LOCKED_MESSAGE = "feedLockedMessage"
  const val ADD_POST_FAB = "addPostFab"
  const val FEED_LIST = "feedList"
  const val NAVIGATE_TO_SEARCH_SCREEN = "navigateToSearchScreen"
  const val NAVIGATE_TO_NOTIFICATIONS_SCREEN = "navigateToNotificationsScreen"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    feedViewModel: FeedViewModel = viewModel(),
    onAddPostClick: () -> Unit,
    onSearchClick: () -> Unit = {},
    onNotificationIconClick: () -> Unit = {},
    onSeeFitClick: (String) -> Unit = {}
) {
  val uiState by feedViewModel.uiState.collectAsState()
  val hasPostedToday = uiState.hasPostedToday
  val posts = uiState.feedPosts

  LaunchedEffect(uiState.currentAccount?.uid, uiState.hasPostedToday) {
    feedViewModel.refreshFeedFromFirestore()
  }

  Scaffold(
      modifier = Modifier.testTag(FeedScreenTestTags.SCREEN),
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
            navigationIcon = {
              IconButton(
                  onClick = onSearchClick, modifier = Modifier.testTag(NAVIGATE_TO_SEARCH_SCREEN)) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.tertiary)
                  }
            },
            actions = {
              IconButton(
                  onClick = onNotificationIconClick,
                  modifier = Modifier.testTag(NAVIGATE_TO_NOTIFICATIONS_SCREEN)) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_notification),
                        contentDescription = "Notifications",
                        tint = MaterialTheme.colorScheme.tertiary)
                  }
            },
            colors =
                TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background))
      },
      floatingActionButton = {
        if (!hasPostedToday) {
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
                  onSeeFitClick = { post -> onSeeFitClick(post.postUID) })

              if (!hasPostedToday && posts.isEmpty()) {
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
    onSeeFitClick: (OutfitPost) -> Unit = {}
) {
  LazyColumn(modifier = Modifier.fillMaxSize().testTag(FeedScreenTestTags.FEED_LIST)) {
    items(posts) { post ->
      OutfitPostCard(post = post, isBlurred, onSeeFitClick = { onSeeFitClick(post) })
    }
  }
}
