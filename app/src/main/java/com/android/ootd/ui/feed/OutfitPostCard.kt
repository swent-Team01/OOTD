package com.android.ootd.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.FavoriteBorder
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
  const val LIKE_BUTTON = "likeButton"
  const val LIKE_COUNT = "likeCount"
}

/**
 * Composable displaying the profile section of the outfit post card, including profile picture,
 * username, and remaining lifetime indicator.
 *
 * @param post The outfit post data.
 */
@Composable
private fun ProfileSection(post: OutfitPost) {
  val totalLifetime = 24 * 60 * 60 * 1000L // 24h in ms
  val now = System.currentTimeMillis()
  // Calculate remaining lifetime
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

      // Profile picture or initial if no profile picture set
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

      // Remaining lifetime label
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

/**
 * Composable displaying the like button and like count.
 *
 * @param isLiked Whether the post is liked by the current user.
 * @param likeCount The total number of likes for the post.
 * @param enabled Whether the like button is enabled(disabled when post is blurred).
 * @param onClick Callback when the like button is clicked.
 */
@Composable
private fun LikeRow(isLiked: Boolean, likeCount: Int, enabled: Boolean, onClick: () -> Unit) {
  Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.testTag(OutfitPostCardTestTags.LIKE_BUTTON)) {
              Icon(
                  imageVector =
                      if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                  contentDescription = if (isLiked) "Unlike" else "Like",
                  tint =
                      if (isLiked) MaterialTheme.colorScheme.error // nice "Instagram-ish" red
                      else MaterialTheme.colorScheme.onSecondaryContainer)
            }

        Text(
            text = likeCount.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.testTag(OutfitPostCardTestTags.LIKE_COUNT))
      }
}

/**
 * Composable displaying the outfit post image, with optional blur effect.
 *
 * @param post The outfit post data.
 * @param isBlurred Whether the post image should be blurred (locked).
 */
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

/**
 * Composable displaying the post description and "See fit" button.
 *
 * @param post The outfit post data.
 * @param isBlurred Whether the post image is blurred (locked).
 * @param onSeeFitClick Callback when "See fit" button is clicked, passing the post UID.
 */
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

/**
 * Composable displaying an outfit post card with profile section, image, description, "See fit"
 * button, and like functionality. Supports blurring for locked posts.
 *
 * @param post The outfit post data.
 * @param isBlurred Whether the post image should be blurred (locked).
 * @param modifier Optional modifier for the card.
 * @param isLiked Whether the current user has liked the post.
 * @param likeCount The total number of likes for the post.
 * @param onLikeClick Callback when the like button is clicked, passing the post UID.
 * @param onSeeFitClick Callback when "See fit" button is clicked, passing the post UID.
 */
@Composable
fun OutfitPostCard(
    post: OutfitPost,
    isBlurred: Boolean,
    modifier: Modifier = Modifier,
    isLiked: Boolean,
    likeCount: Int,
    onLikeClick: (String) -> Unit,
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
                DescriptionAndButton(post, isBlurred, onSeeFitClick)

                Spacer(modifier = Modifier.height(8.dp))

                LikeRow(
                    isLiked = isLiked,
                    likeCount = likeCount,
                    enabled = !isBlurred,
                    onClick = { onLikeClick(post.postUID) })
              }
            }

        // Blur overlay for locked posts
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
