package com.android.ootd.ui.post

import android.net.Uri
import android.util.Log
import android.webkit.URLUtil.isValidUrl
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.items.FirebaseImageUploader
import com.android.ootd.model.items.ImageData
import com.android.ootd.model.items.Item
import com.android.ootd.model.items.ItemsRepository
import com.android.ootd.model.items.ItemsRepositoryProvider
import com.android.ootd.model.items.Material
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the EditItems screen. This state holds the data needed to edit an existing Clothing
 * item.
 */
data class EditItemsUIState(
    val itemId: String = "",
    val image: ImageData = ImageData("", ""),
    val localPhotoUri: Uri? = null,
    val category: String = "",
    val type: String = "",
    val brand: String = "",
    val price: Double = 0.0,
    val material: List<Material> = emptyList(),
    val materialText: String = "",
    val link: String = "",
    val errorMessage: String? = null,
    val invalidPhotoMsg: String? = null,
    val invalidCategory: String? = null,
    val suggestions: List<String> = emptyList(),
    val isSaveSuccessful: Boolean = false,
    val isDeleteSuccessful: Boolean = false,
    val ownerId: String = "",
    val isLoading: Boolean = false
) {
  val isEditValid: Boolean
    get() =
        invalidPhotoMsg == null &&
            invalidCategory == null &&
            (localPhotoUri != null || image.imageUrl.isNotEmpty()) &&
            category.isNotEmpty()
}

/**
 * ViewModel for the EditItems screen.
 *
 * Manages the state and business logic for editing, saving, and deleting items. Handles input
 * validation and provides type suggestions based on category.
 *
 * @property repository The repository used for item operations.
 */
open class EditItemsViewModel(
    private val repository: ItemsRepository = ItemsRepositoryProvider.repository
) : BaseItemViewModel() {

  private val _uiState = MutableStateFlow(EditItemsUIState())
  open val uiState: StateFlow<EditItemsUIState> = _uiState.asStateFlow()

  /** Clears the error message in the UI state. */
  override fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMessage = null)
  }

  /**
   * Sets the error message in the UI state.
   *
   * @param msg The error message to display.
   */
  override fun setErrorMsg(msg: String) {
    _uiState.value = _uiState.value.copy(errorMessage = msg)
  }

  override fun updateType(type: String) {
    _uiState.value = _uiState.value.copy(type = type)
  }

  override fun updateBrand(brand: String) {
    _uiState.value = _uiState.value.copy(brand = brand)
  }

  override fun updateLink(link: String) {
    _uiState.value = _uiState.value.copy(link = link)
  }

  override fun updateMaterial(materialText: String, materials: List<Material>) {
    _uiState.value = _uiState.value.copy(materialText = materialText, material = materials)
  }

  override fun getCurrentCategory(): String = _uiState.value.category

  override fun updateSuggestions(suggestions: List<String>) {
    _uiState.value = _uiState.value.copy(suggestions = suggestions)
  }

  override fun updateCategorySuggestionsState(suggestions: List<String>) {
    // Not used in EditItemsViewModel
  }

  /**
   * Loads an existing item into the UI state for editing.
   *
   * @param item The item to load.
   */
  fun loadItem(item: Item) {
    val materialText =
        item.material.filterNotNull().joinToString(", ") { "${it.name} ${it.percentage}%" }
    _uiState.value =
        EditItemsUIState(
            itemId = item.itemUuid,
            image = item.image,
            category = item.category,
            type = item.type ?: "",
            brand = item.brand ?: "",
            price = item.price ?: 0.0,
            material = item.material.filterNotNull(),
            materialText = materialText,
            link = item.link ?: "",
            ownerId = item.ownerId)
  }

  /** Loads an item by its UUID directly from the repository. */
  fun loadItemById(itemUuid: String) {
    viewModelScope.launch {
      try {
        val item = repository.getItemById(itemUuid)
        loadItem(item)
      } catch (e: Exception) {
        setErrorMsg("Failed to load item: ${e.message}")
      }
    }
  }

  fun onSaveItemClick() {
    val state = _uiState.value

    if (state.link.isNotEmpty() && !isValidUrl(state.link)) {
      setErrorMsg("Please enter a valid URL.")
      return
    }

    if (!state.isEditValid) {
      setErrorMsg("Please fill in all required fields.")
      return
    }

    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true)
      val finalImage =
          if (state.localPhotoUri != null)
              FirebaseImageUploader.uploadImage(state.localPhotoUri, state.itemId)
          else state.image
      if (finalImage.imageUrl.isEmpty()) {
        setErrorMsg("Please select a photo.")
        _uiState.value = _uiState.value.copy(isSaveSuccessful = false)
        return@launch
      }
      val updatedItem =
          Item(
              itemUuid = state.itemId,
              image = finalImage,
              category = state.category,
              type = state.type,
              brand = state.brand,
              price = state.price,
              material = state.material,
              link = state.link,
              ownerId = state.ownerId)

      try {
        _uiState.value =
            _uiState.value.copy(image = finalImage, errorMessage = null, isSaveSuccessful = true)
        repository.editItem(updatedItem.itemUuid, updatedItem)
      } catch (e: Exception) {
        setErrorMsg("Failed to update item: ${e.message}")
      } finally {
        _uiState.value = _uiState.value.copy(isLoading = false)
      }
    }
  }

  /** Deletes the current item from the repository. Shows an error if no item is selected. */
  fun deleteItem() {
    val state = _uiState.value
    if (state.itemId.isEmpty()) {
      setErrorMsg("No item to delete.")
      return
    }

    viewModelScope.launch {
      try {
        repository.deleteItem(state.itemId)
        val deleted = FirebaseImageUploader.deleteImage(state.image.imageId)
        if (!deleted) {
          Log.w("EditItemsViewModel", "Image deletion failed or image not found.")
        }
        _uiState.value = _uiState.value.copy(isDeleteSuccessful = true)
      } catch (e: Exception) {
        setErrorMsg("Failed to delete item: ${e.message}")
      }
    }
  }

  /**
   * Sets the photo URI in the UI state.
   *
   * @param uri The URI of the selected photo.
   */
  fun setPhoto(uri: Uri) {

    if (uri == Uri.EMPTY) {
      _uiState.value =
          _uiState.value.copy(
              localPhotoUri = null,
              image = ImageData("", ""),
              invalidPhotoMsg = "Please select a photo.")
    } else {
      _uiState.value =
          _uiState.value.copy(
              localPhotoUri = uri, image = ImageData("", ""), invalidPhotoMsg = null)
    }
  }

  /**
   * Sets the category in the UI state.
   *
   * @param category The category name.
   */
  fun setCategory(category: String) {
    _uiState.value =
        _uiState.value.copy(
            category = category,
            invalidCategory = if (category.isEmpty()) "Please select a category." else null)
  }

  /**
   * Sets the price in the UI state.
   *
   * @param price The price value.
   */
  fun setPrice(price: Double) {
    _uiState.value = _uiState.value.copy(price = price)
  }
}
