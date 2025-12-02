package com.android.ootd.ui.feed

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.android.ootd.R
import com.android.ootd.model.items.Item
import com.android.ootd.ui.theme.OnSurfaceVariant
import com.android.ootd.ui.theme.Secondary
import com.android.ootd.ui.theme.StarYellow
import com.android.ootd.ui.theme.Typography
import com.android.ootd.utils.composables.ShowText

/**
 * Composable representing an individual item card in the See Fit grid that displays a small part of
 * the item with its image, category, and type.
 */
@Composable
fun ItemCard(
    item: Item,
    onClick: () -> Unit,
    onEditClick: (String) -> Unit,
    isOwner: Boolean,
    isStarred: Boolean,
    onToggleStar: () -> Unit,
    showStarToggle: Boolean
) {
  Card(
      modifier = itemCardModifier(item, onClick),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)) {
        Box(modifier = Modifier.fillMaxSize()) {
          ItemImage(item)
          GradientOverlay()
          PriceChip(item)
          StarToggle(item, showStarToggle, isStarred, onToggleStar)
          EditButton(isOwner, onClick = { onEditClick(item.itemUuid) })
          ItemInfo(item)
        }
      }
}

private fun itemCardModifier(item: Item, onClick: () -> Unit): Modifier {
  return Modifier.fillMaxWidth()
      .aspectRatio(9f / 16f)
      .clickable { onClick() }
      .testTag(SeeFitScreenTestTags.getTestTagForItem(item))
}

@Composable
private fun BoxScope.ItemImage(item: Item) {
  AsyncImage(
      model = item.image.imageUrl,
      contentDescription = "Item Image",
      modifier =
          Modifier.fillMaxSize()
              .testTag(SeeFitScreenTestTags.ITEM_CARD_IMAGE)
              .align(Alignment.Center),
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
  val hasPrice = item.price != null && item.currency != null
  if (!hasPrice) return

  Box(
      modifier =
          Modifier.align(Alignment.TopEnd)
              .padding(8.dp)
              .background(color = Secondary.copy(alpha = 0.9f), shape = RoundedCornerShape(12.dp))
              .padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(
            text = "${item.price.toInt()} ${item.currency}",
            style = Typography.labelMedium.copy(color = OnSurfaceVariant))
      }
}

@Composable
private fun BoxScope.StarToggle(
    item: Item,
    showStarToggle: Boolean,
    isStarred: Boolean,
    onToggle: () -> Unit
) {
  if (!showStarToggle) return

  IconButton(
      onClick = { onToggle() },
      modifier =
          Modifier.align(Alignment.TopStart)
              .padding(6.dp)
              .size(32.dp)
              .background(color = Color.Black.copy(alpha = 0.35f), shape = RoundedCornerShape(8.dp))
              .testTag(SeeFitScreenTestTags.getStarButtonTag(item))) {
        Icon(
            imageVector = if (isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
            contentDescription = "Toggle wishlist",
            tint = if (isStarred) StarYellow else Color.White.copy(alpha = 0.8f))
      }
}

@Composable
private fun BoxScope.EditButton(isOwner: Boolean, onClick: () -> Unit) {
  if (!isOwner) return

  Box(
      modifier =
          Modifier.align(Alignment.BottomEnd)
              .padding(8.dp)
              .size(32.dp)
              .background(color = Secondary.copy(alpha = 0.9f), shape = RoundedCornerShape(12.dp))
              .testTag(SeeFitScreenTestTags.ITEM_CARD_EDIT_BUTTON)) {
        IconButton(onClick = onClick) {
          Icon(
              imageVector = Icons.Default.Edit,
              contentDescription = "Edit item",
              tint = OnSurfaceVariant,
              modifier = Modifier.size(16.dp))
        }
      }
}

@Composable
private fun BoxScope.ItemInfo(item: Item) {
  val brandText = item.brand.orEmpty()
  Column(
      modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
      horizontalAlignment = Alignment.Start) {
        ShowText(
            text = item.category.uppercase(),
            style = Typography.bodyLarge.copy(color = OnSurfaceVariant),
            modifier = Modifier.testTag(SeeFitScreenTestTags.ITEM_CARD_CATEGORY))
        ShowText(
            text = item.type.orEmpty(),
            style = Typography.bodyLarge.copy(color = OnSurfaceVariant),
            textAlign = TextAlign.Start,
            modifier = Modifier.testTag(SeeFitScreenTestTags.ITEM_CARD_TYPE))
        if (brandText.isNotBlank()) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = brandText,
                style = Typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.9f)))
          }
        }
      }
}
