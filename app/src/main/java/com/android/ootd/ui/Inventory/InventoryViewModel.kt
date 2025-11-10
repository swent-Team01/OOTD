package com.android.ootd.ui.Inventory

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
 * UI state for the Inventory screen.
 *
 * @property items List of items in the user's inventory
 * @property isLoading Whether the items are currently being loaded
 * @property errorMessage Error message if loading failed
 */
data class InventoryUIState(
    val items: List<Item> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ViewModel for the Inventory screen.
 *
 * Manages the state of the user's inventory items, loading them from both the account repository
 * (for the list of item IDs) and the items repository (for the actual item data).
 *
 * @property accountRepository Repository for account operations
 * @property itemsRepository Repository for item operations
 */
class InventoryViewModel(
    private val accountRepository: AccountRepository = AccountRepositoryProvider.repository,
    private val itemsRepository: ItemsRepository = ItemsRepositoryProvider.repository
) : ViewModel() {

  private val _uiState = MutableStateFlow(InventoryUIState())
  val uiState: StateFlow<InventoryUIState> = _uiState.asStateFlow()

  init {
    loadInventory()
  }

  /** Loads the user's inventory items. */
  fun loadInventory() {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
      try {
        val currentUserId = Firebase.auth.currentUser?.uid ?: throw Exception("User not logged in")

        // Get the list of item IDs from the account
        val itemIds = accountRepository.getItemsList(currentUserId)

        // Fetch all items using the batch method
        val items = itemsRepository.getItemsByIds(itemIds)

        // Sort items by category using the predefined order
        val sortedItems = sortItemsByCategory(items)

        _uiState.value = _uiState.value.copy(items = sortedItems, isLoading = false)
      } catch (e: Exception) {
        _uiState.value =
            _uiState.value.copy(
                isLoading = false, errorMessage = "Failed to load inventory: ${e.message}")
      }
    }
  }

  /**
   * Sorts items by category in the order: Clothing, Shoes, Accessories, Bags. Items with unknown
   * categories are placed at the end.
   */
  private fun sortItemsByCategory(items: List<Item>): List<Item> {
    val categoryOrder = listOf("Clothing", "Shoes", "Accessories", "Bags")

    return items.sortedWith(
        compareBy { item ->
          val normalizedCategory = normalizeCategoryName(item.category)
          val index =
              categoryOrder.indexOfFirst { it.equals(normalizedCategory, ignoreCase = true) }
          if (index >= 0) index else categoryOrder.size
        })
  }

  /**
   * Normalizes category names to handle variations. E.g., "clothes" -> "Clothing", "shoe" ->
   * "Shoes"
   */
  private fun normalizeCategoryName(category: String): String {
    return when (category.trim().lowercase()) {
      "clothes",
      "clothing" -> "Clothing"
      "shoes",
      "shoe" -> "Shoes"
      "bags",
      "bag" -> "Bags"
      "accessories",
      "accessory" -> "Accessories"
      else -> category.trim()
    }
  }

  /** Clears the error message. */
  fun clearError() {
    _uiState.value = _uiState.value.copy(errorMessage = null)
  }
}
