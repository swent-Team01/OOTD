package com.android.ootd.ui.post.items

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.URLUtil.isValidUrl
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.account.AccountRepositoryProvider
import com.android.ootd.model.image.ImageCompressor
import com.android.ootd.model.items.FirebaseImageUploader
import com.android.ootd.model.items.ImageData
import com.android.ootd.model.items.Item
import com.android.ootd.model.items.ItemsRepository
import com.android.ootd.model.items.ItemsRepositoryProvider
import com.android.ootd.model.items.Material
import com.android.ootd.worker.ImageUploadWorker
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
    val isLoading: Boolean = false,
    val condition: String = "",
    val size: String = "",
    val fitType: String = "",
    val style: String = "",
    val notes: String = "",
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
    private val accountRepository: AccountRepository = AccountRepositoryProvider.repository,
    private val imageCompressor: ImageCompressor = ImageCompressor()
) : BaseItemViewModel<EditItemsUIState>() {

  companion object {

    private const val COMPRESS_THRESHOLD = 200 * 1024L // 200 KB
    private const val TAG = "EditItemsViewModel"
  }

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
            ownerId = item.ownerId,
            condition = item.condition ?: "",
            size = item.size ?: "",
            fitType = item.fitType ?: "",
            style = item.style ?: "",
            notes = item.notes ?: "",
        )
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

  fun onSaveItemClick(context: Context) {
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
      try {
        val finalImage =
            if (state.localPhotoUri != null) {
              // Compress and upload new image
              val compressedImage =
                  imageCompressor.compressImage(state.localPhotoUri, COMPRESS_THRESHOLD, context)
              // Handle compression failure
              if (compressedImage == null) {
                setErrorMsg("Failed to compress image.")
                _uiState.value = _uiState.value.copy(isSaveSuccessful = false, isLoading = false)
                return@launch
              }
              // Upload compressed image and get ImageData
              val uploaded =
                  FirebaseImageUploader.uploadImage(
                      compressedImage, state.itemId, state.localPhotoUri)

              // Check if we need to schedule background upload
              val uri = uploaded.imageUrl.toUri()
              if (uri.scheme == "content" || uri.scheme == "file") {
                val workRequest =
                    OneTimeWorkRequestBuilder<ImageUploadWorker>()
                        .setInputData(
                            workDataOf(
                                "itemUuid" to state.itemId,
                                "imageUri" to uploaded.imageUrl,
                                "fileName" to uploaded.imageId))
                        .setConstraints(
                            Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build())
                        .build()

                WorkManager.getInstance(context).enqueue(workRequest)
              }
              uploaded
            } else state.image

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
                ownerId = state.ownerId,
                condition = state.condition,
                size = state.size,
                fitType = state.fitType,
                style = state.style,
                notes = state.notes,
            )
        if (finalImage.imageUrl.isEmpty()) {
          setErrorMsg("Please select a photo.")
          _uiState.value = _uiState.value.copy(isSaveSuccessful = false, isLoading = false)
          return@launch
        }

        // Call editItem directly in this coroutine (not nested launch)
        // The cache update in editItem() happens synchronously before Firestore .await()
        // So even if this coroutine finishes, the cache is already updated
        try {
          repository.editItem(updatedItem.itemUuid, updatedItem)
        } catch (e: Exception) {
          // Error is acceptable when offline - cache is still updated
          Log.w(TAG, "Item edit may be offline (cache updated): ${e.message}")
        }

        // Return success - cache is already updated above
        _uiState.value =
            _uiState.value.copy(
                image = finalImage, errorMessage = null, isSaveSuccessful = true, isLoading = false)
      } catch (e: Exception) {
        setErrorMsg("Failed to save item: ${e.message}")
        _uiState.value = _uiState.value.copy(isSaveSuccessful = false, isLoading = false)
      }
    }
  }

  /**
   * Deletes the current item from the repository using optimistic pattern. Assumes success
   * immediately and queues operations in background.
   */
  fun deleteItem() {
    val state = _uiState.value
    if (state.itemId.isEmpty()) {
      setErrorMsg("No item to delete.")
      return
    }

    // Optimistic delete - assume success immediately
    _uiState.value = _uiState.value.copy(isDeleteSuccessful = true)

    // Queue delete operations in background
    viewModelScope.launch {
      try {
        accountRepository.removeItem(state.itemId)
      } catch (e: Exception) {
        Log.e(TAG, "Failed to queue item removal: ${e.message}")
      }
    }

    viewModelScope.launch {
      try {
        repository.deleteItem(state.itemId)
      } catch (e: Exception) {
        Log.e(TAG, "Failed to queue item deletion: ${e.message}")
      }
    }

    viewModelScope.launch {
      try {
        FirebaseImageUploader.deleteImage(state.image.imageId)
      } catch (e: Exception) {
        Log.w(TAG, "Image deletion failed: ${e.message}")
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
  fun setPrice(price: Double) = updateSimpleField { it.copy(price = price) }

  /** Sets the currency (UI only). */
  fun setCurrency(currency: String) = updateSimpleField { it.copy(currency = currency) }

  fun setCondition(value: String) = updateSimpleField { it.copy(condition = value) }

  fun setSize(value: String) = updateSimpleField { it.copy(size = value) }

  fun setFitType(value: String) = updateSimpleField { it.copy(fitType = value) }

  fun setStyle(value: String) = updateSimpleField { it.copy(style = value) }

  fun setNotes(value: String) = updateSimpleField { it.copy(notes = value) }
}
