package com.android.ootd.ui.comments

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import com.android.ootd.model.image.ImageCompressor
import com.android.ootd.model.post.OutfitPostRepository
import com.android.ootd.model.post.OutfitPostRepositoryProvider
import com.android.ootd.model.posts.Comment
import com.android.ootd.model.user.User
import com.android.ootd.model.user.UserRepository
import com.android.ootd.model.user.UserRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UI state for comment operations.
 *
 * @property isLoading Whether a comment operation is in progress
 * @property errorMessage Error message to display, null if no error
 * @property userData Cache of user data by userId to avoid redundant fetches
 * @property isSubmitting Whether a comment submission is in progress
 * @property selectedImage URI of the selected reaction image
 */
data class CommentUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val userData: Map<String, User> = emptyMap(),
    val isSubmitting: Boolean = false,
    val selectedImage: Uri? = null
)

/**
 * ViewModel for managing comment operations.
 *
 * Handles:
 * - Adding comments with optional reaction images
 * - Deleting comments
 * - Fetching and caching user data for comment display
 * - Image selection and compression
 *
 * @property postRepository Repository for post operations
 * @property userRepository Repository for user operations
 * @property imageCompressor Image compression utility
 */
class CommentViewModel(
    private val postRepository: OutfitPostRepository = OutfitPostRepositoryProvider.repository,
    private val userRepository: UserRepository = UserRepositoryProvider.repository,
    private val imageCompressor: ImageCompressor = ImageCompressor()
) : ViewModel() {

  companion object {
    private const val TAG = "CommentViewModel"
    private const val COMPRESSION_THRESHOLD = 200_000L // 200KB
  }

  private val _uiState = MutableStateFlow(CommentUiState())
  val uiState: StateFlow<CommentUiState> = _uiState.asStateFlow()

  /**
   * Fetches user data for a given userId. Uses cached data if available to reduce Firestore reads.
   *
   * @param userId The ID of the user to fetch data for
   * @return The User object, or null if fetching fails
   */
  suspend fun getUserData(userId: String): User? {
    // Check cache first
    _uiState.value.userData[userId]?.let {
      return it
    }

    // Fetch from repository
    return try {
      val user = userRepository.getUser(userId)
      _uiState.value = _uiState.value.copy(userData = _uiState.value.userData + (userId to user))
      user
    } catch (e: Exception) {
      Log.e(TAG, "Failed to fetch user data for userId: $userId", e)
      null
    }
  }

  /**
   * Adds a comment to a post with optional reaction image.
   *
   * If an image is provided, it will be compressed before upload.
   *
   * @param postId The ID of the post to comment on
   * @param userId The ID of the user making the comment
   * @param text The comment text
   * @param imageUri Optional URI of the reaction image
   * @param context Context for image compression
   * @return Result containing the created Comment on success, or exception on failure
   */
  suspend fun addComment(
      postId: String,
      userId: String,
      text: String,
      imageUri: Uri?,
      context: Context
  ): Result<Comment> {
    _uiState.value = _uiState.value.copy(isSubmitting = true)

    return try {
      // Compress image if provided
      val imageData =
          imageUri?.let { uri ->
            imageCompressor.compressImage(
                contentUri = uri, compressionThreshold = COMPRESSION_THRESHOLD, context = context)
          }

      // Call repository to add comment
      val comment =
          postRepository.addCommentToPost(
              postId = postId, userId = userId, commentText = text, reactionImageData = imageData)

      _uiState.value = _uiState.value.copy(isSubmitting = false, selectedImage = null)

      Result.success(comment)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to add comment to post $postId", e)
      _uiState.value =
          _uiState.value.copy(
              isSubmitting = false, errorMessage = "Failed to add comment: ${e.message}")
      Result.failure(e)
    }
  }

  /**
   * Deletes a comment from a post.
   *
   * Also deletes the reaction image from storage if present.
   *
   * @param postId The ID of the post the comment belongs to
   * @param comment The comment to delete
   * @return Result indicating success or failure
   */
  suspend fun deleteComment(postId: String, comment: Comment): Result<Unit> {
    _uiState.value = _uiState.value.copy(isLoading = true)

    return try {
      postRepository.deleteCommentFromPost(postId, comment)

      _uiState.value = _uiState.value.copy(isLoading = false)

      Result.success(Unit)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to delete comment ${comment.commentId}", e)
      _uiState.value =
          _uiState.value.copy(
              isLoading = false, errorMessage = "Failed to delete comment: ${e.message}")
      Result.failure(e)
    }
  }

  /**
   * Sets the selected reaction image URI.
   *
   * @param uri The URI of the selected image, or null to clear selection
   */
  fun setSelectedImage(uri: Uri?) {
    _uiState.value = _uiState.value.copy(selectedImage = uri)
  }

  /** Clears the current error message. */
  fun clearError() {
    _uiState.value = _uiState.value.copy(errorMessage = null)
  }
}
