package com.android.ootd.ui.post

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.authentication.AccountService
import com.android.ootd.model.authentication.AccountServiceFirebase
import com.android.ootd.model.map.Location
import com.android.ootd.model.post.OutfitPostRepository
import com.android.ootd.model.post.OutfitPostRepositoryProvider
import com.android.ootd.model.posts.LikesRepository
import com.android.ootd.model.posts.LikesRepositoryProvider
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.model.user.User
import com.android.ootd.model.user.UserRepository
import com.android.ootd.model.user.UserRepositoryProvider
import com.android.ootd.utils.LocationUtils.mapFromLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** UI state for the PostView screen */
data class PostViewUiState(
    val post: OutfitPost? = null,
    val ownerUsername: String? = null, // used to display owner's username
    val ownerProfilePicture: String? = null, // used to display owner's profile picture
    val likedByUsers: List<User> = emptyList(), // to display list of users who liked the post
    val isLikedByCurrentUser: Boolean = false, // to indicate if current user liked the post
    val isLoading: Boolean = false,
    val error: String? = null,
    val isOwner: Boolean = false
)

/** ViewModel for viewing a single post's details */
class PostViewViewModel(
    private val postId: String,
    private val postRepository: OutfitPostRepository = OutfitPostRepositoryProvider.repository,
    private val userRepository: UserRepository = UserRepositoryProvider.repository,
    private val likesRepository: LikesRepository = LikesRepositoryProvider.repository,
    private val accountService: AccountService = AccountServiceFirebase()
) : ViewModel() {

  private val _uiState = MutableStateFlow(PostViewUiState())
  val uiState: StateFlow<PostViewUiState> = _uiState.asStateFlow()
  private val currentUserId
    get() = accountService.currentUserId

  init {
    if (postId.isNotEmpty()) {
      loadPost(postId)
    }
  }

  /**
   * Load all post related data from multiple repositories
   *
   * @param postId ID of the post to load
   */
  fun loadPost(postId: String) {
    _uiState.value = _uiState.value.copy(isLoading = true, error = null)

    viewModelScope.launch {
      try {
        // We load post data in multiple steps:

        // Load post object
        val post =
            postRepository.getPostById(postId)
                ?: return@launch setError("Post View: Post not found")

        // Load owner user - we need to fetch its username and pfp
        val owner = userRepository.getUser(post.ownerId)

        val isOwner = post.ownerId == currentUserId

        // Get all likes for the post - Like objects contain liker IDs
        val likeObjects = likesRepository.getLikesForPost(postId)
        // Extract liker IDs from Like objects
        val likerUserIds = likeObjects.map { it.postLikerId }

        // Convert liker IDs to User objects - we will need usernames and pfps
        val likerUsers =
            likerUserIds.mapNotNull { likerId ->
              runCatching { userRepository.getUser(likerId) }.getOrNull()
            }

        // Determine if current user liked this post
        val isLikedByMe = currentUserId.let { uid -> likerUserIds.contains(uid) }

        _uiState.value =
            PostViewUiState(
                post = post,
                ownerUsername = owner.username,
                ownerProfilePicture = owner.profilePicture,
                likedByUsers = likerUsers,
                isLikedByCurrentUser = isLikedByMe,
                isLoading = false,
                error = null,
                isOwner = isOwner)
      } catch (e: Exception) {
        setError(e.message ?: "Failed to load post")
      }
    }
  }

  /** Toggles the like status of the post for the current user */
  fun toggleLike() {
    val userId = currentUserId
    val post = _uiState.value.post ?: return

    viewModelScope.launch {
      try {
        val postId = post.postUID
        val currentlyLiked = _uiState.value.isLikedByCurrentUser

        if (currentlyLiked) {
          likesRepository.unlikePost(postId, userId)
        } else {
          likesRepository.likePost(
              com.android.ootd.model.posts.Like(
                  postId = postId, postLikerId = userId, timestamp = System.currentTimeMillis()))
        }
        // Reload to update like count + liker list + like state
        loadPost(postId)
      } catch (e: Exception) {
        setError(e.message ?: "Failed to toggle like status")
      }
    }
  }

  /**
   * Saves updated description for the post
   *
   * @param newDescription The new description text to save to the post
   */
  fun saveDescription(newDescription: String) {
    val post = _uiState.value.post ?: return
    viewModelScope.launch {
      try {
        // Update only the description field of the post
        postRepository.updatePostFields(
            post.postUID, updates = mapOf("description" to newDescription))
        // Reload to reflect changes
        loadPost(post.postUID)
      } catch (e: Exception) {
        setError(e.message ?: "Failed to update description")
      }
    }
  }

  private fun setError(msg: String) {
    _uiState.value = _uiState.value.copy(error = msg, isLoading = false)
  }

  fun savePostEdits(newDescription: String, newLocation: Location) {
    val post = _uiState.value.post ?: return
    viewModelScope.launch {
      try {
        postRepository.updatePostFields(
            post.postUID,
            updates =
                mapOf(
                    "description" to newDescription,
                    "location" to mapFromLocation(newLocation),
                ))
        loadPost(post.postUID)
      } catch (e: Exception) {
        setError(e.message ?: "Failed to update post")
      }
    }
  }

  fun deletePost(onSuccess: () -> Unit, onError: (String) -> Unit) {
    val post = _uiState.value.post ?: return
    viewModelScope.launch {
      try {
        _uiState.value = _uiState.value.copy(isLoading = true)
        postRepository.deletePost(post.postUID)
        onSuccess()
      } catch (e: Exception) {
        val msg = e.message ?: "Failed to delete post"
        setError(msg)
        onError(msg)
      } finally {
        _uiState.value = _uiState.value.copy(isLoading = false)
      }
    }
  }
}

/** Factory for creating PostViewViewModel instances */
class PostViewViewModelFactory(
    private val postId: String,
    private val postRepository: OutfitPostRepository = OutfitPostRepositoryProvider.repository,
    private val userRepository: UserRepository = UserRepositoryProvider.repository,
    private val likesRepository: LikesRepository = LikesRepositoryProvider.repository
) : ViewModelProvider.Factory {

  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(PostViewViewModel::class.java)) {
      return PostViewViewModel(postId, postRepository, userRepository, likesRepository) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
}
