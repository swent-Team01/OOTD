package com.android.ootd.ui.post

import android.content.Context
import android.net.Uri
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

// import com.google.firebase.storage.FirebaseStorage

/**
 * UI state for the AddItems screen. This state holds the data needed to create a new Clothing item.
 */
data class AddItemsUIState(
    val image: Uri = Uri.EMPTY,
    val category: String = "",
    val type: String = "",
    val brand: String = "",
    val price: String = "",
    val material: List<Material> = emptyList(), // parsed user input for material field
    val link: String = "",
    val errorMessage: String? = null,
    val invalidPhotoMsg: String? = null,
    val invalidCategory: String? = null,
    val typeSuggestion: List<String> = emptyList(),
    val categorySuggestion: List<String> = emptyList(),
    val materialText: String = "", // raw user input for material field
) {
  val isAddingValid: Boolean
    get() =
        invalidPhotoMsg == null &&
            invalidCategory == null &&
            image != Uri.EMPTY &&
            category.isNotEmpty() &&
            isCategoryValid()

  private fun isCategoryValid(): Boolean {
    val normalized =
        when (category.trim().lowercase()) {
          "clothes",
          "clothing" -> "Clothing"
          "shoes",
          "shoe" -> "Shoes"
          "bags",
          "bag" -> "Bags"
          "accessories",
          "accessory" -> "Accessories"
          else -> category.trim()
        }
    val categories = listOf("Clothing", "Accessories", "Shoes", "Bags")
    return categories.any { it.equals(normalized, ignoreCase = true) }
  }
}

/**
 * ViewModel for the AddItems screen. This ViewModel manages the state of input fields for the
 * AddItems screen.
 */
open class AddItemsViewModel(
    private val repository: ItemsRepository = ItemsRepositoryProvider.repository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(AddItemsUIState())
  open val uiState: StateFlow<AddItemsUIState> = _uiState.asStateFlow()

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

  fun setErrorMsg(msg: String) {
    _uiState.value = _uiState.value.copy(errorMessage = msg)
  }

  fun canAddItems(): Boolean {
    val state = _uiState.value
    if (!state.isAddingValid) {
      val error =
          when {
            state.image == Uri.EMPTY -> "Please upload a photo before adding the item."
            state.category.isBlank() -> "Please enter a category before adding the item."
            state.invalidCategory != null -> "Please select a valid category."
            else -> "Some required fields are missing."
          }

      setErrorMsg(error)
      return false
    }
    val ownerId = Firebase.auth.currentUser?.uid ?: ""
    addItemsToRepository(
        Item(
            uuid = repository.getNewItemId(),
            image = state.image,
            category = state.category,
            type = state.type,
            brand = state.brand,
            price = state.price.toDoubleOrNull() ?: 0.0,
            material = state.material,
            link = state.link,
            ownerId = ownerId))
    clearErrorMsg()
    return true
  }

  fun addItemsToRepository(item: Item) {
    val state = _uiState.value

    if (!state.isAddingValid) {
      setErrorMsg("At least one field is not valid")
      return
    }

    viewModelScope.launch {
      try {
        repository.addItem(item)
      } catch (e: Exception) {
        setErrorMsg("Failed to add item: ${e.message}")
      }
    }
  }

  fun setPhoto(uri: Uri) {
    _uiState.value =
        _uiState.value.copy(
            image = uri, invalidPhotoMsg = if (uri == Uri.EMPTY) "Please select a photo." else null)
  }

  fun setCategory(category: String) {
    val categories = typeSuggestions.keys.toList()
    val trimmedCategory = category.trim()

    // Normalize category for validation
    val normalized =
        when (trimmedCategory.lowercase()) {
          "clothes",
          "clothing" -> "Clothing"
          "shoes",
          "shoe" -> "Shoes"
          "bags",
          "bag" -> "Bags"
          "accessories",
          "accessory" -> "Accessories"
          else -> trimmedCategory
        }

    val isExactMatch = categories.any { it.equals(normalized, ignoreCase = true) }
    val isPotentialMatch = categories.any { it.lowercase().startsWith(trimmedCategory.lowercase()) }

    _uiState.value =
        _uiState.value.copy(
            category = category,
            invalidCategory =
                when {
                  category.isBlank() -> null
                  isExactMatch -> null
                  isPotentialMatch -> null
                  else -> "Please enter one of: Clothing, Accessories, Shoes, or Bags."
                })
  }

  fun setType(type: String) {
    _uiState.value = _uiState.value.copy(type = type)
  }

  fun setBrand(brand: String) {
    _uiState.value = _uiState.value.copy(brand = brand)
  }

  fun setPrice(price: String) {
    _uiState.value = _uiState.value.copy(price = price)
  }

  fun setMaterial(material: String) {
    _uiState.value = _uiState.value.copy(materialText = material)

    // Parse text like "Coton 80%, Laine 20%"
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

    _uiState.value = state.copy(typeSuggestion = filtered)
  }

  fun updateCategorySuggestions(input: String) {
    val categories = typeSuggestions.keys.toList()
    val filtered =
        if (input.isBlank()) {
          categories
        } else {
          categories.filter { it.startsWith(input, ignoreCase = true) }
        }

    _uiState.value =
        _uiState.value.copy(
            categorySuggestion = filtered,
        )
  }

  fun validateCategory() {
    val state = _uiState.value
    val categories = typeSuggestions.keys.toList()
    val trimmedCategory = state.category.trim()

    // Normalize category for validation
    val normalized =
        when (trimmedCategory.lowercase()) {
          "clothes",
          "clothing" -> "Clothing"
          "shoes",
          "shoe" -> "Shoes"
          "bags",
          "bag" -> "Bags"
          "accessories",
          "accessory" -> "Accessories"
          else -> trimmedCategory
        }

    val error =
        when {
          trimmedCategory.isEmpty() -> null
          !categories.any { it.equals(normalized, ignoreCase = true) } ->
              "Please enter one of: Clothing, Accessories, Shoes, or Bags."
          else -> null
        }

    _uiState.value = state.copy(invalidCategory = error)
  }
}
