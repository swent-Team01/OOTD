package com.android.ootd.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.android.ootd.R
import com.android.ootd.model.items.Item
import com.android.ootd.utils.ShowText
import com.android.ootd.ui.theme.Secondary

/**
 * Composable representing an individual item card in the See Fit grid that displays a small part of
 * the item with its image, category, and type.
 *
 * @param item The item to display
 * @param onClick Callback when the item is clicked
 */
@Composable
fun ItemCard(item: Item, onClick: () -> Unit) {
  val hasPrice = item.price != null && item.currency != null
  val brandText = item.brand.orEmpty()
  Card(
      modifier =
          Modifier.fillMaxWidth()
              .aspectRatio(9f / 16f)
              .clickable { onClick() }
              .testTag(SeeFitScreenTestTags.getTestTagForItem(item)),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)) {
        Box(modifier = Modifier.fillMaxSize()) {
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

          // Gradient overlay for better text contrast
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

          // Price chip (top-right) if available
          if (hasPrice) {
            Box(
                modifier =
                    Modifier.align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(
                            color = Secondary.copy(alpha = 0.9f), shape = RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)) {
                  Text(
                      text = "${item.price.toInt()} ${item.currency}",
                      style =
                          MaterialTheme.typography.labelMedium.copy(
                              color = MaterialTheme.colorScheme.onSurfaceVariant))
                }
          }

              Column(
                  modifier =
                      Modifier.weight(0.3f)
                          .fillMaxWidth()
                          .background(MaterialTheme.colorScheme.secondary)
                          .padding(8.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.Center) {
                    ShowText(
                        text = item.category,
                        style =
                            MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.testTag(SeeFitScreenTestTags.ITEM_CARD_CATEGORY))
                    ShowText(
                        text = item.type ?: "",
                        style =
                            MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.testTag(SeeFitScreenTestTags.ITEM_CARD_TYPE))
          Column(
              modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
              horizontalAlignment = Alignment.Start) {
                Text(
                    text = item.category.uppercase(),
                    style =
                        MaterialTheme.typography.labelLarge.copy(
                            color = Color.White.copy(alpha = 0.9f)),
                    modifier = Modifier.testTag(SeeFitScreenTestTags.ITEM_CARD_CATEGORY))

                Text(
                    text = item.type.orEmpty(),
                    style =
                        MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontSize = MaterialTheme.typography.titleLarge.fontSize),
                    textAlign = TextAlign.Start,
                    modifier = Modifier.testTag(SeeFitScreenTestTags.ITEM_CARD_TYPE))

                if (brandText.isNotBlank()) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = brandText,
                        style =
                            MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White.copy(alpha = 0.9f)))
                  }
                }
              }
        }
      }
}
