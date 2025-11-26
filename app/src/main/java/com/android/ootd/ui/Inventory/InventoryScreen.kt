package com.android.ootd.ui.Inventory

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.ootd.ui.navigation.NavigationActions
import com.android.ootd.ui.navigation.Screen
import com.android.ootd.ui.theme.Primary
import com.android.ootd.utils.composables.OOTDTopBar

object InventoryScreenTestTags {
  const val SCREEN = "inventoryScreen"
  const val TOP_APP_BAR = "inventoryTopAppBar"
  const val TITLE = "inventoryTitle"
  const val LOADING_INDICATOR = "inventoryLoadingIndicator"
  const val ERROR_MESSAGE = "inventoryErrorMessage"
  const val EMPTY_STATE = "inventoryEmptyState"
  const val ITEMS_GRID = "inventoryItemsGrid"
  const val ITEM_CARD = "inventoryItemCard"
  const val ADD_ITEM_FAB = "inventoryAddItemFab"
  const val SEARCH_FAB = "inventorySearchFab"
  const val SEARCH_BAR = "inventorySearchBar"
  const val SEARCH_FIELD = "inventorySearchField"
  const val CLOSE_SEARCH_BUTTON = "inventoryCloseSearchButton"
}

/**
 * Inventory screen that displays all items in the user's inventory.
 *
 * Shows items in a grid layout with small rounded square boxes displaying their images. When a user
 * clicks on an item, it navigates to the edit item screen.
 *
 * @param inventoryViewModel ViewModel managing the inventory state
 * @param navigationActions Navigation actions for screen transitions
 * @param modifier Modifier for styling
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    modifier: Modifier = Modifier,
    inventoryViewModel: InventoryViewModel = viewModel(),
    navigationActions: NavigationActions? = null,
) {
  val uiState by inventoryViewModel.uiState.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }
  val lifecycleOwner = LocalLifecycleOwner.current

  // Reload inventory when screen is resumed (e.g., coming back from AddItem screen)
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        inventoryViewModel.loadInventory()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  // Show error message if present
  LaunchedEffect(uiState.errorMessage) {
    uiState.errorMessage?.let { message ->
      snackbarHostState.showSnackbar(message)
      inventoryViewModel.clearError()
    }
  }

  Scaffold(
      modifier = modifier.fillMaxSize().testTag(InventoryScreenTestTags.SCREEN),
      topBar = {
        OOTDTopBar(
            modifier = Modifier.testTag(InventoryScreenTestTags.TOP_APP_BAR),
            textModifier = Modifier.testTag(InventoryScreenTestTags.TITLE),
            centerText = "INVENTORY")
      },
      floatingActionButton = {
        Column {
          FloatingActionButton(
              onClick = { navigationActions?.navigateTo(Screen.AddItemScreen("")) },
              containerColor = Primary,
              modifier = Modifier.testTag(InventoryScreenTestTags.ADD_ITEM_FAB)) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Item",
                    tint = MaterialTheme.colorScheme.onPrimary)
              }

          Spacer(modifier.padding(10.dp))

          FloatingActionButton(
              onClick = { inventoryViewModel.toggleSearch() },
              containerColor = if (uiState.isSearchActive) Red else Primary,
              modifier = Modifier.testTag(InventoryScreenTestTags.SEARCH_FAB)) {
                Icon(
                    imageVector =
                        if (uiState.isSearchActive) Icons.Default.Close else Icons.Default.Search,
                    contentDescription =
                        if (uiState.isSearchActive) "Close Search" else "Search Item",
                    tint = MaterialTheme.colorScheme.onPrimary)
              }
        }
      },
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
          // Search bar (shown when search is active)
          if (uiState.isSearchActive) {
            InventorySearchBar(
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = { inventoryViewModel.updateSearchQuery(it) })
          }

          // Content area
          Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            when {
              uiState.isLoading -> {
                // Loading state
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                  CircularProgressIndicator(
                      modifier = Modifier.testTag(InventoryScreenTestTags.LOADING_INDICATOR),
                      color = Primary)
                }
              }
              uiState.items.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                  Text(
                      text =
                          if (uiState.isSearchActive && uiState.searchQuery.isNotEmpty()) {
                            "No items match your search.\nTry a different search term."
                          } else {
                            "No items in your inventory yet.\n Add new items to your inventory and you will see them here!"
                          },
                      style = MaterialTheme.typography.bodyLarge,
                      color = Color.Gray,
                      textAlign = TextAlign.Center,
                      modifier =
                          Modifier.padding(32.dp).testTag(InventoryScreenTestTags.EMPTY_STATE))
                }
              }
              else -> {
                // Items grid
                InventoryGrid(
                    items = uiState.items,
                    onItemClick = { item ->
                      navigationActions?.navigateTo(Screen.EditItem(item.itemUuid))
                    })
              }
            }
          }
        }
      }
}
