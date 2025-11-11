package com.android.ootd.ui.post

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.ootd.model.items.Item
import com.android.ootd.ui.Inventory.InventoryGrid
import com.android.ootd.ui.theme.Primary

object SelectInventoryItemScreenTestTags {
  const val SCREEN = "selectInventoryItemScreen"
  const val TOP_APP_BAR = "selectInventoryItemTopAppBar"
  const val TITLE = "selectInventoryItemTitle"
  const val LOADING_INDICATOR = "selectInventoryItemLoadingIndicator"
  const val EMPTY_STATE = "selectInventoryItemEmptyState"
  const val ITEMS_GRID = "selectInventoryItemGrid"
  const val GO_BACK_BUTTON = "selectInventoryGoBackButton"

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
        CenterAlignedTopAppBar(
            modifier = Modifier.testTag(SelectInventoryItemScreenTestTags.TOP_APP_BAR),
            title = {
              Text(
                  text = "INVENTORY",
                  style =
                      MaterialTheme.typography.displayLarge.copy(
                          fontWeight = FontWeight.Bold, color = Primary),
                  modifier = Modifier.testTag(SelectInventoryItemScreenTestTags.TITLE))
            },
            navigationIcon = {
              Box(modifier = Modifier.padding(start = 4.dp), contentAlignment = Alignment.Center) {
                IconButton(
                    onClick = onGoBack,
                    modifier = Modifier.testTag(SelectInventoryItemScreenTestTags.GO_BACK_BUTTON)) {
                      Icon(
                          imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                          contentDescription = "Back",
                          tint = MaterialTheme.colorScheme.tertiary)
                    }
              }
            })
      }) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
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
            uiState.availableItems.isEmpty() -> {
              // Empty state
              Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No items in your inventory yet.\nAdd items to your inventory first!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier.padding(32.dp)
                            .testTag(SelectInventoryItemScreenTestTags.EMPTY_STATE))
              }
            }
            else -> {
              InventoryGrid(
                  items = uiState.availableItems,
                  onItemClick = { item -> selectInventoryItemViewModel.addItemToPost(item) },
                  modifier = Modifier.testTag(SelectInventoryItemScreenTestTags.ITEMS_GRID))
            }
          }
        }
      }
}
