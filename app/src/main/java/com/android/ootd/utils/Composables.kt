package com.android.ootd.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.ui.account.AccountPageTestTags
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Secondary

@Composable fun BackArrow(onBackClick: () -> Unit) {}

@Composable
fun TextField(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = colorScheme.onSurface,
    textAlign: TextAlign = TextAlign.Center,
    fontFamily: androidx.compose.ui.text.font.FontFamily? = null,
) {
  Text(
      text = text,
      style = style,
      color = color,
      textAlign = textAlign,
      fontFamily = fontFamily,
      modifier = modifier.fillMaxWidth())
}

@Composable
fun ProfilePicture(size: Dp, profilePicture: String, username: String) {
  val color = colorScheme
  val defaultAvatarPainter = remember(color.tertiary) { ColorPainter(color.tertiary) }

  Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    if (profilePicture.isNotBlank()) {
      AsyncImage(
          model = profilePicture,
          contentDescription = "Profile Picture",
          placeholder = defaultAvatarPainter,
          error = defaultAvatarPainter,
          contentScale = ContentScale.Crop,
          modifier =
              Modifier.size(size).clip(CircleShape).testTag(AccountPageTestTags.AVATAR_IMAGE))
    } else {
      Box(
          modifier = Modifier.size(size).clip(CircleShape).background(Primary),
          contentAlignment = Alignment.Center) {
            Text(
                text = username.firstOrNull()?.uppercase() ?: "",
                style = typography.headlineLarge,
                color = Secondary,
                modifier = Modifier.testTag(AccountPageTestTags.AVATAR_LETTER))
          }
    }
  }
}

@Composable
fun DisplayUserPosts(
    posts: List<OutfitPost>,
    onPostClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    padding: Dp,
    spacing: Dp
) {
  val color = colorScheme
  val defaultPainter = remember(color.tertiary) { ColorPainter(color.tertiary) }
  val configuration = androidx.compose.ui.platform.LocalConfiguration.current
  val screenWidth = configuration.screenWidthDp.dp
  val itemWidth = (screenWidth - padding - (spacing * 2)) / 3 // subtract padding and spacing
  val itemHeight = itemWidth
  val rowCount = (posts.size + 2) / 3
  val totalHeight = rowCount * itemHeight.value + (rowCount - 1) * spacing.value

  LazyVerticalGrid(
      columns = GridCells.Fixed(3),
      horizontalArrangement = Arrangement.spacedBy(spacing),
      verticalArrangement = Arrangement.spacedBy(spacing),
      modifier = modifier.fillMaxWidth().height(totalHeight.dp)) {
        items(posts) { post ->
          AsyncImage(
              model = post.outfitURL,
              contentDescription = "Post image",
              placeholder = defaultPainter,
              error = defaultPainter,
              contentScale = ContentScale.Crop,
              modifier =
                  modifier
                      .size(itemWidth)
                      .clip(RoundedCornerShape(6.dp))
                      .clickable(onClick = { onPostClick(post.postUID) })
                      .background(color.surfaceVariant))
        }
      }
}

@Composable fun LoadingOverlay(modifier: Modifier = Modifier) {}

@Composable
fun FloatingButton(
    onButtonClick: (String?) -> Unit,
    modifier: Modifier = Modifier,
    buttonText: Composable
) {}

@Composable
fun ActionButton(
    onButtonClick: (String?) -> Unit,
    modifier: Modifier = Modifier,
    buttonText: Composable
) {}

/** Contains back arrow and */
@Composable fun OOTDTopBar(centerText: String, backArrow: Composable) {}
