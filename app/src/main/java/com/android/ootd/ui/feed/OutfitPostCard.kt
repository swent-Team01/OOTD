package com.android.ootd.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.ui.theme.OOTDTheme

object OutfitPostCardTestTags {
  const val OUTFIT_POST_CARD = "outfitPostCard"
  const val POST_USERNAME = "postUsername"
  const val POST_IMAGE = "postImage"
  const val POST_DESCRIPTION = "postDescription"
  const val SEE_FIT_BUTTON = "seeFitButton"
}

@Composable
fun OutfitPostCard(
    post: OutfitPost,
    isBlurred: Boolean,
    modifier: Modifier = Modifier,
    onSeeFitClick: () -> Unit = {}
) {
  Box(
      modifier =
          modifier
              .padding(8.dp)
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .testTag(OutfitPostCardTestTags.OUTFIT_POST_CARD)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
              Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Text(
                    text = post.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag(OutfitPostCardTestTags.POST_USERNAME))

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)) {
                      AsyncImage(
                          model = post.outfitURL.ifBlank { null },
                          contentDescription = "Outfit image",
                          modifier =
                              Modifier.fillMaxSize().testTag(OutfitPostCardTestTags.POST_IMAGE),
                          contentScale = ContentScale.Crop,
                          placeholder =
                              rememberAsyncImagePainter("https://via.placeholder.com/600x400"),
                          error =
                              rememberAsyncImagePainter(
                                  "https://via.placeholder.com/600x400?text=No+Image"))
                    }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                      val descriptionText =
                          if (post.description.isNotBlank()) {
                            "${post.name}: ${post.description}"
                          } else {
                            post.name
                          }

                      Text(
                          text = descriptionText,
                          style = MaterialTheme.typography.bodyLarge,
                          color = MaterialTheme.colorScheme.primary,
                          modifier =
                              Modifier.weight(1f)
                                  .padding(end = 8.dp)
                                  .testTag(OutfitPostCardTestTags.POST_DESCRIPTION),
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis)

                      Button(
                          onClick = onSeeFitClick,
                          enabled = !isBlurred,
                          shape = RoundedCornerShape(50),
                          colors =
                              ButtonDefaults.buttonColors(
                                  containerColor = MaterialTheme.colorScheme.primary,
                                  contentColor = MaterialTheme.colorScheme.onPrimary),
                          modifier =
                              Modifier.testTag(OutfitPostCardTestTags.SEE_FIT_BUTTON)
                                  .height(36.dp)) {
                            Text("See fit", style = MaterialTheme.typography.bodySmall)
                          }
                    }
              }
            }

        // ---- Overlay for blur + blue tint ----
        if (isBlurred) {
          Box(
              modifier =
                  Modifier.matchParentSize().blur(8.dp).background(Color.Gray.copy(alpha = 0.9f)))
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
                description = "Casual monochrome fit for fall 🍂",
                outfitURL = "https://via.placeholder.com/600x400"),
        isBlurred = true)
  }
}
