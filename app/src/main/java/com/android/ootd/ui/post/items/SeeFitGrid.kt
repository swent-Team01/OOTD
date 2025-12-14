package com.android.ootd.ui.post.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.android.ootd.model.items.Item

enum class ItemGridLayout {
  VERTICAL_GRID, // 2-column grid, scrolls vertically
  HORIZONTAL_ROW // Single row, scrolls horizontally
}

/**
 * Test tags for item-related UI components. Used by ItemCard, ItemGridScreen, and
 * SeeItemDetailsDialog.
 */
object ItemsTestTags {
  // Grid/List
  const val ITEMS_GRID = "itemsGrid"

  // Card
  const val ITEM_CARD_IMAGE = "itemCardImage"
  const val ITEM_CARD_CATEGORY = "itemCardCategory"
  const val ITEM_CARD_TYPE = "itemCardType"
  const val ITEM_CARD_EDIT_BUTTON = "itemCardEditButton"
  const val ITEM_STAR_BUTTON = "itemStarButton"

  // Dialog
  const val ITEM_DETAILS_DIALOG = "itemDetailsDialog"
  const val ITEM_IMAGE = "itemImage"
  const val ITEM_CATEGORY = "itemCategory"
  const val ITEM_TYPE = "itemType"
  const val ITEM_BRAND = "itemBrand"
  const val ITEM_PRICE = "itemPrice"
  const val ITEM_MATERIAL = "itemMaterial"
  const val ITEM_LINK = "itemLink"
  const val ITEM_CONDITION = "itemCondition"
  const val ITEM_SIZE = "itemSize"
  const val ITEM_FIT_TYPE = "itemFitType"
  const val ITEM_STYLE = "itemStyle"
  const val ITEM_NOTES = "itemNotes"
  const val ITEM_LINK_COPY = "itemLinkCopy"
  const val ITEM_NOTES_COPY = "itemNotesCopy"

  /** Generate a unique test tag for a specific item card */
  fun getTestTagForItem(item: Item): String {
    return "itemCard_${item.itemUuid}"
  }

  /** Generate a unique test tag for a specific item's star button */
  fun getStarButtonTag(item: Item): String {
    return "${ITEM_STAR_BUTTON}_${item.itemUuid}"
  }
}

/**
 * Screen displaying a grid or row of items.
 *
 * @param items List of items to display
 * @param layout Whether to show as vertical grid or horizontal row
 */
@Composable
fun ItemGridScreen(
    items: List<Item>,
    modifier: Modifier = Modifier,
    onEditItem: (String) -> Unit = {},
    isOwner: Boolean = false,
    starredItemIds: Set<String> = emptySet(),
    onToggleStar: (Item) -> Unit = {},
    showStarToggle: Boolean = false,
    layout: ItemGridLayout =
        ItemGridLayout.VERTICAL_GRID // Defaults to grid for backward compatibility
) {

  var selectedItem by remember { mutableStateOf<Item?>(null) }

  Box(modifier = modifier) {
    when (layout) {
      ItemGridLayout.VERTICAL_GRID -> {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.testTag(ItemsTestTags.ITEMS_GRID),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              items(items) { item ->
                ItemCard(
                    item = item,
                    onClick = { selectedItem = item },
                    onEditClick = { onEditItem(item.itemUuid) },
                    isOwner = isOwner,
                    isStarred = starredItemIds.contains(item.itemUuid),
                    onToggleStar = { onToggleStar(item) },
                    showStarToggle = showStarToggle)
              }
            }
      }

      ItemGridLayout.HORIZONTAL_ROW -> {
        LazyRow(
            modifier = Modifier.testTag(ItemsTestTags.ITEMS_GRID),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)) {
              items(items) { item ->
                Box(modifier = Modifier.width(180.dp)) {
                  ItemCard(
                      item = item,
                      onClick = { selectedItem = item },
                      onEditClick = { onEditItem(item.itemUuid) },
                      isOwner = isOwner,
                      isStarred = starredItemIds.contains(item.itemUuid),
                      onToggleStar = { onToggleStar(item) },
                      showStarToggle = showStarToggle)
                }
              }
            }
      }
    }

    selectedItem?.let { item ->
      SeeItemDetailsDialog(item = item, onDismissRequest = { selectedItem = null })
    }
  }
}
