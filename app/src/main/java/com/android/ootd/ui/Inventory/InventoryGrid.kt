package com.android.ootd.ui.Inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.ootd.model.items.Item
import com.android.ootd.ui.theme.Primary

/**
 * Grid layout displaying inventory items grouped by category.
 *
 * @param items List of items to display (should be pre-sorted by category)
 * @param onItemClick Callback when an item is clicked
 * @param modifier Modifier for styling
 */
@Composable
fun InventoryGrid(items: List<Item>, onItemClick: (Item) -> Unit, modifier: Modifier = Modifier) {
  // Group items by normalized category
  val groupedItems = items.groupBy { normalizeCategoryName(it.category) }

  LazyVerticalGrid(
      columns = GridCells.Fixed(3),
      modifier = modifier.fillMaxSize().testTag(InventoryScreenTestTags.ITEMS_GRID),
      contentPadding = PaddingValues(16.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // Iterate through categories in order
        val categoryOrder = listOf("Clothing", "Shoes", "Accessories", "Bags")
        categoryOrder.forEach { category ->
          val categoryItems = groupedItems[category]
          if (!categoryItems.isNullOrEmpty()) {
            // Category header spanning full width
            item(span = { GridItemSpan(3) }) {
              Text(
                  text = category.uppercase(),
                  style =
                      MaterialTheme.typography.titleLarge.copy(
                          fontWeight = FontWeight.Bold, color = Primary),
                  modifier =
                      Modifier.fillMaxWidth()
                          .padding(top = 16.dp, bottom = 8.dp)
                          .testTag("categoryHeader_$category"))
            }

            // Items in this category
            items(categoryItems) { item ->
              InventoryItemCard(item = item, onClick = { onItemClick(item) })
            }
          }
        }

        // Handle items with unknown categories
        val unknownItems = groupedItems.filterKeys { it !in categoryOrder }.values.flatten()
        if (unknownItems.isNotEmpty()) {
          item(span = { GridItemSpan(3) }) {
            Text(
                text = "OTHER",
                style =
                    MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold, color = Primary),
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp)
                        .testTag("categoryHeader_Other"))
          }

          items(unknownItems) { item ->
            InventoryItemCard(item = item, onClick = { onItemClick(item) })
          }
        }
      }
}

/**
 * Normalizes category names to handle variations. E.g., "clothes" -> "Clothing", "shoe" -> "Shoes"
 */
private fun normalizeCategoryName(category: String): String {
  return when (category.trim().lowercase()) {
    "clothes",
    "clothing" -> "Clothing"
    "shoes",
    "shoe" -> "Shoes"
    "bags",
    "bag" -> "Bags"
    "accessories",
    "accessory" -> "Accessories"
    else -> category.trim()
  }
}
