package com.android.ootd.ui.post

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.android.ootd.model.post.OutfitPostRepository
import com.android.ootd.model.post.OutfitPostRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    private val repository: OutfitPostRepository = OutfitPostRepositoryProvider.repository
) : ViewModel() {
  private val _uiState = MutableStateFlow(FitCheckUIState())
  val uiState: StateFlow<FitCheckUIState> = _uiState.asStateFlow()

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
