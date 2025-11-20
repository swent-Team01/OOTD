package com.android.ootd.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.android.ootd.model.map.Location
import com.android.ootd.model.map.isValidLocation
import com.android.ootd.model.posts.OutfitPost

object OutfitPostCardTestTags {
  const val OUTFIT_POST_CARD = "outfitPostCard"
  const val POST_USERNAME = "postUsername"
  const val POST_IMAGE = "postImage"
  const val POST_DESCRIPTION = "postDescription"
  const val SEE_FIT_BUTTON = "seeFitButton"
  const val PROFILE_PIC = "profilePic"
  const val PROFILE_INITIAL = "profileInitial"
  const val BLUR_OVERLAY = "blurOverlay"
  const val REMAINING_TIME = "remainingTime"
  const val EXPIRED_INDICATOR = "expiredIndicator"

  const val POST_LOCATION = "postLocation"
}

@Composable
private fun ProfileSection(post: OutfitPost) {
  val totalLifetime = 24 * 60 * 60 * 1000L // 24h in ms
  val now = System.currentTimeMillis()
  val elapsed = (now - post.timestamp).coerceAtLeast(0L)
  val remainingMs = totalLifetime - elapsed
  val remainingFraction = (remainingMs.toFloat() / totalLifetime.toFloat()).coerceIn(0f, 1f)

  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
    Box(contentAlignment = Alignment.Center) {
      // Circular indicator for remaining lifetime
      CircularProgressIndicator(
          progress = { remainingFraction },
          color = MaterialTheme.colorScheme.primary,
          trackColor = MaterialTheme.colorScheme.surfaceVariant,
          strokeWidth = 3.dp,
          modifier = Modifier.size(44.dp))

      if (post.userProfilePicURL.isNotBlank()) {
        AsyncImage(
            model = post.userProfilePicURL,
            contentDescription = "Profile picture",
            contentScale = ContentScale.Crop,
            modifier =
                Modifier.size(36.dp)
                    .clip(CircleShape)
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
    }

    Spacer(modifier = Modifier.width(8.dp))

    Column {
      Text(
          text = post.name,
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.testTag(OutfitPostCardTestTags.POST_USERNAME))

      // ---- Remaining lifetime label ----
      val (remainingText, tag) =
          when {
            remainingMs <= 0L -> {
              "Expired" to OutfitPostCardTestTags.EXPIRED_INDICATOR
            }
            remainingMs < 60 * 60 * 1000L -> {
              val mins = (remainingMs / (60 * 1000L)).coerceAtLeast(1)
              "${mins}m left" to OutfitPostCardTestTags.REMAINING_TIME
            }
            else -> {
              val hrs = (remainingMs / (60 * 60 * 1000L)).coerceAtLeast(1)
              "${hrs}h left" to OutfitPostCardTestTags.REMAINING_TIME
            }
          }

      Text(
          text = remainingText,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.tertiary,
          modifier = Modifier.testTag(tag))
    }
  }
}

@Composable
private fun PostImage(post: OutfitPost, isBlurred: Boolean) {
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
            placeholder = rememberAsyncImagePainter("https://via.placeholder.com/600x400"),
            error = rememberAsyncImagePainter("https://via.placeholder.com/600x400?text=No+Image"))
      }
}

@Composable
private fun DescriptionAndButton(
    post: OutfitPost,
    isBlurred: Boolean,
    onSeeFitClick: (String) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }

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
            maxLines = if (expanded) Int.MAX_VALUE else 1,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier.weight(1f)
                    .padding(end = 8.dp)
                    .testTag(OutfitPostCardTestTags.POST_DESCRIPTION)
                    .clickable { expanded = !expanded })

        Button(
            onClick = { onSeeFitClick(post.postUID) },
            enabled = !isBlurred,
            shape = RoundedCornerShape(50),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary),
            modifier = Modifier.testTag(OutfitPostCardTestTags.SEE_FIT_BUTTON).height(36.dp)) {
              Text("See fit", style = MaterialTheme.typography.bodySmall)
            }
      }
}

@Composable
fun OutfitPostCard(
    post: OutfitPost,
    isBlurred: Boolean,
    modifier: Modifier = Modifier,
    onSeeFitClick: (String) -> Unit = {}
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
              Column(Modifier.fillMaxWidth().padding(12.dp)) {
                ProfileSection(post)
                Spacer(modifier = Modifier.height(8.dp))
                PostImage(post, isBlurred)
                PostLocation(post.location)
                DescriptionAndButton(post, isBlurred, onSeeFitClick)
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

/**
 * Displays a post's geographic location below the outfit image when available.
 *
 * @param location the Location to display; only rendered when valid
 */
@Composable
fun PostLocation(location: Location) {
  if (isValidLocation(location)) {
    val displayName =
        if (location.name.length > 50) {
          location.name.take(47) + "..."
        } else {
          location.name
        }

    Text(
        text = displayName,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.tertiary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier =
            Modifier.fillMaxWidth()
                .padding(top = 4.dp)
                .testTag(OutfitPostCardTestTags.POST_LOCATION))
  }
}
