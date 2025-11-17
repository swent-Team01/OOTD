package com.android.ootd.ui.post.items

import android.net.Uri
import android.util.Log
import android.webkit.URLUtil.isValidUrl
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.account.AccountRepositoryProvider
import com.android.ootd.model.items.FirebaseImageUploader
import com.android.ootd.model.items.ImageData
import com.android.ootd.model.items.Item
import com.android.ootd.model.items.ItemsRepository
import com.android.ootd.model.items.ItemsRepositoryProvider
import com.android.ootd.model.items.Material
import kotlinx.coroutines.launch

/**
 * UI state for the EditItems screen. This state holds the data needed to edit an existing Clothing
 * item.
 */
data class EditItemsUIState(
    val itemId: String = "",
    val postUuids: List<String> = emptyList(),
    val image: ImageData = ImageData("", ""),
    val localPhotoUri: Uri? = null,
    val category: String = "",
    val type: String = "",
    val brand: String = "",
    val price: Double = 0.0,
    val currency: String = "CHF",
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
 * @property accountRepository The repository used for account operations.
 */
open class EditItemsViewModel(
    private val repository: ItemsRepository = ItemsRepositoryProvider.repository,
    private val accountRepository: AccountRepository = AccountRepositoryProvider.repository
) : BaseItemViewModel<EditItemsUIState>() {

  // Provide initial state to the BaseItemViewModel (which owns _uiState + uiState)
  override fun initialState() = EditItemsUIState()

  override fun updateType(state: EditItemsUIState, type: String) = state.copy(type = type)

  override fun updateBrand(state: EditItemsUIState, brand: String) = state.copy(brand = brand)

  override fun updateLink(state: EditItemsUIState, link: String) = state.copy(link = link)

  override fun updateMaterial(
      state: EditItemsUIState,
      materialText: String,
      materials: List<Material>
  ) = state.copy(materialText = materialText, material = materials)

  override fun getCategory(state: EditItemsUIState) = state.category

  override fun setErrorMessage(state: EditItemsUIState, message: String?) =
      state.copy(errorMessage = message)

  override fun updateTypeSuggestionsState(state: EditItemsUIState, suggestions: List<String>) =
      state.copy(suggestions = suggestions)

  override fun updateCategorySuggestionsState(state: EditItemsUIState, suggestions: List<String>) =
      state // no-op for categories in this screen

  override fun setPhotoState(
      state: EditItemsUIState,
      uri: Uri?,
      image: ImageData,
      invalidPhotoMsg: String?
  ) = state.copy(localPhotoUri = uri, image = image, invalidPhotoMsg = invalidPhotoMsg)

  /** Loads an existing item into the UI state for editing. */
  fun loadItem(item: Item) {
    val materialText =
        item.material.filterNotNull().joinToString(", ") { "${it.name} ${it.percentage}%" }
    _uiState.value =
        EditItemsUIState(
            itemId = item.itemUuid,
            postUuids = item.postUuids,
            image = item.image,
            category = item.category,
            type = item.type ?: "",
            brand = item.brand ?: "",
            price = item.price ?: 0.0,
            currency = item.currency ?: "CHF",
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
        _uiState.value = _uiState.value.copy(isLoading = false)
        return@launch
      }

      val updatedItem =
          Item(
              itemUuid = state.itemId,
              postUuids = state.postUuids,
              image = finalImage,
              category = state.category,
              type = state.type,
              brand = state.brand,
              price = state.price,
              currency = state.currency,
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

  /** Deletes the current item from the repository. */
  fun deleteItem() {
    val state = _uiState.value
    if (state.itemId.isEmpty()) {
      setErrorMsg("No item to delete.")
      return
    }

    viewModelScope.launch {
      try {
        // Remove item from user's inventory
        val removedFromInventory = accountRepository.removeItem(state.itemId)
        if (!removedFromInventory) {
          setErrorMsg("Failed to remove item from inventory. Please try again.")
          return@launch
        }

        repository.deleteItem(state.itemId)

        val deleted = FirebaseImageUploader.deleteImage(state.image.imageId)
        if (!deleted) Log.w("EditItemsViewModel", "Image deletion failed or image not found.")
        _uiState.value = _uiState.value.copy(isDeleteSuccessful = true)
      } catch (e: Exception) {
        setErrorMsg("Failed to delete item: ${e.message}")
      }
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

  /** Sets the currency (UI only). */
  fun setCurrency(currency: String) {
    _uiState.value = _uiState.value.copy(currency = currency)
  }
}
