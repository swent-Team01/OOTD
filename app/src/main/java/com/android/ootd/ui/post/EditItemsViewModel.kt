package com.android.ootd.ui.post

import android.content.Context
import android.net.Uri
import android.webkit.URLUtil.isValidUrl
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.Item
import com.android.ootd.model.ItemsRepository
import com.android.ootd.model.ItemsRepositoryProvider
import com.android.ootd.model.Material
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
    val image: Uri = Uri.EMPTY,
    val category: String = "",
    val type: String = "",
    val brand: String = "",
    val price: Double = 0.0,
    val material: List<Material> = emptyList(),
    val link: String = "",
    val errorMessage: String? = null,
    val invalidPhotoMsg: String? = null,
    val invalidCategory: String? = null,
    val suggestions: List<String> = emptyList(),
) {
  val isEditValid: Boolean
    get() =
        invalidPhotoMsg == null &&
            invalidCategory == null &&
            image != Uri.EMPTY &&
            category.isNotEmpty()
}

/**
 * ViewModel for the EditItems screen. This ViewModel manages the state of input fields for the
 * EditItems screen.
 */
open class EditItemsViewModel(
    private val repository: ItemsRepository = ItemsRepositoryProvider.repository
) : ViewModel() {

  private val _uiState = MutableStateFlow(EditItemsUIState())
  open val uiState: StateFlow<EditItemsUIState> = _uiState.asStateFlow()

  private var typeSuggestions: Map<String, List<String>> = emptyMap()

  /**
   * Initializes the type suggestions from YAML file. Should be called from the composable with the
   * context.
   */
  fun initTypeSuggestions(context: Context) {
    typeSuggestions = TypeSuggestionsLoader.loadTypeSuggestions(context)
  }

  /** Clears the error message in the UI state. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMessage = null)
  }

  fun setErrorMsg(msg: String) {
    _uiState.value = _uiState.value.copy(errorMessage = msg)
  }

  /** Loads an existing item into the UI state for editing. */
  fun loadItem(item: Item) {
    _uiState.value =
        EditItemsUIState(
            itemId = item.uuid,
            image = item.image,
            category = item.category,
            type = item.type,
            brand = item.brand,
            price = item.price,
            material = item.material,
            link = item.link)
  }

  fun canEditItems(): Boolean {
    val state = _uiState.value
    if (!state.isEditValid) {
      setErrorMsg("Please fill in all required fields.")
      return false
    }

    if (state.link.isNotEmpty() && !isValidUrl(state.link)) {
      setErrorMsg("Please enter a valid URL.")
      return false
    }

    editItemsInRepository(
        Item(
            uuid = state.itemId,
            image = state.image,
            category = state.category,
            type = state.type,
            brand = state.brand,
            price = state.price,
            material = state.material,
            link = state.link))
    clearErrorMsg()
    return true
  }

  fun editItemsInRepository(item: Item) {
    viewModelScope.launch {
      try {
        repository.editItem(item.uuid, item)
        setErrorMsg("Item updated successfully!")
      } catch (e: Exception) {
        setErrorMsg("Failed to update item: ${e.message}")
      }
    }
  }

  /** Deletes the current item from the repository. */
  fun deleteItem() {
    val state = _uiState.value
    if (state.itemId.isEmpty()) {
      setErrorMsg("No item to delete.")
      return
    }

    viewModelScope.launch {
      try {
        repository.deleteItem(state.itemId)
        setErrorMsg("Item deleted successfully!")
        // Clear the form after successful deletion
        _uiState.value = EditItemsUIState()
      } catch (e: Exception) {
        setErrorMsg("Failed to delete item: ${e.message}")
      }
    }
  }

  fun setPhoto(uri: Uri) {
    _uiState.value =
        _uiState.value.copy(
            image = uri, invalidPhotoMsg = if (uri == Uri.EMPTY) "Please select a photo." else null)
  }

  fun setCategory(category: String) {
    _uiState.value =
        _uiState.value.copy(
            category = category,
            invalidCategory = if (category.isEmpty()) "Please select a category." else null)
  }

  fun setType(type: String) {
    _uiState.value = _uiState.value.copy(type = type)
  }

  fun setBrand(brand: String) {
    _uiState.value = _uiState.value.copy(brand = brand)
  }

  fun setPrice(price: Double) {
    _uiState.value = _uiState.value.copy(price = price)
  }

  fun setMaterial(material: List<Material>) {
    _uiState.value = _uiState.value.copy(material = material)
  }

  fun setLink(link: String) {
    _uiState.value = _uiState.value.copy(link = link)
  }

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
