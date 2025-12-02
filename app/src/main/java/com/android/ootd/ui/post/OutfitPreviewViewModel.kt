package com.android.ootd.ui.post

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.authentication.AccountService
import com.android.ootd.model.authentication.AccountServiceFirebase
import com.android.ootd.model.image.ImageCompressor
import com.android.ootd.model.items.Item
import com.android.ootd.model.items.ItemsRepository
import com.android.ootd.model.items.ItemsRepositoryProvider
import com.android.ootd.model.map.Location
import com.android.ootd.model.map.emptyLocation
import com.android.ootd.model.post.OutfitPostRepository
import com.android.ootd.model.post.OutfitPostRepositoryProvider
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.model.user.UserRepository
import com.android.ootd.model.user.UserRepositoryProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the PreviewScreen.
 *
 * This state holds the data needed to display a preview of the outfit post before submission.
 */
data class PreviewUIState(
    val postUuid: String = "",
    val imageUri: String = "",
    val description: String = "",
    val items: List<Item> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isLoading: Boolean = false,
    val isPublished: Boolean = false,
    val location: Location = emptyLocation,
    val isPublic: Boolean = false
)

/* Compression threshold for images before upload */
private const val COMPRESS_THRESHOLD = 200 * 1024L // 200 KB

/**
 * ViewModel for the PreviewScreen.
 *
 * Responsible for managing the state by fetching and providing data for items from the Items
 * [ItemsRepository]
 *
 * @property itemsRepository The repository used to fetch and manage items.
 * @property postRepository The repository used to manage outfit posts.
 * @property userRepository The repository used to fetch user data.
 * @property accountService The service used to manage user accounts.
 * @property imageCompressor The utility used to compress images before upload.
 */
