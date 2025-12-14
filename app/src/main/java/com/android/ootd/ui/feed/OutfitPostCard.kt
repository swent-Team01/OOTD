package com.android.ootd.ui.feed

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
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
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.android.ootd.model.map.Location
import com.android.ootd.model.map.isValidLocation
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.ui.theme.OnSecondaryContainer
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Secondary
import com.android.ootd.ui.theme.Tertiary
import com.android.ootd.ui.theme.Typography
import com.android.ootd.utils.composables.CenteredEmptyState
import com.android.ootd.utils.composables.ProfilePicture
import com.android.ootd.utils.composables.ShowText

object OutfitPostCardTestTags {
  const val OUTFIT_POST_CARD = "outfitPostCard"
  const val POST_USERNAME = "postUsername"
  const val POST_IMAGE = "postImage"
  const val POST_IMAGE_BOX = "postImageBox"
  const val POST_DESCRIPTION = "postDescription"
  const val PROFILE_PIC = "profilePic"
  const val PROFILE_INITIAL = "profileInitial"
  const val BLUR_OVERLAY = "blurOverlay"
  const val REMAINING_TIME = "remainingTime"
  const val EXPIRED_INDICATOR = "expiredIndicator"

  const val POST_LOCATION = "postLocation"
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
private fun ProfileSection(post: OutfitPost, onProfileClick: (String) -> Unit = {}) {
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
          color = Primary,
          trackColor = Secondary,
          strokeWidth = 3.dp,
          modifier = Modifier.size(44.dp))

      // Profile picture or initial if no profile picture set
      val profilePic = post.userProfilePicURL
      val testTag =
          OutfitPostCardTestTags.PROFILE_PIC.takeIf { profilePic.isNotBlank() }
              ?: OutfitPostCardTestTags.PROFILE_INITIAL
      ProfilePicture(
          modifier = Modifier.testTag(testTag),
          size = 36.dp,
          profilePicture = profilePic,
          username = post.name,
          textStyle = Typography.titleMedium,
          onClick = { onProfileClick(post.ownerId) })
    }

    Spacer(modifier = Modifier.width(8.dp))

    Column(modifier = Modifier.clickable { onProfileClick(post.ownerId) }) {
      Text(
          text = post.name,
          style = Typography.titleLarge,
          color = Primary,
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
          style = Typography.bodySmall,
          color = Tertiary,
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
  Row(verticalAlignment = Alignment.CenterVertically) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.testTag(OutfitPostCardTestTags.LIKE_BUTTON)) {
          Icon(
              imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
              contentDescription = if (isLiked) "Liked" else "Unliked",
              tint = if (isLiked) MaterialTheme.colorScheme.error else OnSecondaryContainer)
        }
    Spacer(modifier = Modifier.width(1.dp))
    Text(
        text = likeCount.toString(),
        style = Typography.bodyMedium,
        color = OnSecondaryContainer,
        modifier = Modifier.testTag(OutfitPostCardTestTags.LIKE_COUNT))
  }
}

