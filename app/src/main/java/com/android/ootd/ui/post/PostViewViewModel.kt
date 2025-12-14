package com.android.ootd.ui.post

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.account.AccountRepositoryProvider
import com.android.ootd.model.authentication.AccountService
import com.android.ootd.model.authentication.AccountServiceFirebase
import com.android.ootd.model.items.Item
import com.android.ootd.model.items.ItemsRepository
import com.android.ootd.model.items.ItemsRepositoryProvider
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
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** UI state for the PostView screen with unified loading/error states */
data class PostViewUiState(
    val post: OutfitPost? = null,
    val ownerUsername: String? = null,
    val ownerProfilePicture: String? = null,
    val likedByUsers: List<User> = emptyList(),
    val isLikedByCurrentUser: Boolean = false,
    val items: List<Item> = emptyList(),
    val starredItemIds: Set<String> = emptySet(),
    val isOwner: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

/** ViewModel for viewing a single post's details with integrated items display */
class PostViewViewModel(
    private val postId: String,
    private val postRepository: OutfitPostRepository = OutfitPostRepositoryProvider.repository,
    private val userRepository: UserRepository = UserRepositoryProvider.repository,
    private val likesRepository: LikesRepository = LikesRepositoryProvider.repository,
    private val accountService: AccountService = AccountServiceFirebase(),
    private val itemsRepository: ItemsRepository = ItemsRepositoryProvider.repository,
    private val accountRepository: AccountRepository = AccountRepositoryProvider.repository
) : ViewModel() {

  private companion object {
    const val TAG = "PostViewViewModel"
    const val NETWORK_TIMEOUT_MILLIS = 2000L
  }

  private val _uiState = MutableStateFlow(PostViewUiState())
  val uiState: StateFlow<PostViewUiState> = _uiState.asStateFlow()

  private val currentUserId
    get() = accountService.currentUserId

  init {
    if (postId.isNotEmpty()) {
      loadPostWithItems(postId)
    }
  }

  /**
   * Load ALL post-related data (post, likes, items, stars) in parallel Uses a single unified
   * loading state
   */
  private fun loadPostWithItems(postId: String) {
    _uiState.value = _uiState.value.copy(isLoading = true, error = null)

    viewModelScope.launch {
      try {
        // Launch both operations in parallel
        val postDeferred = async { loadPostData(postId) }
        val itemsDeferred = async { loadItemsData(postId) }
        val starsDeferred = async { loadStarredItems() }

        // Wait for both to complete
        val postSuccess = postDeferred.await()
        val itemsSuccess = itemsDeferred.await()
        val stars = starsDeferred.await()

        // Update UI state with results
        when {
          !postSuccess -> {
            setError("Unable to load post. Please try again.")
          }
          !itemsSuccess -> {
            // Post loaded but items failed we show post with error message
            _uiState.value =
                _uiState.value.copy(
                    starredItemIds = stars,
                    isLoading = false,
                    error = "Post loaded, but items could not be loaded.")
          }
          else -> {
            // Everything loaded successfully
            _uiState.value =
                _uiState.value.copy(starredItemIds = stars, isLoading = false, error = null)
          }
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to load post with items", e)
        setError(e.message ?: "Failed to load post")
      }
    }
  }

  /** Load just the post data (post, owner, likes) Returns true if successful */
  private suspend fun loadPostData(postId: String): Boolean {
    return try {
      // Load post object
      val post = postRepository.getPostById(postId) ?: return false

      // Load owner user
      val owner = userRepository.getUser(post.ownerId)
      val isOwner = post.ownerId == currentUserId

      // Get all likes for the post
      val likeObjects = likesRepository.getLikesForPost(postId)
      val likerUserIds = likeObjects.map { it.postLikerId }

      // Convert liker IDs to User objects
      val likerUsers =
          likerUserIds.mapNotNull { likerId ->
            runCatching { userRepository.getUser(likerId) }.getOrNull()
          }

      // Determine if current user liked this post
      val isLikedByMe = currentUserId.let { uid -> likerUserIds.contains(uid) }

      // Update UI state with post data
      _uiState.value =
          _uiState.value.copy(
              post = post,
              ownerUsername = owner.username,
              ownerProfilePicture = owner.profilePicture,
              likedByUsers = likerUsers,
              isLikedByCurrentUser = isLikedByMe,
              isOwner = isOwner)
      true
    } catch (e: Exception) {
      Log.e(TAG, "Failed to load post data", e)
      false
    }
  }

  /** Load items for the post with offline/online fallback Returns true if successful */
  private suspend fun loadItemsData(postUuid: String): Boolean {
    if (postUuid.isEmpty()) {
      Log.w(TAG, "Post UUID is empty. Cannot fetch items.")
      return false
    }

    var cachedItemsSucceeded = false
    try {
      // Try offline/cached post first
      val cachedPost =
          try {
            postRepository.getPostById(postUuid)
          } catch (_: Exception) {
            null
          }

      if (cachedPost != null) {
        cachedItemsSucceeded = updateUiWithItems(cachedPost)
      }

      // Best effort online refresh (2s timeout)
      val freshPost =
          withTimeoutOrNull(NETWORK_TIMEOUT_MILLIS) { postRepository.getPostById(postUuid) }

      if (freshPost == null) {
        return cachedItemsSucceeded
      }

      return updateUiWithItems(freshPost)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to load items for post", e)
      return cachedItemsSucceeded
    }
  }

  /**
   * Load starred items for current user Returns the set of starred item IDs (empty set on failure)
   */
  private suspend fun loadStarredItems(): Set<String> {
    val userId = currentUserId
    if (userId.isBlank()) return emptySet()

    return try {
      accountRepository.getStarredItems(userId).toSet()
    } catch (e: Exception) {
      Log.w(TAG, "Failed to load starred items: ${e.message}")
      emptySet()
    }
  }

  /** Refresh starred items (called after starring/unstarring) */
  fun refreshStarredItems() {
    viewModelScope.launch {
      val starred = loadStarredItems()
      _uiState.value = _uiState.value.copy(starredItemIds = starred)
    }
  }

  /** Stars or unstars an item for the current user */
  fun toggleStar(item: Item) {
    val itemId = item.itemUuid
    if (itemId.isBlank()) return

    viewModelScope.launch {
      try {
        val updated = accountRepository.toggleStarredItem(itemId).toSet()
        Log.d(TAG, "Toggled star for $itemId -> ${updated.contains(itemId)}")
        _uiState.value = _uiState.value.copy(starredItemIds = updated)
      } catch (e: Exception) {
        Log.w(TAG, "Failed to toggle star: ${e.message}")
        setError("Couldn't update wishlist. Please try again.")
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

        // Reload only post data (not items)
        _uiState.value = _uiState.value.copy(isLoading = true)
        val success = loadPostData(postId)
        _uiState.value =
            _uiState.value.copy(
                isLoading = false, error = if (!success) "Failed to update like status" else null)
      } catch (e: Exception) {
        setError(e.message ?: "Failed to toggle like status")
      }
    }
  }

  /** Saves updated description and location for the post */
  fun savePostEdits(newDescription: String, newLocation: Location) {
    val post = _uiState.value.post ?: return

    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true)
      try {
        postRepository.updatePostFields(
            post.postUID,
            updates =
                mapOf(
                    "description" to newDescription,
                    "location" to mapFromLocation(newLocation),
                ))

        // Reload only post data
        val success = loadPostData(post.postUID)
        _uiState.value =
            _uiState.value.copy(
                isLoading = false, error = if (!success) "Failed to update post" else null)
      } catch (e: Exception) {
        setError(e.message ?: "Failed to update post")
      }
    }
  }

  /** Deletes the post */
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

  /** Clears any existing error message */
  fun clearError() {
    _uiState.value = _uiState.value.copy(error = null)
  }

  private fun setError(msg: String) {
    _uiState.value = _uiState.value.copy(error = msg, isLoading = false)
  }

  private suspend fun updateUiWithItems(post: OutfitPost): Boolean {
    val items =
        try {
          itemsRepository.getFriendItemsForPost(post.postUID, post.ownerId)
        } catch (_: Exception) {
          return false
        }

    _uiState.value = _uiState.value.copy(items = items)
    return true
  }
}

/** Factory for creating PostViewViewModel instances */
class PostViewViewModelFactory(
    private val postId: String,
    private val postRepository: OutfitPostRepository = OutfitPostRepositoryProvider.repository,
    private val userRepository: UserRepository = UserRepositoryProvider.repository,
    private val likesRepository: LikesRepository = LikesRepositoryProvider.repository,
    private val itemsRepository: ItemsRepository = ItemsRepositoryProvider.repository,
    private val accountRepository: AccountRepository = AccountRepositoryProvider.repository
) : ViewModelProvider.Factory {

  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(PostViewViewModel::class.java)) {
      return PostViewViewModel(
          postId,
          postRepository,
          userRepository,
          likesRepository,
          AccountServiceFirebase(),
          itemsRepository,
          accountRepository)
          as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
}
