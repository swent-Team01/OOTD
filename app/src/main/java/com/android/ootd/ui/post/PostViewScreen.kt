package com.android.ootd.ui.post

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.ootd.ui.theme.*
import com.android.ootd.ui.theme.Background
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
              val post = uiState.post!!
              val defaultPainter = remember(Tertiary) { ColorPainter(Tertiary) }

              Column(
                  modifier = Modifier.fillMaxSize().padding(16.dp),
                  horizontalAlignment = Alignment.CenterHorizontally) {
                    // Display the post image
                    AsyncImage(
                        model = post.outfitURL,
                        contentDescription = "Post image",
                        placeholder = defaultPainter,
                        error = defaultPainter,
                        contentScale = ContentScale.Fit,
                        modifier =
                            Modifier.fillMaxWidth().weight(1f).testTag(PostViewTestTags.POST_IMAGE))

                    if (post.description.isNotBlank()) {
                      Spacer(modifier = Modifier.height(16.dp))
                      Text(
                          text = post.description,
                          style = MaterialTheme.typography.bodyLarge,
                          color = Primary,
                          modifier = Modifier.fillMaxWidth())
                    }
                  }
            }
          }
        }
      }
}
