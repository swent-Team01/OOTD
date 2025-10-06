package com.android.ootd.ui.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(feedViewModel: FeedViewModel = viewModel(), onAddPostClick: () -> Unit) {
  val hasPostedToday by feedViewModel.hasPostedToday.collectAsState()
  val posts by feedViewModel.feedPosts.collectAsState()

  Scaffold(
      topBar = { TopAppBar(title = { Text("OOTD Feed") }) },
      floatingActionButton = {
        if (!hasPostedToday) {
          FloatingActionButton(onClick = onAddPostClick) { Text("Do a Fit Check") }
        }
      }) { paddingValues ->
        if (!hasPostedToday) {
          Box(
              modifier = Modifier.fillMaxSize().padding(paddingValues),
              contentAlignment = Alignment.Center) {
                Text(
                    "Do a fit check to unlock today’s feed",
                    style = MaterialTheme.typography.titleMedium)
              }
        } else {
          // Show feed posts after user has posted today
          LazyColumn(modifier = Modifier.padding(paddingValues)) {
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
