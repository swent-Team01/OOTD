package com.android.ootd.ui.account

import NotificationRepository
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.account.AccountRepositoryProvider
import com.android.ootd.model.authentication.AccountService
import com.android.ootd.model.authentication.AccountServiceFirebase
import com.android.ootd.model.feed.FeedRepository
import com.android.ootd.model.feed.FeedRepositoryProvider
import com.android.ootd.model.notifications.Notification
import com.android.ootd.model.notifications.NotificationRepositoryProvider
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.model.user.UserRepository
import com.android.ootd.model.user.UserRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val NOTIFICATION_TYPE_FOLLOW_REQUEST = "FOLLOW_REQUEST"

/**
 * Data class representing the UI state for viewing another user's profile.
 *
 * @property username The username of the user being viewed
 * @property profilePicture URL of the user's profile picture, empty string if not set
 * @property isFriend Boolean indicating whether the viewed user is a friend of the current user
 * @property friendPosts List of outfit posts from the user (only populated if they are a friend)
 * @property friendCount Number of friends the viewed user has
 * @property error Boolean indicating if an error occurred while loading the profile
 * @property errorMsg Error message to display, null if no error
 * @property isLoading Boolean indicating if the profile data is currently being loaded
 */
data class ViewUserData(
    val username: String = "",
    val friendId: String = "",
    val profilePicture: String = "",
    val isFriend: Boolean = false,
    val friendPosts: List<OutfitPost> = emptyList(),
    val friendCount: Int = 0,
    val error: Boolean = false,
    val errorMsg: String? = null,
    val isLoading: Boolean = true,
    val hasRequestPending: Boolean = false
)

private const val currentLog = "ViewUserProfile"

/**
 * ViewModel for managing the state and business logic of viewing another user's profile.
 *
 * This ViewModel handles fetching and managing user profile data, including:
 * - User information (username, profile picture)
 * - Friend relationship status
 * - User's outfit posts (visible only if they are a friend)
 * - Error handling and loading states
 *
 * The ViewModel updates its data when the [update] method is called with a valid user ID. It
 * exposes the UI state through [uiState] as a StateFlow that UI components can observe.
 *
 * @property accountService Service for managing user authentication
 * @property userRepository Repository for fetching user data
 * @property accountRepository Repository for managing account relationships (friends)
 * @property feedRepository Repository for fetching user posts
 */
class ViewUserViewModel(
    private val accountService: AccountService = AccountServiceFirebase(),
    private val userRepository: UserRepository = UserRepositoryProvider.repository,
    private val accountRepository: AccountRepository = AccountRepositoryProvider.repository,
    private val feedRepository: FeedRepository = FeedRepositoryProvider.repository,
    private val notificationRepository: NotificationRepository =
        NotificationRepositoryProvider.repository
) : ViewModel() {

  private val _uiState = MutableStateFlow(ViewUserData())
  val uiState: StateFlow<ViewUserData> = _uiState.asStateFlow()

  /**
   * Updates the ViewModel with data for the specified user.
   *
   * Triggers a refresh of the user's profile data if the provided ID is not blank. This method
   * should be called when navigating to a user's profile or when the profile needs to be refreshed.
   *
   * @param friendId The unique identifier of the user whose profile should be displayed
   */
  fun update(friendId: String) {
    if (friendId.isNotBlank()) {
      refresh(friendId)
    }
  }

  /**
   * Refreshes the user profile data from the repositories.
   *
   * @param friendId The unique identifier of the user whose profile is being viewed
   */
  private fun refresh(friendId: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true) }
      try {
        val currentUserId = accountService.currentUserId
        val isFriend = isMyFriend(currentUserId, friendId)
        val friend = userRepository.getUser(friendId)
        val friendPosts = postsToShow(friendId, isFriend)
        val isRequestPending = checkForPendingRequest(currentUserId, friendId)

        _uiState.update {
          it.copy(
              username = friend.username,
              friendId = friendId,
              hasRequestPending = isRequestPending,
              profilePicture = friend.profilePicture,
              friendPosts = friendPosts,
              isLoading = false)
        }
      } catch (e: Exception) {
        Log.e(currentLog, "Failed to fetch user data ${e.message}")
        _uiState.update { it.copy(error = true, errorMsg = e.localizedMessage, isLoading = false) }
      }
    }
  }

  /**
   * Checks if the specified user is a friend of the current user.
   *
   * Updates the UI state with the friend status and returns the result.
   *
   * @param userId The ID of the current user
   * @param friendId The ID of the user to check friendship status with
   * @return True if the users are friends, false otherwise
   */
  private suspend fun isMyFriend(userId: String, friendId: String): Boolean {
    val isFriend = accountRepository.isMyFriend(userId, friendId)
    _uiState.update { it.copy(isFriend = isFriend) }
    return isFriend
  }

  /**
   * Retrieves the posts to display based on the friend relationship status.
   *
   * If the user is a friend, fetches their outfit posts from the feed repository. Otherwise,
   * returns an empty list to maintain privacy.
   *
   * @param friendId The ID of the user whose posts to retrieve
   * @param isFriend Whether the user is a friend of the current user
   * @return List of outfit posts if the user is a friend, empty list otherwise
   */
  private suspend fun postsToShow(friendId: String, isFriend: Boolean): List<OutfitPost> {
    if (isFriend) {
      return feedRepository.getFeedForUids(listOf(friendId))
    }
    return emptyList()
  }

  /**
   * Enacts what happens when the follow button is clicked. Creates a follow request notification
   * for the selected user.
   */
  fun pressFollowButton() {
    viewModelScope.launch {
      try {
        val friendId = _uiState.value.friendId
        val currentUserId = accountService.currentUserId
        if (_uiState.value.isFriend) {
          // Already followed → unfollow
          accountRepository.removeFriend(currentUserId, friendId)
          _uiState.value = _uiState.value.copy(isFriend = false, errorMsg = null)
          return@launch
        }

        if (_uiState.value.hasRequestPending) {
          // Request already sent → no-op
          return@launch
        }

        // Send a new follow request notification
        val notification =
            Notification(
                uid =
                    notificationRepository.getFollowNotificationId(
                        senderId = currentUserId, receiverId = friendId),
                senderId = currentUserId,
                receiverId = friendId,
                type = NOTIFICATION_TYPE_FOLLOW_REQUEST,
                content = "wants to follow you",
                senderName = accountRepository.getAccount(currentUserId).username)

        notificationRepository.addNotification(notification)
        _uiState.value = _uiState.value.copy(hasRequestPending = true, errorMsg = null)
      } catch (e: Exception) {
        Log.e("ViewUserViewModel", "Error in pressFollowButton: ${e.message}", e)
        _uiState.value =
            _uiState.value.copy(
                errorMsg = "Something went wrong. Please check your connection and try again.")
      }
    }
  }

  /**
   * Checks if there is already a pending follow request notification from the current user to the
   * specified user.
   */
  private suspend fun checkForPendingRequest(senderId: String, receiverId: String): Boolean {
    return try {
      val notifications = notificationRepository.getNotificationsForSender(senderId)
      notifications.any {
        it.receiverId == receiverId && it.type == NOTIFICATION_TYPE_FOLLOW_REQUEST
      }
    } catch (e: Exception) {
      Log.e("UserSearchViewModel", "Error checking for pending request: ${e.message}")
      false
    }
  }
}
