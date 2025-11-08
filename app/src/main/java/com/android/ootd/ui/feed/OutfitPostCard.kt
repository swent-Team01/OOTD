package com.android.ootd.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
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
  const val PROFILE_PIC = "profilePic"
  const val PROFILE_INITIAL = "profileInitial"
  const val BLUR_OVERLAY = "blurOverlay"
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 4.dp)) {
                      if (post.userProfilePicURL.isNotBlank()) {
                        AsyncImage(
                            model = post.userProfilePicURL,
                            contentDescription = "Profile picture",
                            contentScale = ContentScale.Crop,
                            modifier =
                                Modifier.size(36.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .testTag(OutfitPostCardTestTags.PROFILE_PIC))
                      } else {
                        Box(
                            modifier =
                                Modifier.size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .testTag(OutfitPostCardTestTags.PROFILE_INITIAL),
                            contentAlignment = Alignment.Center) {
                              Text(
                                  text = post.name.firstOrNull()?.uppercase() ?: "",
                                  style = MaterialTheme.typography.titleMedium,
                                  color = MaterialTheme.colorScheme.onPrimary)
                            }
                      }

                      Spacer(modifier = Modifier.width(8.dp))

                      Text(
                          text = post.name,
                          style = MaterialTheme.typography.titleLarge,
                          color = MaterialTheme.colorScheme.primary,
                          modifier = Modifier.testTag(OutfitPostCardTestTags.POST_USERNAME))
                    }

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
                              Modifier.fillMaxSize()
                                  .testTag(OutfitPostCardTestTags.POST_IMAGE)
                                  .then(if (isBlurred) Modifier.blur(12.dp) else Modifier),
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
        // ---- Overlay for blur + message ----
        if (isBlurred) {
          Box(
              modifier = Modifier.matchParentSize().testTag(OutfitPostCardTestTags.BLUR_OVERLAY),
              contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center) {
                      Icon(
                          imageVector = Icons.Default.PhotoCamera,
                          contentDescription = "Photo icon",
                          tint = Color.White,
                          modifier = Modifier.size(48.dp))

                      Spacer(modifier = Modifier.height(8.dp))

                      Text(
                          text = "Do a fit check to unlock today's feed",
                          style = MaterialTheme.typography.titleLarge,
                          color = Color.White,
                          modifier = Modifier.padding(horizontal = 16.dp))
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
                description = "Casual monochrome fit for fall 🍂",
                outfitURL = "https://via.placeholder.com/600x400"),
        isBlurred = true)
  }
}
