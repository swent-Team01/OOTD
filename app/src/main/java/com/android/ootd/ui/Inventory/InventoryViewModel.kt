package com.android.ootd.ui.Inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.account.AccountRepositoryProvider
import com.android.ootd.model.items.Item
import com.android.ootd.model.items.ItemsRepository
import com.android.ootd.model.items.ItemsRepositoryProvider
import com.android.ootd.utils.CategoryNormalizer
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
 * @property searchQuery Current search query text
 * @property isSearchActive Whether search mode is active
 */
data class InventoryUIState(
    val items: List<Item> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false
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

  // Store all items separately for filtering
  private var allItems: List<Item> = emptyList()

  companion object {
    private const val FIRESTORE_TIMEOUT_MS = 2_000L
  }

  init {
    loadInventory()
  }

  /**
   * Loads the user's inventory items using offline-first pattern.
   *
   * First loads from cache immediately (non-blocking), then fetches fresh data in the background.
   * This provides instant UI updates from cache while ensuring data freshness.
   */
  fun loadInventory() {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(errorMessage = null)
      try {
        val currentUserId = Firebase.auth.currentUser?.uid ?: throw Exception("User not logged in")

        val cachedItemIds = accountRepository.getItemsList(currentUserId)

        if (cachedItemIds.isNotEmpty()) {
          val cachedItems = itemsRepository.getItemsByIds(cachedItemIds)

          if (cachedItems.isNotEmpty()) {
            // Display cached items immediately
            val sortedCachedItems = sortItemsByCategory(cachedItems)
            allItems = sortedCachedItems
            _uiState.value = _uiState.value.copy(items = sortedCachedItems, isLoading = false)
          }
        }

        viewModelScope.launch {
          try {
            val freshItemIds =
                kotlinx.coroutines.withTimeoutOrNull(FIRESTORE_TIMEOUT_MS) {
                  accountRepository.getItemsList(currentUserId)
                } ?: cachedItemIds

            val freshItems =
                kotlinx.coroutines.withTimeoutOrNull(FIRESTORE_TIMEOUT_MS) {
                  itemsRepository.getItemsByIds(freshItemIds)
                } ?: emptyList()

            if (freshItems.isNotEmpty() && freshItems != allItems) {
              val sortedFreshItems = sortItemsByCategory(freshItems)
              allItems = sortedFreshItems
              // Apply current search filter if active
              val displayItems =
                  if (_uiState.value.isSearchActive) {
                    filterItems(sortedFreshItems, _uiState.value.searchQuery)
                  } else {
                    sortedFreshItems
                  }
              _uiState.value = _uiState.value.copy(items = displayItems, isLoading = false)
            }
          } catch (e: Exception) {
            // Silently fail background refresh -> user already sees cached data
            if (_uiState.value.items.isEmpty()) {
              _uiState.value =
                  _uiState.value.copy(
                      isLoading = false, errorMessage = "Failed to load inventory: ${e.message}")
            }
          }
        }
      } catch (e: Exception) {
        _uiState.value =
            _uiState.value.copy(
                isLoading = false, errorMessage = "Failed to load inventory: ${e.message}")
      }
    }
  }

  /**
   * Sorts items by category in the order: Clothing → Shoes → Accessories → Bags. Items with unknown
   * categories are placed at the end.
   */
  private fun sortItemsByCategory(items: List<Item>): List<Item> {
    return items.sortedBy { item -> CategoryNormalizer.getSortOrder(item.category) }
  }

  /** Clears the error message. */
  fun clearError() {
    _uiState.value = _uiState.value.copy(errorMessage = null)
  }

  /** Toggles search mode on/off. */
  fun toggleSearch() {
    val newSearchActive = !_uiState.value.isSearchActive
    _uiState.value =
        _uiState.value.copy(
            isSearchActive = newSearchActive,
            searchQuery = if (newSearchActive) _uiState.value.searchQuery else "",
            items =
                if (newSearchActive) filterItems(allItems, _uiState.value.searchQuery)
                else allItems)
  }

  /** Updates the search query and filters items. */
  fun updateSearchQuery(query: String) {
    _uiState.value = _uiState.value.copy(searchQuery = query, items = filterItems(allItems, query))
  }

  /**
   * Filters items based on search query. Searches across brand, type, category, style, and notes.
   */
  private fun filterItems(items: List<Item>, query: String): List<Item> {
    if (query.isBlank()) return items

    val lowerQuery = query.lowercase().trim()
    return items.filter { item ->
      item.brand?.lowercase()?.contains(lowerQuery) == true ||
          item.type?.lowercase()?.contains(lowerQuery) == true ||
          item.category.lowercase().contains(lowerQuery) ||
          item.style?.lowercase()?.contains(lowerQuery) == true ||
          item.notes?.lowercase()?.contains(lowerQuery) == true
    }
  }
}
