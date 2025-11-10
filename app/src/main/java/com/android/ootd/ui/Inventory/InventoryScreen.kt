package com.android.ootd.ui.Inventory

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
