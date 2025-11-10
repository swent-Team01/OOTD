package com.android.ootd.ui.Inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.android.ootd.model.items.Item

/**
 * Grid layout displaying inventory items.
 *
 * @param items List of items to display
 * @param onItemClick Callback when an item is clicked
 * @param modifier Modifier for styling
 */
@Composable
fun InventoryGrid(items: List<Item>, onItemClick: (Item) -> Unit, modifier: Modifier = Modifier) {
  LazyVerticalGrid(
      columns = GridCells.Fixed(3),
      modifier = modifier.fillMaxSize().testTag(InventoryScreenTestTags.ITEMS_GRID),
      contentPadding = PaddingValues(16.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items) { item -> InventoryItemCard(item = item, onClick = { onItemClick(item) }) }
      }
}
