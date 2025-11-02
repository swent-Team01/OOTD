package com.android.ootd.ui.post

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.items.Item
import com.android.ootd.model.items.ItemsRepository
import com.android.ootd.model.items.ItemsRepositoryProvider
import com.android.ootd.model.post.OutfitPostRepository
import com.android.ootd.model.post.OutfitPostRepositoryProvider
import com.android.ootd.model.posts.OutfitPost
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
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
    val isPublished: Boolean = false
)

/**
 * ViewModel for the PreviewScreen.
 *
 * Responsible for managing the state by fetching and providing data for items from the Items
 * [ItemsRepository]
 *
 * @property itemsRepository The repository used to fetch and manage items.
 */
class OutfitPreviewViewModel(
    private val itemsRepository: ItemsRepository = ItemsRepositoryProvider.repository,
    private val postRepository: OutfitPostRepository = OutfitPostRepositoryProvider.repository
) : ViewModel() {

  private val _uiState = MutableStateFlow(PreviewUIState())
  val uiState: StateFlow<PreviewUIState> = _uiState.asStateFlow()

  /**
   * Initialises state from FitCheck screen (receives imageUri and description) Generates a new
   * postUuid if not already set
   */
  fun initFromFitCheck(imageUri: String, description: String) {
    // the state will generate a new postUuid if not already set
    val newUuid = _uiState.value.postUuid.ifEmpty { postRepository.getNewPostId() }

    _uiState.value =
        _uiState.value.copy(postUuid = newUuid, imageUri = imageUri, description = description)

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
   * Publishes the current outfit post to Firebase.
   *
   * On success, updates the state with `isPublished = true` and a success message. On failure, logs
   * the error and updates the state with an error message.
   */
  fun publishPost() {
    val state = _uiState.value
    if (state.imageUri.isEmpty() || state.postUuid.isEmpty()) {
      setErrorMessage("Missing required post data")
      return
    }

    viewModelScope.launch {
      _uiState.value = state.copy(isLoading = true)
      try {
        val user = Firebase.auth.currentUser ?: throw Exception("User not logged in")

        // Upload main outfit image
        val outfitPhotoUrl =
            postRepository.uploadOutfitPhoto(localPath = state.imageUri, postId = state.postUuid)

        // Fetch all items for this post
        val items = itemsRepository.getAssociatedItems(state.postUuid)
        val itemIds = items.map { it.itemUuid }

        // Build and save Firestore post
        val post =
            OutfitPost(
                postUID = state.postUuid,
                ownerId = user.uid,
                name = user.displayName ?: "",
                userProfilePicURL = user.photoUrl?.toString() ?: "",
                outfitURL = outfitPhotoUrl,
                description = state.description,
                itemsID = itemIds,
                timestamp = System.currentTimeMillis())

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

  /** Clears the error message in the UI state */
  fun clearErrorMessage() {
    _uiState.value = _uiState.value.copy(errorMessage = null)
  }

  /** Sets an error message in the UI state */
  private fun setErrorMessage(message: String) {
    _uiState.value = _uiState.value.copy(errorMessage = message)
  }
}