/**
 * Composable displaying the outfit post image, with optional blur effect.
 *
 * @param post The outfit post data.
 * @param isBlurred Whether the post image should be blurred (locked).
 */
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
private fun PostImage(post: OutfitPost, isBlurred: Boolean, modifier: Modifier = Modifier) {
  Box(
      modifier =
          Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(White)
              .testTag(OutfitPostCardTestTags.POST_IMAGE_BOX)
              .then(modifier)) {
        val context = LocalContext.current

        AsyncImage(
            model =
                ImageRequest.Builder(context)
                    .data(post.outfitURL.ifBlank { null })
                    .crossfade(true)
                    .allowHardware(false)
                    .memoryCacheKey(post.postUID) // Ensures unique cache key per image ID
                    .diskCacheKey(post.postUID) // Ensures unique disk cache key per image ID
                    .build(),
            contentDescription = "Outfit image",
            modifier =
                Modifier.fillMaxSize()
                    .testTag(OutfitPostCardTestTags.POST_IMAGE)
                    .then(if (isBlurred) Modifier.blur(12.dp) else Modifier),
            contentScale = ContentScale.Fit,
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
private fun PostDescription(post: OutfitPost) {
  var expanded by remember { mutableStateOf(false) }

  val descriptionText =
      if (post.description.isNotBlank()) {
        "${post.name}: ${post.description}"
      } else {
        post.name
      }

  Text(
      text = descriptionText,
      style = Typography.bodyLarge,
      color = Primary,
      maxLines = if (expanded) Int.MAX_VALUE else 2,
      overflow = TextOverflow.Ellipsis,
      modifier =
          Modifier.fillMaxWidth()
              .padding(top = 8.dp)
              .testTag(OutfitPostCardTestTags.POST_DESCRIPTION)
              .clickable { expanded = !expanded })
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
 * @param onCardClick Callback when the card is clicked, passing the post UID.
 * @param onLocationClick Callback when the location is clicked, passing the location.
 */
@Composable
fun OutfitPostCard(
    post: OutfitPost,
    isBlurred: Boolean,
    modifier: Modifier = Modifier,
    isLiked: Boolean,
    likeCount: Int,
    onLikeClick: (String) -> Unit,
    onCardClick: (String) -> Unit = {},
    onLocationClick: (Location) -> Unit = {},
    onCommentClick: (OutfitPost) -> Unit = {},
    onProfileClick: (String) -> Unit = {}
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
            colors = CardDefaults.cardColors(containerColor = Secondary),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
              Column(Modifier.fillMaxWidth().padding(12.dp)) {
                ProfileSection(post, onProfileClick = onProfileClick)
                Spacer(modifier = Modifier.height(8.dp))

                // Click to get details enabled only when not blurred
                val clickableModifier =
                    if (isBlurred) {
                      Modifier
                    } else {
                      Modifier.clickable { onCardClick(post.postUID) }
                    }
                PostImage(post, isBlurred, modifier = clickableModifier)
                PostLocation(post.location, onClick = { onLocationClick(post.location) })
                PostDescription(post)
                // Reactions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                      LikeRow(
                          isLiked = isLiked,
                          likeCount = likeCount,
                          enabled = !isBlurred,
                          onClick = { onLikeClick(post.postUID) })

                      CommentButton(
                          commentCount = post.comments.size,
                          enabled = !isBlurred,
                          onClick = { onCommentClick(post) })
                    }
              }

              Spacer(modifier = Modifier.height(8.dp))
            }

        // Blur overlay for locked posts
        if (isBlurred) {
          Box(
              modifier = Modifier.matchParentSize().testTag(OutfitPostCardTestTags.BLUR_OVERLAY),
              contentAlignment = Alignment.Center) {
                CenteredEmptyState(
                    icon = {
                      Icon(
                          imageVector = Icons.Default.PhotoCamera,
                          contentDescription = "Photo icon",
                          tint = White,
                          modifier = Modifier.size(48.dp))
                    },
                    text = {
                      ShowText(
                          text = "Do a fit check to unlock today's feed",
                          style = Typography.bodyLarge,
                          color = White)
                    })
              }
        }
      }
}

/**
 * Composable displaying the comment button and comment count.
 *
 * @param commentCount The total number of comments for the post.
 * @param enabled Whether the comment button is enabled (disabled when post is blurred).
 * @param onClick Callback when the comment button is clicked.
 */
@Composable
private fun CommentButton(commentCount: Int, enabled: Boolean, onClick: () -> Unit) {
  Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.clickable(enabled = enabled) { onClick() }.testTag("commentButton")) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Comment,
            contentDescription = "Comments",
            tint = if (enabled) OnSecondaryContainer else Tertiary)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = commentCount.toString(),
            style = Typography.bodyMedium,
            color = if (enabled) OnSecondaryContainer else Tertiary)
      }
}

/**
 * Displays a post's geographic location below the outfit image when available.
 *
 * @param location the Location to display; only rendered when valid
 * @param onClick callback when the location is clicked
 */
@Composable
fun PostLocation(location: Location, onClick: () -> Unit = {}) {
  if (isValidLocation(location)) {
    val displayName =
        if (location.name.length > 50) {
          location.name.take(47) + "..."
        } else {
          location.name
        }

    Text(
        text = displayName,
        style = Typography.bodySmall,
        color = Tertiary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier =
            Modifier.fillMaxWidth()
                .padding(top = 4.dp)
                .clickable { onClick() }
                .testTag(OutfitPostCardTestTags.POST_LOCATION))
  }
}
