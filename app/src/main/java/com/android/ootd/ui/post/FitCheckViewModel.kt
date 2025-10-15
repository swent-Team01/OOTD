package com.android.ootd.ui.post

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FitCheckUIState(
    val image: Uri = Uri.EMPTY,
    val description: String = "",
    val errorMessage: String? = null,
    val invalidPhotoMsg: String? = null,
    // val addedItemIds: List<String> = emptyList(), // optional for later steps
) {
  val isPhotoValid: Boolean
    get() = invalidPhotoMsg == null && image != Uri.EMPTY
}

class FitCheckViewModel : ViewModel() {
  private val _uiState = MutableStateFlow(FitCheckUIState())
  val uiState: StateFlow<FitCheckUIState> = _uiState.asStateFlow()

  fun setPhoto(uri: Uri) {
    _uiState.value =
        _uiState.value.copy(
            image = uri,
            // 👇 Clear error if user now added a valid photo
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

  //  fun clearDraft() {
  //    _uiState.value = FitCheckUIState()
  //  }
}
