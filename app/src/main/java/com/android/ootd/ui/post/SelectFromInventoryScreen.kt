package com.android.ootd.ui.post

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.ootd.model.items.Item
import com.android.ootd.ui.inventory.InventoryGrid
import com.android.ootd.ui.inventory.InventorySearchBar
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Secondary
import com.android.ootd.ui.theme.Typography
import com.android.ootd.utils.composables.BackArrow
import com.android.ootd.utils.composables.OOTDTopBar

object SelectInventoryItemScreenTestTags {
  const val SCREEN = "selectInventoryItemScreen"
  const val TOP_APP_BAR = "selectInventoryItemTopAppBar"
  const val TITLE = "selectInventoryItemTitle"
  const val LOADING_INDICATOR = "selectInventoryItemLoadingIndicator"
  const val EMPTY_STATE = "selectInventoryItemEmptyState"
  const val ITEMS_GRID = "selectInventoryItemGrid"
  const val GO_BACK_BUTTON = "selectInventoryGoBackButton"
  const val SEARCH_FAB = "selectInventorySearchFab"

  fun getTestTagForItem(item: Item): String = "selectInventoryItem${item.itemUuid}"
}

/**
 * Screen that allows users to select items from their inventory to add to a post.
 *
 * @param postUuid The UUID of the post to add items to
 * @param selectInventoryItemViewModel ViewModel managing the selection state
 * @param onItemSelected Callback when an item is successfully added to the post
 * @param onGoBack Callback to navigate back
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectInventoryItemScreen(
    postUuid: String,
    selectInventoryItemViewModel: SelectInventoryItemViewModel = viewModel(),
    onItemSelected: () -> Unit = {},
    onGoBack: () -> Unit = {}
) {
  val context = LocalContext.current
  val uiState by selectInventoryItemViewModel.uiState.collectAsState()

  // Initialize the ViewModel with the postUuid
  LaunchedEffect(postUuid) { selectInventoryItemViewModel.initPostUuid(postUuid) }

  // Handle success message
  LaunchedEffect(uiState.successMessage) {
    uiState.successMessage?.let { message ->
      Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
      selectInventoryItemViewModel.clearMessages()
      onItemSelected()
    }
  }

  // Handle error message
  LaunchedEffect(uiState.errorMessage) {
    uiState.errorMessage?.let { message ->
      Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
      selectInventoryItemViewModel.clearMessages()
    }
  }

  Scaffold(
      modifier = Modifier.fillMaxSize().testTag(SelectInventoryItemScreenTestTags.SCREEN),
      topBar = {
        OOTDTopBar(
            modifier = Modifier.testTag(SelectInventoryItemScreenTestTags.TOP_APP_BAR),
            textModifier = Modifier.testTag(SelectInventoryItemScreenTestTags.TITLE),
            centerText = "INVENTORY",
            leftComposable = {
              BackArrow(
                  onBackClick = onGoBack,
                  modifier = Modifier.testTag(SelectInventoryItemScreenTestTags.GO_BACK_BUTTON))
            })
      },
      floatingActionButton = {
        FloatingActionButton(
            onClick = { selectInventoryItemViewModel.toggleSearch() },
            containerColor = if (uiState.isSearchActive) Red else Primary,
            modifier = Modifier.testTag(SelectInventoryItemScreenTestTags.SEARCH_FAB)) {
              Icon(
                  imageVector =
                      if (uiState.isSearchActive) Icons.Default.Close else Icons.Default.Search,
                  contentDescription =
                      if (uiState.isSearchActive) "Close Search" else "Search Item",
                  tint = White)
            }
      }) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
          if (uiState.isSearchActive) {
            InventorySearchBar(
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = { selectInventoryItemViewModel.updateSearchQuery(it) })
          }

          Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            when {
              uiState.isLoading -> {
                // Loading state
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                  CircularProgressIndicator(
                      modifier =
                          Modifier.testTag(SelectInventoryItemScreenTestTags.LOADING_INDICATOR),
                      color = Primary)
                }
              }
              uiState.filteredItems.isEmpty() -> {
                // Empty state
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                  Text(
                      text =
                          if (uiState.isSearchActive && uiState.searchQuery.isNotEmpty()) {
                            "No items match your search.\nTry a different search term."
                          } else {
                            "No items in your inventory yet.\nAdd items to your inventory first!"
                          },
                      style = Typography.bodyLarge,
                      color = if (uiState.isSearchActive) Secondary else Color.Gray,
                      textAlign = TextAlign.Center,
                      modifier =
                          Modifier.padding(32.dp)
                              .testTag(SelectInventoryItemScreenTestTags.EMPTY_STATE))
                }
              }
              else -> {
                InventoryGrid(
                    items = uiState.filteredItems,
                    onItemClick = { item -> selectInventoryItemViewModel.addItemToPost(item) },
                    starredItemIds = emptySet(),
                    onToggleStar = {},
                    modifier = Modifier.testTag(SelectInventoryItemScreenTestTags.ITEMS_GRID),
                    showStarToggle = false)
              }
            }
          }
        }
      }
}
