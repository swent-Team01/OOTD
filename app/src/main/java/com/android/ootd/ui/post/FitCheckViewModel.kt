package com.android.ootd.ui.post

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.items.ItemsRepository
import com.android.ootd.model.items.ItemsRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FitCheckUIState(
    val image: Uri = Uri.EMPTY,
    val description: String = "",
    val errorMessage: String? = null,
    val invalidPhotoMsg: String? = null,
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val savedPostId: String? = null
) {
  val isPhotoValid: Boolean
    get() = invalidPhotoMsg == null && image != Uri.EMPTY
}

class FitCheckViewModel(
    private val repository: ItemsRepository = ItemsRepositoryProvider.repository
) : ViewModel() {
  private val _uiState = MutableStateFlow(FitCheckUIState())
  val uiState: StateFlow<FitCheckUIState> = _uiState.asStateFlow()

  fun deleteItemsForPost(postUuid: String) {
    viewModelScope.launch {
      try {
        val itemsRepo = ItemsRepositoryProvider.repository
        itemsRepo.deletePostItems(postUuid)
      } catch (e: Exception) {
        Log.e("FitCheckViewModel", "Failed to delete items for post: $postUuid", e)
      }
    }
  }

  fun setPhoto(uri: Uri) {
    _uiState.value =
        _uiState.value.copy(
            image = uri,
            errorMessage =
                if (uri == Uri.EMPTY) {
                  "Please select a photo before continuing."
                } else {
                  null
                })
  }

  fun setDescription(description: String) {
    _uiState.value = _uiState.value.copy(description = description)
  }

  fun setErrorMsg(msg: String) {
    _uiState.value = _uiState.value.copy(errorMessage = msg)
  }

  fun clearError() {
    _uiState.value = _uiState.value.copy(errorMessage = null)
  }
}
