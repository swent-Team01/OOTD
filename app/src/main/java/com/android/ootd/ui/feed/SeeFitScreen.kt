package com.android.ootd.ui.feed

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.ootd.model.items.Item
import com.android.ootd.ui.theme.Tertiary
import com.android.ootd.ui.theme.Typography
import com.android.ootd.utils.composables.BackArrow
import com.android.ootd.utils.composables.CenteredEmptyState
import com.android.ootd.utils.composables.CenteredLoadingState
import com.android.ootd.utils.composables.OOTDTopBar
import com.android.ootd.utils.composables.ShowText

object SeeFitScreenTestTags {
  const val SCREEN = "seeFitScreen"
  const val TOP_APP_BAR = "seeFitTopAppBar"
  const val TITLE = "seeFitTitle"
  const val NAVIGATE_TO_FEED_SCREEN = "navigateToFeedScreen"
  const val ITEMS_GRID = "seeFitItemsGrid"

  // Card
  const val ITEM_CARD_IMAGE = "seeFitItemCardImage"
  const val ITEM_CARD_CATEGORY = "seeFitItemCardCategory"
  const val ITEM_CARD_TYPE = "seeFitItemCardType"

  const val ITEM_CARD_EDIT_BUTTON = "seeFitItemCardEditButton"
  const val ITEM_STAR_BUTTON = "seeFitItemStarButton"

  // Dialog
  const val ITEM_DETAILS_DIALOG = "seeFitItemDetailsDialog"
  const val ITEM_IMAGE = "seeFitItemImage"
  const val ITEM_CATEGORY = "seeFitItemCategory"
  const val ITEM_TYPE = "seeFitItemType"
  const val ITEM_BRAND = "seeFitItemBrand"
  const val ITEM_PRICE = "seeFitItemPrice"
  const val ITEM_MATERIAL = "seeFitItemMaterial"
  const val ITEM_LINK = "seeFitItemLink"
  const val ITEM_CONDITION = "seeFitItemCondition"
  const val ITEM_SIZE = "seeFitItemSize"
  const val ITEM_FIT_TYPE = "seeFitItemFitType"
  const val ITEM_STYLE = "seeFitItemStyle"
  const val ITEM_NOTES = "seeFitItemNotes"
  const val ITEM_LINK_COPY = "seeFitItemLinkCopy"
  const val ITEM_NOTES_COPY = "seeFitItemNotesCopy"

  fun getTestTagForItem(item: Item): String {
    return "seeFitItemCard_${item.itemUuid}"
  }

  fun getStarButtonTag(item: Item): String = "${ITEM_STAR_BUTTON}_${item.itemUuid}"
}

/**
 * Screen to see items associated with a specific post.
 *
 * @param seeFitViewModel ViewModel managing the UI state
 * @param postUuid UUID of the post to fetch items for
 * @param goBack Callback to navigate back
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeeFitScreen(
    seeFitViewModel: SeeFitViewModel = viewModel(),
    postUuid: String = "",
    goBack: () -> Unit = {},
    onEditItem: (String) -> Unit = {}
) {

  val context = LocalContext.current
  val uiState by seeFitViewModel.uiState.collectAsState()
  val items = uiState.items

  LaunchedEffect(postUuid) { seeFitViewModel.getItemsForPost(postUuid) }
  LaunchedEffect(Unit) { seeFitViewModel.refreshStarredItems() }

  LaunchedEffect(uiState.errorMessage) {
    uiState.errorMessage?.let { errorMsg ->
      Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
      seeFitViewModel.clearMessage()
    }
  }

  Scaffold(
      modifier = Modifier.testTag(SeeFitScreenTestTags.SCREEN),
      topBar = {
        OOTDTopBar(
            modifier = Modifier.testTag(SeeFitScreenTestTags.TOP_APP_BAR),
            textModifier = Modifier.testTag(SeeFitScreenTestTags.TITLE),
            leftComposable = {
              BackArrow(
                  onBackClick = goBack,
                  modifier = Modifier.testTag(SeeFitScreenTestTags.NAVIGATE_TO_FEED_SCREEN))
            })
      },
  ) { innerPadding ->
    when {
      uiState.isLoading -> {
        CenteredLoadingState(
            message = "Loading items...", modifier = Modifier.fillMaxSize().padding(innerPadding))
      }

      items.isEmpty() -> {
        CenteredEmptyState(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            text = {
              ShowText(
                  text = "No items associated with this post.",
                  style = Typography.bodyLarge,
                  color = Tertiary,
                  modifier = Modifier.padding(innerPadding))
            })
      }

      else -> {
        ItemGridScreen(
            items = items,
            modifier = Modifier.fillMaxWidth().padding(16.dp).padding(innerPadding),
            onEditItem = onEditItem,
            isOwner = uiState.isOwner,
            starredItemIds = uiState.starredItemIds,
            onToggleStar = seeFitViewModel::toggleStar,
            showStarToggle = !uiState.isOwner)
      }
    }
  }
}
