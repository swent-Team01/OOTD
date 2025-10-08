package com.android.ootd.ui.post

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.Item
import com.android.ootd.model.ItemsRepository
import com.android.ootd.model.ItemsRepositoryProvider
import com.android.ootd.model.Material
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
            category.isNotEmpty()
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

  /** Clears the error message in the UI state. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMessage = null)
  }

  fun setErrorMsg(msg: String) {
    _uiState.value = _uiState.value.copy(errorMessage = msg)
  }

  fun canAddItems(): Boolean {
    // Log.e("AddItemsViewModel", "Add button clicked") //
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

    addItemsToRepository(
        Item(
            uuid = repository.getNewItemId(),
            image = state.image,
            category = state.category,
            type = state.type,
            brand = state.brand,
            price = state.price.toDoubleOrNull() ?: 0.0,
            material = state.material,
            link = state.link))
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
        // Log.e("AddItemsViewModel", "Adding item: ${item.uuid} (${item.category})")
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
    val isValid = categories.any { it.equals(category.trim(), ignoreCase = true) }

    _uiState.value =
        _uiState.value.copy(
            category = category,
            invalidCategory =
                if (isValid || category.isBlank()) null else _uiState.value.invalidCategory)
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

  val typeSuggestions =
      mapOf(
          "Clothes" to
              listOf(
                  "T-shirt",
                  "Shirt",
                  "Jeans",
                  "Jacket",
                  "Dress",
                  "Skirt",
                  "Shorts",
                  "Sweater",
                  "Coat",
                  "Blouse",
                  "Suit",
                  "Hoodie",
                  "Cardigan",
                  "Pants",
                  "Leggings",
                  "Overalls",
                  "Jumpsuit"),
          "Shoes" to
              listOf(
                  "Sneakers",
                  "Boots",
                  "Sandals",
                  "Heels",
                  "Flats",
                  "Loafers",
                  "Oxfords",
                  "Slippers",
                  "Wedges",
                  "Espadrilles",
                  "Ballerinas",
                  "Moccasins",
                  "Sports Shoes"),
          "Accessories" to
              listOf(
                  "Hat",
                  "Scarf",
                  "Belt",
                  "Gloves",
                  "Sunglasses",
                  "Watch",
                  "Bracelet",
                  "Necklace",
                  "Earrings",
                  "Tie",
                  "Beanie",
                  "Cap "),
          "Bags" to
              listOf(
                  "Backpack",
                  "Handbag",
                  "Tote",
                  "Clutch",
                  "Messenger Bag",
                  "Duffel Bag",
                  "Satchel",
                  "Crossbody Bag",
                  "Shopper",
                  "Wallet"),
      )

  fun updateTypeSuggestions(input: String) {
    val state = _uiState.value

    val normalizeCategory =
        when (state.category.trim().lowercase()) {
          "clothes",
          "clothing" -> "Clothes"
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
    val category = state.category.trim()

    val error =
        when {
          category.isEmpty() -> "Please select a category."
          !categories.any { it.equals(category, ignoreCase = true) } ->
              "Please select a valid category : $categories"
          else -> null
        }

    _uiState.value = state.copy(invalidCategory = error)
  }
}
