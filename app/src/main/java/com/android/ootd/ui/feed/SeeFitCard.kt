package com.android.ootd.ui.feed

import androidx.compose.runtime.Composable
import com.android.ootd.model.items.Item
import com.android.ootd.ui.post.items.OotdItemCard

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
      testTag = SeeFitScreenTestTags.getTestTagForItem(item),
      starButtonTestTag = SeeFitScreenTestTags.getStarButtonTag(item),
      editButtonTestTag = SeeFitScreenTestTags.ITEM_CARD_EDIT_BUTTON,
      imageTestTag = SeeFitScreenTestTags.ITEM_CARD_IMAGE,
      categoryTestTag = SeeFitScreenTestTags.ITEM_CARD_CATEGORY,
      typeTestTag = SeeFitScreenTestTags.ITEM_CARD_TYPE)
}
