package com.android.ootd.ui.post.items

import androidx.compose.runtime.Composable
import com.android.ootd.model.items.Item

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
  OotdItemCard(
      item = item,
      onClick = onClick,
      onEditClick = onEditClick,
      isOwner = isOwner,
      isStarred = isStarred,
      onToggleStar = onToggleStar,
      showStarToggle = showStarToggle,
      testTag = ItemsTestTags.getTestTagForItem(item),
      starButtonTestTag = ItemsTestTags.getStarButtonTag(item),
      editButtonTestTag = ItemsTestTags.ITEM_CARD_EDIT_BUTTON,
      imageTestTag = ItemsTestTags.ITEM_CARD_IMAGE,
      categoryTestTag = ItemsTestTags.ITEM_CARD_CATEGORY,
      typeTestTag = ItemsTestTags.ITEM_CARD_TYPE)
}
