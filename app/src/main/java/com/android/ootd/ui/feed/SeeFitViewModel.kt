package com.android.ootd.ui.feed

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.account.AccountRepositoryProvider
import com.android.ootd.model.authentication.AccountService
import com.android.ootd.model.authentication.AccountServiceFirebase
import com.android.ootd.model.feed.FeedRepository
import com.android.ootd.model.feed.FeedRepositoryProvider
import com.android.ootd.model.items.Item
import com.android.ootd.model.items.ItemsRepository
import com.android.ootd.model.items.ItemsRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the SeeFit screen.
 *
 * This state represents the current data of items associated to the specific post, which is
 * assigned with postUuid, being displayed in the SeeFit screen,
 *
 * @property items The list of item IDs associated with the outfit post.
 * @property postUuid The unique identifier for the outfit post.
 * @property isLoading Indicates whether a background operation (e.g. fetching data) is ongoing.
 * @property errorMessage An optional error message displayed to the user.
 * @property postOwnerId The owner ID of the post
 * @property isOwner Whether the current user owns the post
 */
data class SeeFitUIState(
    val items: List<Item> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val postOwnerId: String = "",
    val isOwner: Boolean = false,
    val starredItemIds: Set<String> = emptySet()
)

class SeeFitViewModel(
    private val itemsRepository: ItemsRepository = ItemsRepositoryProvider.repository,
    private val feedRepository: FeedRepository = FeedRepositoryProvider.repository,
    private val accountService: AccountService = AccountServiceFirebase(),
    private val accountRepository: AccountRepository = AccountRepositoryProvider.repository
) : ViewModel() {

  private companion object {
    const val TAG = "SeeFitViewModel"
  }

  private val _uiState = MutableStateFlow(SeeFitUIState())
  val uiState: StateFlow<SeeFitUIState> = _uiState.asStateFlow()

  /**
   * Fetches items associated with the given post UUID and updates the UI state.
   *
   * @param postUuid The unique identifier for the outfit post.
   */
  fun getItemsForPost(postUuid: String) {
    if (postUuid.isEmpty()) {
      setErrorMessage("Unable to load this fit please try again later.")
      Log.w(TAG, "Post UUID is empty. Cannot fetch items.")
      return
    }
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
      try {
        // Gets the post owner ID otherwise it throws an exception caught below
        val postOwner = feedRepository.getPostById(postUuid)?.ownerId.orEmpty()
        val items = itemsRepository.getFriendItemsForPost(postUuid, postOwner)
        // Get the current user ID to determine ownership
        val currentUserId = accountService.currentUserId
        // Determine if the current user is the owner of the post
        val isOwner = currentUserId.isNotEmpty() && currentUserId == postOwner
        _uiState.value =
            _uiState.value.copy(
                items = items,
                postOwnerId = postOwner,
                isOwner = isOwner,
                isLoading = false,
                errorMessage = null)
        refreshStarredItems()
      } catch (e: Exception) {
        Log.e("SeeFitViewModel", "Failed to load items for post", e)
        _uiState.value =
            _uiState.value.copy(
                isLoading = false,
                errorMessage = "Unable to load this fit. Please try again later.")
      }
    }
  }

  /** Sets an error message in the UI state. */
  fun setErrorMessage(message: String) {
    _uiState.value = _uiState.value.copy(errorMessage = message)
  }

  /** Clears any existing error message in the UI state. */
  fun clearMessage() {
    _uiState.value = _uiState.value.copy(errorMessage = null)
  }

  /**
   * Loads the starred item ids for the current user so the UI can highlight wishlist entries.
   *
   * This is invoked both on entering the screen and when toggles succeed so See Fit cards always
   * reflect the same state as the account wishlist.
   */
  fun refreshStarredItems() {
    viewModelScope.launch {
      val currentUserId = accountService.currentUserId
      if (currentUserId.isBlank()) return@launch
      try {
        val starred = accountRepository.getStarredItems(currentUserId).toSet()
        _uiState.value = _uiState.value.copy(starredItemIds = starred)
      } catch (e: Exception) {
        Log.w(TAG, "Failed to refresh starred items: ${e.message}")
      }
    }
  }

  /**
   * Stars or unstars [item] for the current user.
   *
   * The repository updates Firestore (or queues it offline) and we mirror the updated id set so the
   * star icon reflects the latest status immediately.
   */
  fun toggleStar(item: Item) {
    val itemId = item.itemUuid
    if (itemId.isBlank()) return
    viewModelScope.launch {
      try {
        val updated = accountRepository.toggleStarredItem(itemId).toSet()
        Log.d(TAG, "Toggled star for $itemId -> ${updated.contains(itemId)}")
        _uiState.value = _uiState.value.copy(starredItemIds = updated)
      } catch (e: Exception) {
        Log.w(TAG, "Failed to toggle star: ${e.message}")
        setErrorMessage("Couldn't update wishlist. Please try again.")
      }
    }
  }
}
