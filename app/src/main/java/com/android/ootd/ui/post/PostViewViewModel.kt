package com.android.ootd.ui.post

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.post.OutfitPostRepository
import com.android.ootd.model.post.OutfitPostRepositoryFirestore
import com.android.ootd.model.posts.OutfitPost
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** UI state for the PostView screen */
data class PostViewUiState(
    val post: OutfitPost? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

/** ViewModel for viewing a single post */
class PostViewViewModel(
    private val postId: String,
    private val repository: OutfitPostRepository =
        OutfitPostRepositoryFirestore(
            FirebaseFirestore.getInstance(), FirebaseStorage.getInstance())
) : ViewModel() {

  private val _uiState = MutableStateFlow(PostViewUiState())
  val uiState: StateFlow<PostViewUiState> = _uiState.asStateFlow()

  init {
    if (postId.isNotEmpty()) {
      loadPost(postId)
    }
  }

  /** Load the post from the repository */
  fun loadPost(postId: String) {
    _uiState.value = _uiState.value.copy(isLoading = true, error = null)

    viewModelScope.launch {
      try {
        val post = repository.getPostById(postId)
        if (post != null) {
          _uiState.value = PostViewUiState(post = post, isLoading = false, error = null)
        } else {
          _uiState.value = PostViewUiState(post = null, isLoading = false, error = "Post not found")
        }
      } catch (exception: Exception) {
        _uiState.value =
            PostViewUiState(
                post = null, isLoading = false, error = exception.message ?: "Failed to load post")
      }
    }
  }
}

/** Factory for creating PostViewViewModel instances */
class PostViewViewModelFactory(
    private val postId: String,
    private val repository: OutfitPostRepository =
        OutfitPostRepositoryFirestore(
            FirebaseFirestore.getInstance(), FirebaseStorage.getInstance())
) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(PostViewViewModel::class.java)) {
      return PostViewViewModel(postId, repository) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
}
