package com.android.ootd.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.android.ootd.model.post.OutfitPost

object OutfitPostCardTestTags {
  const val OUTFIT_POST_CARD = "outfitPostCard"
  const val POST_USERNAME = "postUsername"
  const val POST_IMAGE_PLACEHOLDER = "postImagePlaceholder"
  const val POST_DESCRIPTION = "postDescription"
  const val SEE_FIT_BUTTON = "seeFitButton"
}

@Composable
fun OutfitPostCard(
    post: OutfitPost,
    isBlurred: Boolean, // will add blur later, dependencies need to be changed to include coil
    modifier: Modifier = Modifier,
    onSeeFitClick: () -> Unit = {}
) {
  Card(
      modifier =
          modifier.padding(8.dp).fillMaxWidth().testTag(OutfitPostCardTestTags.OUTFIT_POST_CARD)) {
        Column(Modifier.padding(16.dp)) {
          Text(
              text = post.name,
              style = MaterialTheme.typography.titleMedium,
              modifier = Modifier.testTag(OutfitPostCardTestTags.POST_USERNAME))
          Spacer(modifier = Modifier.height(8.dp))

          // just a placeholder for now, will add the real photo later
          Box(
              modifier =
                  Modifier.fillMaxWidth()
                      .height(200.dp)
                      .background(MaterialTheme.colorScheme.surfaceVariant)
                      .testTag(OutfitPostCardTestTags.POST_IMAGE_PLACEHOLDER)) {
                Text(text = "Image placeholder", modifier = Modifier.align(Alignment.Center))
              }

          if (post.description.isNotBlank()) {
            Text(
                text = post.description,
                modifier = Modifier.testTag(OutfitPostCardTestTags.POST_DESCRIPTION))
            Spacer(modifier = Modifier.height(8.dp))
          }

          Spacer(modifier = Modifier.height(8.dp))
          Button(
              onClick = onSeeFitClick,
              modifier = Modifier.testTag(OutfitPostCardTestTags.SEE_FIT_BUTTON)) {
                Text("See fit")
              }
        }
      }
}
