package com.android.ootd.ui.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
fun FeedScreen(
    feedViewModel: FeedViewModel = viewModel(),
    onAddPostClick: () -> Unit,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit
) {
  val uiState by feedViewModel.uiState.collectAsState()
  val hasPostedToday = uiState.hasPostedToday
  val posts = uiState.feedPosts

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
                          fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
            },
            navigationIcon = {
              IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.tertiary)
              }
            },
            actions = {
              // TODO: replace this icon with user profile avatar once implemented
              IconButton(onClick = onProfileClick) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Profile",
                    tint = MaterialTheme.colorScheme.primary)
              }
            },
            colors =
                TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background))
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
        // Use a single Box and overlay the locked message when needed.
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
          // Feed list is always part of the layout; it will show items when available.
          LazyColumn(modifier = Modifier.fillMaxSize().testTag(FeedScreenTestTags.FEED_LIST)) {
            items(posts) { post ->
              OutfitPostCard(
                  post = post,
                  isBlurred = false,
                  onSeeFitClick = { /* TODO: navigation to feeditems */})
              // no blur for now
            }
          }

          if (!hasPostedToday) {
            Box(
                modifier = Modifier.fillMaxSize().testTag(FeedScreenTestTags.LOCKED_MESSAGE),
                contentAlignment = Alignment.Center) {
                  Text(
                      "Do a fit check to unlock today’s feed",
                      style = MaterialTheme.typography.titleMedium)
                }
          }
        }
      }
}
