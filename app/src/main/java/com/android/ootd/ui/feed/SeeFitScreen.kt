package com.android.ootd.ui.feed

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.ootd.model.items.Item
import com.android.ootd.ui.theme.Primary

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

  // Dialog
  const val ITEM_DETAILS_DIALOG = "seeFitItemDetailsDialog"
  const val ITEM_IMAGE = "seeFitItemImage"
  const val ITEM_CATEGORY = "seeFitItemCategory"
  const val ITEM_TYPE = "seeFitItemType"
  const val ITEM_BRAND = "seeFitItemBrand"
  const val ITEM_PRICE = "seeFitItemPrice"
  const val ITEM_MATERIAL = "seeFitItemMaterial"
  const val ITEM_LINK = "seeFitItemLink"

  fun getTestTagForItem(item: Item): String {
    return "seeFitItemCard_${item.itemUuid}"
  }
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
    goBack: () -> Unit = {}
) {

  val context = LocalContext.current
  val uiState by seeFitViewModel.uiState.collectAsState()
  val items = uiState.items

  LaunchedEffect(Unit) { seeFitViewModel.getItemsForPost(postUuid) }

  LaunchedEffect(uiState.errorMessage) {
    uiState.errorMessage?.let { errorMsg ->
      Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
      seeFitViewModel.clearMessage()
    }
  }

  Scaffold(
      modifier = Modifier.testTag(SeeFitScreenTestTags.SCREEN),
      topBar = {
        CenterAlignedTopAppBar(
            modifier = Modifier.testTag(SeeFitScreenTestTags.TOP_APP_BAR),
            title = {
              Text(
                  modifier = Modifier.testTag(SeeFitScreenTestTags.TITLE),
                  text = "OOTD",
                  style =
                      MaterialTheme.typography.displayLarge.copy(
                          fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
            },
            navigationIcon = {
              IconButton(
                  onClick = { goBack() },
                  modifier = Modifier.testTag(SeeFitScreenTestTags.NAVIGATE_TO_FEED_SCREEN)) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Go Back",
                        tint = MaterialTheme.colorScheme.tertiary)
                  }
            },
            colors =
                TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = Primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant),
        )
      },
  ) { innerPadding ->
    if (items.isEmpty()) {
      Column(
          modifier = Modifier.fillMaxSize().padding(innerPadding),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No items associated with this post.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
    } else {
      Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp).padding(innerPadding),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally) {
            ItemGridScreen(items = items)
          }
    }
  }
}
