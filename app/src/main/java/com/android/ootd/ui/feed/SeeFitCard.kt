package com.android.ootd.ui.feed

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.android.ootd.R
import com.android.ootd.model.items.Item
import com.android.ootd.utils.ShowText

/**
 * Composable representing an individual item card in the See Fit grid that displays a small part of
 * the item with its image, category, and type.
 *
 * @param item The item to display
 * @param onClick Callback when the item is clicked
 */
@Composable
fun ItemCard(item: Item, onClick: () -> Unit) {
  Card(
      modifier =
          Modifier.fillMaxWidth()
              .aspectRatio(9f / 16f)
              .clickable { onClick() }
              .testTag(SeeFitScreenTestTags.getTestTagForItem(item)),
      shape = RoundedCornerShape(8.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
      elevation = CardDefaults.cardElevation(4.dp)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally) {
              Box(
                  modifier = Modifier.weight(0.7f).fillMaxWidth(),
                  contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = item.image.imageUrl,
                        contentDescription = "Item Image",
                        modifier =
                            Modifier.fillMaxSize().testTag(SeeFitScreenTestTags.ITEM_CARD_IMAGE),
                        placeholder = painterResource(R.drawable.ic_photo_placeholder),
                        error = painterResource(R.drawable.ic_photo_placeholder),
                        contentScale = ContentScale.Crop,
                    )
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
                  }
            }
      }
}
