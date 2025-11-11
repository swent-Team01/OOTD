package com.android.ootd.ui.post

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.account.AccountRepositoryProvider
import com.android.ootd.model.items.Item
import com.android.ootd.model.items.ItemsRepository
import com.android.ootd.model.items.ItemsRepositoryProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the SelectInventoryItemScreen.
 *
 * @property postUuid The UUID of the post to add items to
 * @property availableItems List of items from inventory that can be added to the post
 * @property isLoading Whether data is currently being loaded
 * @property errorMessage Error message if an operation failed
 * @property successMessage Success message when an item is added successfully
 */
data class SelectInventoryItemUIState(
    val postUuid: String = "",
    val availableItems: List<Item> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

/**
 * ViewModel for the SelectInventoryItemScreen.
 *
 * Manages the state for selecting items from the user's inventory to add to a post. Filters out
 * items that are already associated with the current post.
 *
 * @property accountRepository Repository for account operations
 * @property itemsRepository Repository for item operations
 */
class SelectInventoryItemViewModel(
    private val accountRepository: AccountRepository = AccountRepositoryProvider.repository,
    private val itemsRepository: ItemsRepository = ItemsRepositoryProvider.repository
) : ViewModel() {

  private val _uiState = MutableStateFlow(SelectInventoryItemUIState())
  val uiState: StateFlow<SelectInventoryItemUIState> = _uiState.asStateFlow()

  /**
   * Initializes the ViewModel with the post UUID and loads available items.
   *
   * @param postUuid The UUID of the post to add items to
   */
  fun initPostUuid(postUuid: String) {
    if (_uiState.value.postUuid == postUuid) return

    _uiState.value = _uiState.value.copy(postUuid = postUuid)
    loadAvailableItems()
  }

  /**
   * Loads items from the user's inventory, excluding items already associated with the current
   * post.
   */
  private fun loadAvailableItems() {
    val postUuid = _uiState.value.postUuid
    if (postUuid.isEmpty()) {
      Log.w("SelectInventoryItemViewModel", "loadAvailableItems called with empty postUuid")
      return
    }

    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
      try {
        val currentUserId = Firebase.auth.currentUser?.uid ?: throw Exception("User not logged in")

        // Get the list of item IDs from the account
        val itemIds = accountRepository.getItemsList(currentUserId)

        // Fetch all items from inventory
        val allItems = itemsRepository.getItemsByIds(itemIds)

        // Get items already associated with this post
        val postItems = itemsRepository.getAssociatedItems(postUuid)
        val postItemIds = postItems.map { it.itemUuid }.toSet()

        // Filter out items already in the post
        val availableItems = allItems.filter { it.itemUuid !in postItemIds }

        _uiState.value = _uiState.value.copy(availableItems = availableItems, isLoading = false)
      } catch (e: Exception) {
        Log.e("SelectInventoryItemViewModel", "Error loading items", e)
        _uiState.value =
            _uiState.value.copy(
                isLoading = false, errorMessage = "Failed to load inventory: ${e.message}")
      }
    }
  }

  /**
   * Adds an item from inventory to the current post by updating the item's postUuids list.
   *
   * @param item The item to add to the post
   */
  fun addItemToPost(item: Item) {
    val postUuid = _uiState.value.postUuid
    if (postUuid.isEmpty()) {
      _uiState.value = _uiState.value.copy(errorMessage = "Invalid post ID")
      return
    }

    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true)
      try {
        // Update the item to include this post in its postUuids list
        val updatedItem = item.copy(postUuids = item.postUuids + postUuid)
        itemsRepository.editItem(item.itemUuid, updatedItem)

        _uiState.value =
            _uiState.value.copy(
                isLoading = false, successMessage = "Item added to outfit successfully!")
      } catch (e: Exception) {
        Log.e("SelectInventoryItemViewModel", "Error adding item to post", e)
        _uiState.value =
            _uiState.value.copy(
                isLoading = false, errorMessage = "Failed to add item: ${e.message}")
      }
    }
  }

  /** Clears error and success messages. */
  fun clearMessages() {
    _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
  }
}
