package com.android.ootd.ui.post.items

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.account.AccountRepositoryProvider
import com.android.ootd.model.items.FirebaseImageUploader
import com.android.ootd.model.items.ImageData
import com.android.ootd.model.items.Item
import com.android.ootd.model.items.ItemsRepository
import com.android.ootd.model.items.ItemsRepositoryProvider
import com.android.ootd.model.items.Material
import com.android.ootd.utils.CategoryNormalizer
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the AddItems screen. This state holds the data needed to create a new Clothing item.
 */
data class AddItemsUIState(
    val image: ImageData = ImageData(imageId = "", imageUrl = ""),
    val postUuid: String = "",
    val localPhotoUri: Uri? = null,
    val category: String = "",
    val type: String = "",
    val brand: String = "",
    val price: Double = 0.0,
    val currency: String = "CHF",
    val material: List<Material> = emptyList(),
    val link: String = "",
    val errorMessage: String? = null,
    val invalidPhotoMsg: String? = null,
    val invalidCategory: String? = null,
    val typeSuggestion: List<String> = emptyList(),
    val categorySuggestion: List<String> = emptyList(),
    val materialText: String = "",
    val isLoading: Boolean = false,
    val overridePhoto: Boolean = false,
    val condition: String = "",
    val size: String = "",
    val fitType: String = "",
    val style: String = "",
    val notes: String = ""
) {
  val isAddingValid: Boolean
    get() =
        overridePhoto ||
            (invalidPhotoMsg == null &&
                invalidCategory == null &&
                (localPhotoUri != null || image.imageUrl.isNotEmpty()) &&
                category.isNotEmpty() &&
                isCategoryValid())

  private fun isCategoryValid(): Boolean {
    return CategoryNormalizer.isValid(category)
  }
}

/** Factory used to pass parameter to the view model for testing. */
class AddItemsViewModelFactory(private val overridePhoto: Boolean) : ViewModelProvider.Factory {

  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(AddItemsViewModel::class.java)) {
      return AddItemsViewModel(overridePhoto = overridePhoto) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
}

/**
 * ViewModel for the AddItems screen. This ViewModel manages the state of input fields for the
 * AddItems screen.
 */
