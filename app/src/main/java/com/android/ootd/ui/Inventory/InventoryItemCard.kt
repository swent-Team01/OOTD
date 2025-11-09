package com.android.ootd.ui.Inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.android.ootd.model.items.Item

/**
 * Individual item card displaying a small rounded square with the item's image.
 *
 * @param item The item to display
 * @param onClick Callback when the item is clicked
 * @param modifier Modifier for styling
 */
@Composable
fun InventoryItemCard(item: Item, onClick: () -> Unit, modifier: Modifier = Modifier) {
  Box(
      modifier =
          modifier
              .fillMaxWidth()
              .aspectRatio(1f)
              .clip(RoundedCornerShape(16.dp))
              .background(Color.LightGray.copy(alpha = 0.3f))
              .clickable(onClick = onClick)
              .testTag("${InventoryScreenTestTags.ITEM_CARD}_${item.itemUuid}"),
      contentAlignment = Alignment.Center) {
        if (item.image.imageUrl.isNotEmpty()) {
          AsyncImage(
              model = item.image.imageUrl,
              contentDescription = "Item image: ${item.category}",
              modifier = Modifier.fillMaxSize(),
              contentScale = ContentScale.Crop)
        } else {
          // Fallback for items without images
          Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center) {
                Text(
                    text = item.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(4.dp))
              }
        }
      }
}
