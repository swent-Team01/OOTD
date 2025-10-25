package com.android.ootd.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.ui.theme.OOTDTheme

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
          modifier.padding(8.dp).fillMaxWidth().testTag(OutfitPostCardTestTags.OUTFIT_POST_CARD),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
      elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
          // TODO: Add user profile avatar once profile page is implemented

          Text(
              text = post.name,
              style = MaterialTheme.typography.titleLarge,
              modifier = Modifier.testTag(OutfitPostCardTestTags.POST_USERNAME))
          Spacer(modifier = Modifier.height(8.dp))

          // just a placeholder for now, will add the real photo later
          Box(
              modifier =
                  Modifier.fillMaxWidth()
                      .height(260.dp)
                      .background(MaterialTheme.colorScheme.surfaceVariant)
                      .clip(RoundedCornerShape(12.dp))
                      .testTag(OutfitPostCardTestTags.POST_IMAGE_PLACEHOLDER),
              contentAlignment = Alignment.Center) {
                Text(text = "Image placeholder", modifier = Modifier.align(Alignment.Center))
              }

          if (post.description.isNotBlank()) {
            Text(
                text = "${post.name}: ${post.description}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.testTag(OutfitPostCardTestTags.POST_DESCRIPTION))
            Spacer(modifier = Modifier.height(8.dp))
          }

          Row(
              modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
              horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = onSeeFitClick,
                    shape = RoundedCornerShape(50),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary),
                    modifier =
                        Modifier.testTag(OutfitPostCardTestTags.SEE_FIT_BUTTON).height(36.dp)) {
                      Text("See fit", style = MaterialTheme.typography.bodySmall)
                    }
              }
        }
      }
}

@Preview(showBackground = true, backgroundColor = 0xFFFEFFFE)
@Composable
fun OutfitPostCardPreview() {
  OOTDTheme {
    OutfitPostCard(
        post =
            OutfitPost(
                postUID = "1",
                name = "Pit",
                ownerId = "user123",
                description = "Casual monochrome fit for fall 🍂"),
        isBlurred = false)
  }
}