class OutfitPreviewViewModel(
    private val itemsRepository: ItemsRepository = ItemsRepositoryProvider.repository,
    private val postRepository: OutfitPostRepository = OutfitPostRepositoryProvider.repository,
    private val userRepository: UserRepository = UserRepositoryProvider.repository,
    private val accountService: AccountService = AccountServiceFirebase(),
    private val imageCompressor: ImageCompressor = ImageCompressor()
) : ViewModel() {

  private val _uiState = MutableStateFlow(PreviewUIState())
  val uiState: StateFlow<PreviewUIState> = _uiState.asStateFlow()

  /**
   * Initialises state from FitCheck screen (receives imageUri, description, and location) Generates
   * a new postUuid if not already set
   */
  fun initFromFitCheck(imageUri: String, description: String, location: Location) {
    // the state will generate a new postUuid if not already set
    val newUuid = _uiState.value.postUuid.ifEmpty { postRepository.getNewPostId() }

    _uiState.value =
        _uiState.value.copy(
            postUuid = newUuid, imageUri = imageUri, description = description, location = location)

    loadItemsForPost()
  }

  /** Loads all the items belonging to the current post in creation by their postUuid */
  fun loadItemsForPost() {
    val postUuid = _uiState.value.postUuid
    if (postUuid.isEmpty()) {
      Log.w("OutfitPreviewViewModel", "loadItemsForPost() called before postUuid set")
      return
    }

    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true)
      try {
        val items = itemsRepository.getAssociatedItems(postUuid)
        _uiState.value = _uiState.value.copy(items = items, isLoading = false)
      } catch (e: Exception) {
        setErrorMessage("Failed to load items: ${e.message}")
        _uiState.value = _uiState.value.copy(isLoading = false)
      }
    }
  }

  /**
   * Sets whether the post should be public.
   *
   * @param isPublic True if the post should be public, false otherwise.
   */
  fun setPublic(isPublic: Boolean) {
    _uiState.value = _uiState.value.copy(isPublic = isPublic)
  }

  /**
   * Publishes the current outfit post to Firebase.
   *
   * On success, updates the state with `isPublished = true` and a success message. On failure, logs
   * the error and updates the state with an error message.
   *
   * @param overridePhoto: Whether to override the checks for the image appearing in testing when we
   *   cannot create images.
   */
  fun publishPost(overridePhoto: Boolean = false, context: Context) {
    if (overridePhoto) {
      _uiState.value =
          _uiState.value.copy(
              isLoading = false, isPublished = true, successMessage = "Post created successfully!")
    } else {
      val state = _uiState.value
      if (state.imageUri.isEmpty() || state.postUuid.isEmpty()) {
        setErrorMessage("Missing required post data")
        return
      }

      // returns a byte array of the compressed image
      viewModelScope.launch {
        _uiState.value = state.copy(isLoading = true)
        try {

          // Compress image before upload
          val compressedImage =
              imageCompressor.compressImage(
                  state.imageUri.toUri(),
                  compressionThreshold = COMPRESS_THRESHOLD,
                  context = context)

          // Check compression result
          if (compressedImage == null) {
            setErrorMessage("Failed to compress image")
            _uiState.value = state.copy(isLoading = false)
            return@launch
          }

          // Fetch current user id
          val currentUserId = accountService.currentUserId
          // Fetch user data
          val user = userRepository.getUser(currentUserId)

          // Upload main outfit image
          val outfitPhotoUrl =
              postRepository.uploadOutfitWithCompressedPhoto(
                  imageData = compressedImage, postId = state.postUuid)

          // Fetch all items for this post
          val items = itemsRepository.getAssociatedItems(state.postUuid)
          val itemIds = items.map { it.itemUuid }

          // If post is public, update items to be public
          if (state.isPublic) {
            val toMakePublic = items.filter { !it.isPublic }
            try {
              coroutineScope {
                toMakePublic
                    .map { item ->
                      async { itemsRepository.editItem(item.itemUuid, item.copy(isPublic = true)) }
                    }
                    .awaitAll()
              }
            } catch (e: Exception) {
              Log.e("OutfitPreviewViewModel", "Failed to mark items public", e)
              setErrorMessage("Failed to update items visibility")
              _uiState.value = state.copy(isLoading = false)
              return@launch
            }
          }

          // Build and save Firestore post
          val post =
              OutfitPost(
                  postUID = state.postUuid,
                  ownerId = user.uid,
                  name = user.username,
                  userProfilePicURL = user.profilePicture,
                  outfitURL = outfitPhotoUrl,
                  description = state.description,
                  itemsID = itemIds,
                  timestamp = System.currentTimeMillis(),
                  location = state.location,
                  isPublic = state.isPublic)

          postRepository.savePostToFirestore(post)

          _uiState.value =
              state.copy(
                  isLoading = false,
                  isPublished = true,
                  successMessage = "Post created successfully!")
        } catch (e: Exception) {
          Log.e("OutfitPreviewViewModel", "Error publishing post", e)
          setErrorMessage("Failed to publish post: ${e.message}")
          _uiState.value = state.copy(isLoading = false)
        }
      }
    }
  }

  /** Clears the error message in the UI state */
  fun clearErrorMessage() {
    _uiState.value = _uiState.value.copy(errorMessage = null)
  }

  /** Removes an item from the current post without deleting it. */
  fun removeItemFromPost(itemUuid: String) {
    val state = _uiState.value
    val postId = state.postUuid
    val targetItem = state.items.find { it.itemUuid == itemUuid } ?: return
    viewModelScope.launch {
      try {
        val updatedItem =
            targetItem.copy(postUuids = targetItem.postUuids.filterNot { it == postId })
        itemsRepository.editItem(itemUuid, updatedItem)
      } catch (e: Exception) {
        Log.e("OutfitPreviewViewModel", "Failed to remove item from post", e)
        setErrorMessage("Couldn't remove item from post")
      } finally {
        _uiState.value =
            _uiState.value.copy(items = state.items.filterNot { it.itemUuid == itemUuid })
      }
    }
  }

  /** Sets an error message in the UI state */
  private fun setErrorMessage(message: String) {
    _uiState.value = _uiState.value.copy(errorMessage = message)
  }
}
