package com.android.ootd.ui.post.items

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.android.ootd.R
import com.android.ootd.model.items.Item
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Secondary
import com.android.ootd.ui.theme.StarYellow
import com.android.ootd.ui.theme.Typography
import com.android.ootd.utils.composables.ShowText

/**
 * A reusable item card component that displays an item with its image, details, and actions. Based
 * on the design from SeeFitCard.
 */
@Composable
fun OotdItemCard(
    item: Item,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onEditClick: ((String) -> Unit)? = null,
    isOwner: Boolean = false,
    isStarred: Boolean = false,
    onToggleStar: (() -> Unit)? = null,
    showStarToggle: Boolean = false,
    aspectRatio: Float = 9f / 16f,
    testTag: String = "itemCard_${item.itemUuid}",
    starButtonTestTag: String? = null,
    editButtonTestTag: String? = null,
    imageTestTag: String? = null,
    categoryTestTag: String? = null,
    typeTestTag: String? = null,
    showPrice: Boolean = true,
    showCategory: Boolean = true
) {
  Card(
      modifier =
          modifier.fillMaxWidth().aspectRatio(aspectRatio).clickable { onClick() }.testTag(testTag),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = White),
      elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)) {
        Box(modifier = Modifier.fillMaxSize()) {
          ItemImage(item, imageTestTag)
          GradientOverlay()
          if (showPrice) {
            PriceChip(item)
          }

          if (showStarToggle && onToggleStar != null) {
            StarToggle(isStarred, onToggleStar, testTag, starButtonTestTag)
          }

          if (isOwner && onEditClick != null) {
            EditButton(onClick = { onEditClick(item.itemUuid) }, testTag, editButtonTestTag)
          }

          ItemInfo(item, testTag, showCategory, categoryTestTag, typeTestTag)
        }
      }
}

@Composable
private fun BoxScope.ItemImage(item: Item, testTag: String?) {
  AsyncImage(
      model = item.image.imageUrl,
      contentDescription = "Item Image",
      modifier =
          Modifier.fillMaxSize()
              .align(Alignment.Center)
              .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
      placeholder = painterResource(R.drawable.ic_photo_placeholder),
      error = painterResource(R.drawable.ic_photo_placeholder),
      contentScale = ContentScale.Crop)
}

@Composable
private fun BoxScope.GradientOverlay() {
  Box(
      modifier =
          Modifier.matchParentSize()
              .background(
                  Brush.verticalGradient(
                      colors =
                          listOf(
                              Color.Transparent,
                              Color.Black.copy(alpha = 0.4f),
                              Color.Black.copy(alpha = 0.75f)),
                      startY = 0f,
                      endY = Float.POSITIVE_INFINITY)))
}

@Composable
private fun BoxScope.PriceChip(item: Item) {
  val hasPrice = item.price != null && item.price != 0.0 && item.currency != null
  if (!hasPrice) return

  Box(
      modifier =
          Modifier.align(Alignment.TopEnd)
              .padding(8.dp)
              .background(color = Secondary.copy(alpha = 0.9f), shape = RoundedCornerShape(12.dp))
              .padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(
            text = "${item.price?.toInt()} ${item.currency}",
            style = Typography.labelMedium.copy(color = Primary))
      }
}

@Composable
private fun BoxScope.StarToggle(
    isStarred: Boolean,
    onToggle: () -> Unit,
    parentTestTag: String,
    customTestTag: String?
) {
  IconButton(
      onClick = { onToggle() },
      modifier =
          Modifier.align(Alignment.TopStart)
              .padding(6.dp)
              .size(32.dp)
              .testTag(customTestTag ?: "${parentTestTag}_starButton")) {
        Icon(
            imageVector = if (isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
            contentDescription = "Toggle wishlist",
            tint = if (isStarred) StarYellow else White.copy(alpha = 0.8f))
      }
}

@Composable
private fun BoxScope.EditButton(
    onClick: () -> Unit,
    parentTestTag: String,
    customTestTag: String?
) {
  Box(
      modifier =
          Modifier.align(Alignment.BottomEnd)
              .padding(8.dp)
              .size(32.dp)
              .background(color = Secondary.copy(alpha = 0.9f), shape = RoundedCornerShape(12.dp))
              .testTag(customTestTag ?: "${parentTestTag}_editButton")) {
        IconButton(onClick = onClick) {
          Icon(
              imageVector = Icons.Default.Edit,
              contentDescription = "Edit item",
              tint = White,
              modifier = Modifier.size(16.dp))
        }
      }
}

@Composable
private fun BoxScope.ItemInfo(
    item: Item,
    parentTestTag: String,
    showCategory: Boolean,
    categoryTestTag: String?,
    typeTestTag: String?
) {
  val brandText = item.brand.orEmpty()
  Column(
      modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
      horizontalAlignment = Alignment.Start) {
        if (showCategory) {
          ShowText(
              text = item.category.uppercase(),
              style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
              color = White,
              modifier = Modifier.testTag(categoryTestTag ?: "${parentTestTag}_category"))
        }
        ShowText(
            text = item.type.orEmpty(),
            style = Typography.bodyLarge,
            color = White,
            textAlign = TextAlign.Start,
            modifier = Modifier.testTag(typeTestTag ?: "${parentTestTag}_type"))
        if (brandText.isNotBlank()) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            ShowText(
                text = brandText,
                style = Typography.bodyMedium,
                color = White.copy(alpha = 0.9f),
                textAlign = TextAlign.Start)
          }
        }
      }
}
