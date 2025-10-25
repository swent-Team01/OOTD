package com.android.ootd.ui.post

import android.content.Context
import android.net.Uri
import android.webkit.URLUtil.isValidUrl
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.items.Item
import com.android.ootd.model.items.ItemsRepository
import com.android.ootd.model.items.ItemsRepositoryProvider
import com.android.ootd.model.items.Material
import com.android.ootd.utils.TypeSuggestionsLoader
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
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
    _uiState.value =
        EditItemsUIState(
            itemId = item.uuid,
            image = item.image,
            category = item.category,
            type = item.type ?: "",
            brand = item.brand ?: "",
            price = item.price ?: 0.0,
            material = item.material.filterNotNull(),
            link = item.link ?: "")
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

  /**
   * Checks if all requirements are met to edit the item. If valid, automatically edits the item in
   * the repository.
   *
   * @return true if the item can be edited, false otherwise.
   */
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
    val ownerId = Firebase.auth.currentUser?.uid ?: ""
    editItemsInRepository(
        Item(
            uuid = state.itemId,
            image = state.image,
            category = state.category,
            type = state.type,
            brand = state.brand,
            price = state.price,
            material = state.material,
            link = state.link,
            ownerId = ownerId))
    return true
  }

  /**
   * Edits the item in the repository.
   *
   * @param item The item to update.
   */
  fun editItemsInRepository(item: Item) {
    viewModelScope.launch {
      try {
        repository.editItem(item.uuid, item)
      } catch (e: Exception) {
        setErrorMsg("Failed to update item: ${e.message}")
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
        _uiState.value = EditItemsUIState(errorMessage = "Item deleted successfully!")
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
    _uiState.value =
        _uiState.value.copy(
            image = uri, invalidPhotoMsg = if (uri == Uri.EMPTY) "Please select a photo." else null)
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
  fun setMaterial(material: List<Material>) {
    _uiState.value = _uiState.value.copy(material = material)
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
