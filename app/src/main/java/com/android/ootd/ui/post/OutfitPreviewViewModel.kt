package com.android.ootd.ui.post

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.items.Item
import com.android.ootd.model.items.ItemsRepository
import com.android.ootd.model.items.ItemsRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the PreviewScreen.
 *
 * This state holds the data needed to display a preview of the outfit post before submission.
 */
data class PreviewUIState(
    val imageUri: String = "",
    val description: String = "",
    val items: List<Item> = emptyList(),
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
)

/**
 * ViewModel for the PreviewScreen.
 *
 * Responsible for managing the state by fetching and providing data for items from the Items
 * [ItemsRepository]
 *
 * @property itemsRepository The repository used to fetch and manage items.
 */
class OutfitPreviewViewModel(
    private val itemsRepository: ItemsRepository = ItemsRepositoryProvider.repository
    /** Repository for the photo also */
) : ViewModel() {

  private val _uiState = MutableStateFlow(PreviewUIState())
  val uiState: StateFlow<PreviewUIState> = _uiState.asStateFlow()

  init {
    loadOutfitPost()
  }

  /** Clears the error message in the UI state */
  fun clearErrorMessage() {
    _uiState.value = _uiState.value.copy(errorMessage = null)
  }

  /** Sets an error message in the UI state */
  private fun setErrorMessage(message: String) {
    _uiState.value = _uiState.value.copy(errorMessage = message)
  }

  private fun loadOutfitPost() {

    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true)
      try {
        val items = itemsRepository.getAllItems()
        _uiState.value = PreviewUIState(items = items, isLoading = false)
      } catch (e: Exception) {
        Log.e("OutfitPreviewViewModel", "Error fetching items", e)
        setErrorMessage("Failed to load items: ${e.message}")
      }
    }
  }
}
