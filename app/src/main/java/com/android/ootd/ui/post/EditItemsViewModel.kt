package com.android.ootd.ui.post

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.URLUtil.isValidUrl
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.items.FirebaseImageUploader
import com.android.ootd.model.items.ImageData
import com.android.ootd.model.items.Item
import com.android.ootd.model.items.ItemsRepository
import com.android.ootd.model.items.ItemsRepositoryProvider
import com.android.ootd.model.items.Material
import com.android.ootd.utils.TypeSuggestionsLoader
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
) : ViewModel() {

  private val _uiState = MutableStateFlow(EditItemsUIState())
  open val uiState: StateFlow<EditItemsUIState> = _uiState.asStateFlow()

  private var typeSuggestions: Map<String, List<String>> = emptyMap()

  /**
   * Initializes the type suggestions from a YAML file.
   *
   * Should be called from the composable with the context.
   *
   * @param context The Android context used to load suggestions.
   */
  fun initTypeSuggestions(context: Context) {
    typeSuggestions = TypeSuggestionsLoader.loadTypeSuggestions(context)
  }

  /** Clears the error message in the UI state. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMessage = null)
  }

  /**
   * Sets the error message in the UI state.
   *
   * @param msg The error message to display.
   */
  fun setErrorMsg(msg: String) {
    _uiState.value = _uiState.value.copy(errorMessage = msg)
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
   * Sets the type in the UI state.
   *
   * @param type The type name.
   */
  fun setType(type: String) {
    _uiState.value = _uiState.value.copy(type = type)
  }

  /**
   * Sets the brand in the UI state.
   *
   * @param brand The brand name.
   */
  fun setBrand(brand: String) {
    _uiState.value = _uiState.value.copy(brand = brand)
  }

  /**
   * Sets the price in the UI state.
   *
   * @param price The price value.
   */
  fun setPrice(price: Double) {
    _uiState.value = _uiState.value.copy(price = price)
  }

  /**
   * Sets the material list in the UI state.
   *
   * @param material The list of materials.
   */
  fun setMaterial(material: String) {
    _uiState.value = _uiState.value.copy(materialText = material)

    // Use to parse the text for the material
    val materials =
        material.split(",").mapNotNull { entry ->
          val parts = entry.trim().split(" ")
          if (parts.size == 2 && parts[1].endsWith("%")) {
            val name = parts[0]
            val percentage = parts[1].removeSuffix("%").toDoubleOrNull()
            if (percentage != null) Material(name, percentage) else null
          } else null
        }
    _uiState.value = _uiState.value.copy(material = materials)
  }

  /**
   * Sets the link in the UI state.
   *
   * @param link The URL link.
   */
  fun setLink(link: String) {
    _uiState.value = _uiState.value.copy(link = link)
  }

  /**
   * Updates the type suggestions based on the current category and input.
   *
   * @param input The input string to filter suggestions.
   */
  fun updateTypeSuggestions(input: String) {
    val state = _uiState.value

    val normalizeCategory =
        when (state.category.trim().lowercase()) {
          "clothes",
          "clothing" -> "Clothing"
          "shoe",
          "shoes" -> "Shoes"
          "bag",
          "bags" -> "Bags"
          "accessory",
          "accessories" -> "Accessories"
          else -> state.category
        }
    val allSuggestions = typeSuggestions[normalizeCategory] ?: emptyList()

    val filtered =
        if (input.isBlank()) {
          allSuggestions
        } else {
          allSuggestions.filter { it.startsWith(input, ignoreCase = true) }
        }

    _uiState.value = state.copy(suggestions = filtered)
  }
}
