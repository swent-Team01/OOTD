package com.android.ootd.ui.post

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.items.FirebaseImageUploader
import com.android.ootd.model.items.ImageData
import com.android.ootd.model.items.Item
import com.android.ootd.model.items.ItemsRepository
import com.android.ootd.model.items.ItemsRepositoryProvider
import com.android.ootd.model.items.Material
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
    val image: ImageData = ImageData(imageId = "", imageUrl = ""),
    val postUuid: String = "",
    val localPhotoUri: Uri? = null, // temporary local URI for photo before upload
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
    val isLoading: Boolean = false,
) {
  val isAddingValid: Boolean
    get() =
        invalidPhotoMsg == null &&
            invalidCategory == null &&
            (localPhotoUri != null || image.imageUrl.isNotEmpty()) &&
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
) : BaseItemViewModel<AddItemsUIState>() {

  override val _uiState = MutableStateFlow(AddItemsUIState())
  override val uiState: StateFlow<AddItemsUIState> = _uiState.asStateFlow()
  private val _addOnSuccess = MutableStateFlow(false)
  val addOnSuccess: StateFlow<Boolean> = _addOnSuccess

  fun resetAddSuccess() {
    _addOnSuccess.value = false
  }

  override fun updateType(state: AddItemsUIState, type: String): AddItemsUIState =
      state.copy(type = type)

  override fun updateBrand(state: AddItemsUIState, brand: String): AddItemsUIState =
      state.copy(brand = brand)

  override fun updateLink(state: AddItemsUIState, link: String): AddItemsUIState =
      state.copy(link = link)

  override fun updateMaterial(
      state: AddItemsUIState,
      materialText: String,
      materials: List<Material>
  ): AddItemsUIState = state.copy(materialText = materialText, material = materials)

  override fun getCategory(state: AddItemsUIState): String = state.category

  override fun setErrorMessage(state: AddItemsUIState, message: String?): AddItemsUIState =
      state.copy(errorMessage = message)

  override fun updateTypeSuggestionsState(
      state: AddItemsUIState,
      suggestions: List<String>
  ): AddItemsUIState = state.copy(typeSuggestion = suggestions)

  override fun updateCategorySuggestionsState(
      state: AddItemsUIState,
      suggestions: List<String>
  ): AddItemsUIState = state.copy(categorySuggestion = suggestions)

  override fun setPhotoState(
      state: AddItemsUIState,
      uri: Uri?,
      image: ImageData,
      invalidPhotoMsg: String?
  ): AddItemsUIState =
      state.copy(
          localPhotoUri = uri,
          image = image,
          invalidPhotoMsg = invalidPhotoMsg,
          errorMessage = null)

  /** Initializes the ViewModel with the post UUID. */
  fun initPostUuid(postUuid: String) {
    _uiState.value = _uiState.value.copy(postUuid = postUuid)
  }

  fun onAddItemClick() {
    val state = _uiState.value
    if (!state.isAddingValid) {
      val error =
          when {
            state.localPhotoUri == null && state.image.imageUrl.isEmpty() ->
                "Please upload a photo before adding the item."
            state.category.isBlank() -> "Please enter a category before adding the item."
            state.invalidCategory != null -> "Please select a valid category."
            else -> "Some required fields are missing."
          }

      setErrorMsg(error)
      _addOnSuccess.value = false
      return
    }
    val ownerId = Firebase.auth.currentUser?.uid ?: ""
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true)
      try {
        val itemUuid = repository.getNewItemId()
        val localUri = state.localPhotoUri
        val uploadedImage =
            if (localUri != null) {
              FirebaseImageUploader.uploadImage(localUri, itemUuid)
            } else {
              ImageData("", "")
            }
        if (uploadedImage.imageUrl.isEmpty()) {
          setErrorMsg("Image upload failed. Please try again.")
          _addOnSuccess.value = false
          return@launch
        }

        val item =
            Item(
                itemUuid = itemUuid,
                postUuids =
                    listOf(
                        state.postUuid), // Add to a list since an item can belong to multiple posts
                image = uploadedImage,
                category = state.category,
                type = state.type,
                brand = state.brand,
                price = state.price.toDoubleOrNull() ?: 0.0,
                material = state.material,
                link = state.link,
                ownerId = ownerId)

        _addOnSuccess.value = true
        repository.addItem(item)
        clearErrorMsg()
      } catch (e: Exception) {
        setErrorMsg("Failed to add item: ${e.message}")
        _addOnSuccess.value = false
      } finally {
        _uiState.value = _uiState.value.copy(isLoading = false)
      }
    }
  }

  override fun setPhoto(uri: Uri) {
    if (uri == Uri.EMPTY) {
      updateState {
        setErrorMessage(setPhotoState(it, null, ImageData("", ""), "Please select a photo."), null)
      }
    } else {
      updateState { setErrorMessage(setPhotoState(it, uri, ImageData("", ""), null), null) }
    }
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

  fun setPrice(price: String) {
    _uiState.value = _uiState.value.copy(price = price)
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
