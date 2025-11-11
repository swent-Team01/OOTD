package com.android.ootd.ui.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.android.ootd.model.items.Item

@Composable
fun ItemGridScreen(items: List<Item>) {

  var selectedItem by remember { mutableStateOf<Item?>(null) }

  Box(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.testTag(SeeFitScreenTestTags.ITEMS_GRID),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          items(items) { item -> ItemCard(item = item, onClick = { selectedItem = item }) }
        }

    // Show dialog if an item is selected
    selectedItem?.let { item ->
      SeeItemDetailsDialog(item = item, onDismissRequest = { selectedItem = null })
    }
  }
}