open class AddItemsViewModel(
    private val repository: ItemsRepository = ItemsRepositoryProvider.repository,
    private val accountRepository: AccountRepository = AccountRepositoryProvider.repository,
    private val overridePhoto: Boolean = false
) : BaseItemViewModel<AddItemsUIState>() {

  // Provide initial state to the BaseItemViewModel (which owns _uiState + uiState)
  override fun initialState() = AddItemsUIState(overridePhoto = overridePhoto)

  private val _addOnSuccess = MutableStateFlow(false)
  val addOnSuccess: StateFlow<Boolean> = _addOnSuccess

  fun resetAddSuccess() {
    _addOnSuccess.value = false
  }

  override fun updateType(state: AddItemsUIState, type: String) = state.copy(type = type)

  override fun updateBrand(state: AddItemsUIState, brand: String) = state.copy(brand = brand)

  override fun updateLink(state: AddItemsUIState, link: String) = state.copy(link = link)

  override fun updateMaterial(
      state: AddItemsUIState,
      materialText: String,
      materials: List<Material>
  ) = state.copy(materialText = materialText, material = materials)

  override fun getCategory(state: AddItemsUIState) = state.category

  override fun setErrorMessage(state: AddItemsUIState, message: String?) =
      state.copy(errorMessage = message)

  override fun updateTypeSuggestionsState(state: AddItemsUIState, suggestions: List<String>) =
      state.copy(typeSuggestion = suggestions)

  override fun updateCategorySuggestionsState(state: AddItemsUIState, suggestions: List<String>) =
      state.copy(categorySuggestion = suggestions)

  override fun setPhotoState(
      state: AddItemsUIState,
      uri: Uri?,
      image: ImageData,
      invalidPhotoMsg: String?
  ) =
      state.copy(
          localPhotoUri = uri,
          image = image,
          invalidPhotoMsg = invalidPhotoMsg,
          errorMessage = null)

  /** Initializes the ViewModel with the post UUID. */
  fun initPostUuid(postUuid: String) {
    _uiState.value = _uiState.value.copy(postUuid = postUuid)
  }

  /** Validates the current state and returns an error message if invalid, or null if valid */
  private fun validateAddItemState(state: AddItemsUIState): String? {
    return when {
      state.localPhotoUri == null && state.image.imageUrl.isEmpty() ->
          "Please upload a photo before adding the item."
      state.category.isBlank() -> "Please enter a category before adding the item."
      state.invalidCategory != null -> "Please select a valid category."
      else -> null
    }
  }

  /** Uploads image and returns the uploaded ImageData, or null if upload fails */
  private suspend fun uploadItemImage(localUri: Uri?, itemUuid: String): ImageData? {
    if (localUri == null) return ImageData("", "")

    val uploadedImage = FirebaseImageUploader.uploadImage(localUri, itemUuid)
    return if (uploadedImage.imageUrl.isEmpty()) null else uploadedImage
  }

  /** Creates an Item object from the current state */
  private fun createItemFromState(
      state: AddItemsUIState,
      itemUuid: String,
      uploadedImage: ImageData,
      ownerId: String
  ): Item {
    return Item(
        itemUuid = itemUuid,
        postUuids = if (state.postUuid.isNotEmpty()) listOf(state.postUuid) else emptyList(),
        image = uploadedImage,
        category = state.category,
        type = state.type,
        brand = state.brand,
        price = state.price,
        currency = state.currency,
        material = state.material,
        link = state.link,
        ownerId = ownerId,
        condition = state.condition.ifBlank { null },
        size = state.size.ifBlank { null },
        fitType = state.fitType.ifBlank { null },
        style = state.style.ifBlank { null },
        notes = state.notes.ifBlank { null },
    )
  }

  /** Adds item to repository and inventory, handling rollback on failure */
  private suspend fun addItemAndUpdateInventory(item: Item, uploadedImage: ImageData): Boolean {
    repository.addItem(item)

    val addedToInventory = accountRepository.addItem(item.itemUuid)
    if (!addedToInventory) {
      // Rollback: delete item and uploaded image
      repository.deleteItem(item.itemUuid)
      FirebaseImageUploader.deleteImage(uploadedImage.imageId)
      return false
    }
    return true
  }

  fun onAddItemClick() {
    val state = _uiState.value

    if (overridePhoto) {
      _addOnSuccess.value = true
      return
    }

    // Validate state
    if (!state.isAddingValid) {
      val error = validateAddItemState(state) ?: "Some required fields are missing."
      setErrorMsg(error)
      _addOnSuccess.value = false
      return
    }

    val ownerId = Firebase.auth.currentUser?.uid ?: ""
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true)
      try {
        val itemUuid = repository.getNewItemId()

        // Upload image
        val uploadedImage = uploadItemImage(state.localPhotoUri, itemUuid)
        if (uploadedImage == null) {
          setErrorMsg("Image upload failed. Please try again.")
          _addOnSuccess.value = false
          return@launch
        }

        // Create item
        val item = createItemFromState(state, itemUuid, uploadedImage, ownerId)

        // Add item and update inventory
        val success = addItemAndUpdateInventory(item, uploadedImage)
        if (!success) {
          setErrorMsg("Failed to add item to inventory. Please try again.")
          _addOnSuccess.value = false
          return@launch
        }

        _addOnSuccess.value = true
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

  fun setPrice(price: Double) {
    _uiState.value = _uiState.value.copy(price = price)
  }

  fun setCurrency(currency: String) {
    _uiState.value = _uiState.value.copy(currency = currency)
  }

  fun setCondition(value: String) {
    _uiState.value = _uiState.value.copy(condition = value)
  }

  fun setSize(value: String) {
    _uiState.value = _uiState.value.copy(size = value)
  }

  fun setFitType(value: String) {
    _uiState.value = _uiState.value.copy(fitType = value)
  }

  fun setStyle(value: String) {
    _uiState.value = _uiState.value.copy(style = value)
  }

  fun setNotes(value: String) {
    _uiState.value = _uiState.value.copy(notes = value)
  }

  fun validateCategory() {
    val state = _uiState.value
    val categories = typeSuggestions.keys.toList()
    val trimmedCategory = state.category.trim()
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
