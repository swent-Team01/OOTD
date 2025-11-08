package com.android.ootd.ui.Inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.ootd.model.items.Item
import com.android.ootd.ui.navigation.NavigationActions
import com.android.ootd.ui.navigation.Screen
import com.android.ootd.ui.theme.Primary

object InventoryScreenTestTags {
  const val SCREEN = "inventoryScreen"
  const val TOP_APP_BAR = "inventoryTopAppBar"
  const val TITLE = "inventoryTitle"
  const val LOADING_INDICATOR = "inventoryLoadingIndicator"
  const val ERROR_MESSAGE = "inventoryErrorMessage"
  const val EMPTY_STATE = "inventoryEmptyState"
  const val ITEMS_GRID = "inventoryItemsGrid"
  const val ITEM_CARD = "inventoryItemCard"
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
    inventoryViewModel: InventoryViewModel = viewModel(),
    navigationActions: NavigationActions? = null,
    modifier: Modifier = Modifier
) {
  val uiState by inventoryViewModel.uiState.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }

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
        CenterAlignedTopAppBar(
            modifier = Modifier.testTag(InventoryScreenTestTags.TOP_APP_BAR),
            title = {
              Text(
                  text = "INVENTORY",
                  style =
                      MaterialTheme.typography.displayLarge.copy(
                          fontWeight = FontWeight.Bold, color = Primary),
                  modifier = Modifier.testTag(InventoryScreenTestTags.TITLE))
            })
      },
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
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
              // Empty state
              // ON A FUTURE PR YOU WILL BE ABLE TO ADD ITEMS FROM HERE
              Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No items in your inventory yet.\nAdd items from the fit check screen!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp).testTag(InventoryScreenTestTags.EMPTY_STATE))
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

// ON A LATER PR THEY WILL BE SORTED BY CATEGORIES
/**
 * Grid layout displaying inventory items.
 *
 * @param items List of items to display
 * @param onItemClick Callback when an item is clicked
 * @param modifier Modifier for styling
 */
@Composable
private fun InventoryGrid(
    items: List<Item>,
    onItemClick: (Item) -> Unit,
    modifier: Modifier = Modifier
) {
  LazyVerticalGrid(
      columns = GridCells.Fixed(3),
      modifier = modifier.fillMaxSize().testTag(InventoryScreenTestTags.ITEMS_GRID),
      contentPadding = PaddingValues(16.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items) { item -> InventoryItemCard(item = item, onClick = { onItemClick(item) }) }
      }
}

/**
 * Individual item card displaying a small rounded square with the item's image.
 *
 * @param item The item to display
 * @param onClick Callback when the item is clicked
 * @param modifier Modifier for styling
 */
@Composable
private fun InventoryItemCard(item: Item, onClick: () -> Unit, modifier: Modifier = Modifier) {
  Box(
      modifier =
          modifier
              .fillMaxWidth()
              .aspectRatio(1f)
              .clip(RoundedCornerShape(16.dp))
              .background(Color.LightGray.copy(alpha = 0.3f))
              .clickable(onClick = onClick)
              .testTag("${InventoryScreenTestTags.ITEM_CARD}_${item.itemUuid}"),
      contentAlignment = Alignment.Center) {
        if (item.image.imageUrl.isNotEmpty()) {
          AsyncImage(
              model = item.image.imageUrl,
              contentDescription = "Item image: ${item.category}",
              modifier = Modifier.fillMaxSize(),
              contentScale = ContentScale.Crop)
        } else {
          // Fallback for items without images
          Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center) {
                Text(
                    text = item.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(4.dp))
              }
        }
      }
}
