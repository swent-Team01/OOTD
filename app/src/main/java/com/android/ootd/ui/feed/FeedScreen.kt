package com.android.ootd.ui.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel

object FeedScreenTestTags {
  const val SCREEN = "feedScreen"
  const val TOP_BAR = "feedTopBar"
  const val LOCKED_MESSAGE = "feedLockedMessage"
  const val ADD_POST_FAB = "addPostFab"
  const val FEED_LIST = "feedList"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(feedViewModel: FeedViewModel = viewModel(), onAddPostClick: () -> Unit) {
  val hasPostedToday by feedViewModel.hasPostedToday.collectAsState()
  val posts by feedViewModel.feedPosts.collectAsState()

  Scaffold(
      modifier = Modifier.testTag(FeedScreenTestTags.SCREEN),
      topBar = {
        TopAppBar(
            title = { Text("OOTD Feed") }, modifier = Modifier.testTag(FeedScreenTestTags.TOP_BAR))
      },
      floatingActionButton = {
        if (!hasPostedToday) {
          FloatingActionButton(
              onClick = onAddPostClick,
              modifier = Modifier.testTag(FeedScreenTestTags.ADD_POST_FAB)) {
                Text("Do a Fit Check")
              }
        }
      }) { paddingValues ->
        if (!hasPostedToday) {
          Box(
              modifier =
                  Modifier.fillMaxSize()
                      .padding(paddingValues)
                      .testTag(FeedScreenTestTags.LOCKED_MESSAGE),
              contentAlignment = Alignment.Center) {
                Text(
                    "Do a fit check to unlock today’s feed",
                    style = MaterialTheme.typography.titleMedium)
              }
        } else {
          // Show feed posts after user has posted today
          LazyColumn(
              modifier = Modifier.padding(paddingValues).testTag(FeedScreenTestTags.FEED_LIST)) {
                items(posts) { post ->
                  OutfitPostCard(
                      post = post,
                      isBlurred = false, // no blur for now
                      onSeeFitClick = { /* TODO: navigation to feeditems */})
                }
              }
        }
      }
}
