package com.android.ootd.ui.post

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.items.ItemsRepository
import com.android.ootd.model.items.ItemsRepositoryProvider
import com.android.ootd.model.map.Location
import com.android.ootd.model.map.emptyLocation
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the FitCheck screen.
 *
 * This state represents the current data being entered by the user before creating an outfit post,
 * including the selected image and description.
 *
 * @property image The selected outfit photo as a [Uri].
 * @property description The text description entered by the user.
 * @property errorMessage An optional error message displayed to the user.
 * @property invalidPhotoMsg An optional validation message if the selected photo is invalid.
 * @property isLoading Indicates whether a background operation (e.g. saving or uploading) is
 *   ongoing.
 * @property successMessage A success message to show when a task completes successfully.
 * @property savedPostId The ID of the saved post if available.
 */
data class FitCheckUIState(
    val image: Uri = Uri.EMPTY,
    val description: String = "",
    val location: Location = emptyLocation,
    val errorMessage: String? = null,
    val invalidPhotoMsg: String? = null,
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val savedPostId: String? = null
) {
  /**
   * Indicates whether the currently selected photo is valid.
   *
   * A photo is considered valid if it is not empty and there is no validation error message
   * ([invalidPhotoMsg]).
   */
  val isPhotoValid: Boolean
    get() = invalidPhotoMsg == null && image != Uri.EMPTY
}

/**
 * ViewModel for the FitCheck screen.
 *
 * Responsible for managing the outfit photo selection and description input before moving to the
 * preview or publishing steps.
 */
class FitCheckViewModel(
    private val repository: ItemsRepository = ItemsRepositoryProvider.repository
) : ViewModel() {
  private val _uiState = MutableStateFlow(FitCheckUIState())
  val uiState: StateFlow<FitCheckUIState> = _uiState.asStateFlow()

  /**
   * Deletes all items associated with a given post. This function is typically used when a user
   * decides to discard a draft
   *
   * @param postUuid The unique identifier of the post whose items should be deleted.
   */
  fun deleteItemsForPost(postUuid: String) {
    viewModelScope.launch {
      try {
        repository.deletePostItems(postUuid)
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

  /**
   * Updates the description entered by the user.
   *
   * @param description The new description text.
   */
  fun setDescription(description: String) {
    _uiState.value = _uiState.value.copy(description = description)
  }

  /**
   * Updates the location selected by the user.
   *
   * @param location The location selected by the user, or emptyLocation if none selected.
   */
  fun setLocation(location: Location) {
    _uiState.value = _uiState.value.copy(location = location)
  }

  /**
   * Sets an error message in the UI state.
   *
   * @param msg The error message to be displayed.
   */
  fun setErrorMsg(msg: String) {
    _uiState.value = _uiState.value.copy(errorMessage = msg)
  }

  /** Clears the error message in the UI state. */
  fun clearError() {
    _uiState.value = _uiState.value.copy(errorMessage = null)
  }

  fun generateDescription(bitmap: Bitmap) {
    _uiState.value = _uiState.value.copy(isLoading = true)
    viewModelScope.launch {
      try {
        val model =
            Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel("gemini-2.5-flash")

        val response =
            model.generateContent(
                content {
                  image(bitmap)
                  text(
                      "Write an engaging description for a social media app of the image you got as input. Make it joyfull and include any detailed you see in the image. Only output around 100 characters. Do not add anything else")
                })

        _uiState.value = _uiState.value.copy(description = response.text ?: "", isLoading = false)
      } catch (e: Exception) {
        _uiState.value =
            _uiState.value.copy(
                errorMessage = "Failed to generate description: ${e.message}", isLoading = false)
      }
    }
  }
}
