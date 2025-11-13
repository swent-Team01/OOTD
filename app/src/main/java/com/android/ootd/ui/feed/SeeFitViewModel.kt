package com.android.ootd.ui.feed

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
 */
data class SeeFitUIState(
    val items: List<Item> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class SeeFitViewModel(
    private val itemsRepository: ItemsRepository = ItemsRepositoryProvider.repository,
    private val feedRepository: FeedRepository = FeedRepositoryProvider.repository
) : ViewModel() {

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
      Log.w("SeeFitViewModel", "Post UUID is empty. Cannot fetch items.")
      return
    }
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
      try {
        val friendId = feedRepository.getPostById(postUuid)?.ownerId
        val items = itemsRepository.getFriendItemsForPost(postUuid, friendId ?: "")
        _uiState.value = _uiState.value.copy(items = items, isLoading = false, errorMessage = null)
      } catch (e: Exception) {
        _uiState.value =
            _uiState.value.copy(
                isLoading = false, errorMessage = "Failed to load items: ${e.message}")
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
}
