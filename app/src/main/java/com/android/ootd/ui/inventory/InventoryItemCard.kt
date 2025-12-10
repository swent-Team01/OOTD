package com.android.ootd.ui.inventory

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.android.ootd.model.items.Item
import com.android.ootd.ui.post.items.OotdItemCard

/**
 * Individual item card displaying the item using the OotdItemCard component.
 *
 * @param item The item to display
 * @param onClick Callback when the item is clicked
 * @param modifier Modifier for styling
 */
@Composable
fun InventoryItemCard(
    item: Item,
    onClick: () -> Unit,
    isStarred: Boolean,
    onToggleStar: () -> Unit,
    modifier: Modifier = Modifier,
    showStarIcon: Boolean = true
) {
  OotdItemCard(
      item = item,
      onClick = onClick,
      modifier = modifier,
      isStarred = isStarred,
      onToggleStar = onToggleStar,
      showStarToggle = showStarIcon,
      aspectRatio = 9f / 16f, // Match See Fit card aspect ratio
      testTag = "${InventoryScreenTestTags.ITEM_CARD}_${item.itemUuid}",
      starButtonTestTag = "${InventoryScreenTestTags.ITEM_STAR_BUTTON}_${item.itemUuid}",
      showPrice = false,
      showCategory = false)
}
