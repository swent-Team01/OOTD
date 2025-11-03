package com.android.ootd.ui.search

import NotificationRepository
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.account.AccountRepositoryProvider
import com.android.ootd.model.notifications.Notification
import com.android.ootd.model.notifications.NotificationRepositoryProvider
import com.android.ootd.model.user.User
import com.android.ootd.model.user.UserRepository
import com.android.ootd.model.user.UserRepositoryProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val MAX_NUMBER_SUGGESTIONS = 5
private const val testingUsername = "user1"
private const val NOTIFICATION_TYPE_FOLLOW_REQUEST = "FOLLOW_REQUEST"

data class SearchUserUIState(
    val username: String = "",
    val userSuggestions: List<User> = emptyList(),
    val selectedUser: User? = null,
    val isSelectedUserFollowed: Boolean = false,
    val hasRequestPending: Boolean = false,
    val suggestionsExpanded: Boolean = false,
    val errorMessage: String? = null
)

class UserSearchViewModel(
    private val userRepository: UserRepository = UserRepositoryProvider.repository,
    private val accountRepository: AccountRepository = AccountRepositoryProvider.repository,
    private val notificationRepository: NotificationRepository =
        NotificationRepositoryProvider.repository,
    private val overrideUser: Boolean = false
) : ViewModel() {
  private val _uiState = MutableStateFlow(SearchUserUIState())
  val uiState: StateFlow<SearchUserUIState> = _uiState.asStateFlow()

  fun updateUsername(username: String) {
    _uiState.value =
        _uiState.value.copy(
            username = username,
            selectedUser = if (username.isBlank()) null else _uiState.value.selectedUser,
            errorMessage = null)
    searchUsernames(username)
  }

  private fun searchUsernames(query: String) {
    viewModelScope.launch {
      try {
        val allUsers = userRepository.getAllUsers()
        val suggestions =
            allUsers
                .filter { it.username.startsWith(query, ignoreCase = true) }
                .take(MAX_NUMBER_SUGGESTIONS)
        _uiState.value =
            _uiState.value.copy(
                userSuggestions = suggestions, suggestionsExpanded = true, errorMessage = null)
      } catch (e: Exception) {
        Log.e("UserSearchViewModel", "Username search failed: ${e.message}")
        _uiState.value =
            _uiState.value.copy(userSuggestions = emptyList(), suggestionsExpanded = true)
      }
    }
  }

  fun selectUsername(user: User) {
    val myUID = if (overrideUser) testingUsername else (Firebase.auth.currentUser?.uid ?: "")
    if (myUID == "") {
      throw IllegalStateException("The user is not authenticated")
    }
    viewModelScope.launch {
      try {
        val isFollowed = accountRepository.isMyFriend(userID = myUID, friendID = user.uid)
        val hasPendingRequest = checkForPendingRequest(myUID, user.uid)

        _uiState.value =
            _uiState.value.copy(
                username = user.username,
                selectedUser = user,
                userSuggestions = emptyList(),
                isSelectedUserFollowed = isFollowed,
                hasRequestPending = hasPendingRequest,
                suggestionsExpanded = false,
                errorMessage = null)
      } catch (e: Exception) {
        Log.e("UserSearchViewModel", "Error selecting user: ${e.message}")
        _uiState.value =
            _uiState.value.copy(
                username = user.username,
                selectedUser = user,
                userSuggestions = emptyList(),
                isSelectedUserFollowed = false,
                hasRequestPending = false,
                suggestionsExpanded = false,
                errorMessage = null)
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

  /**
   * Enacts what happens when the follow button is clicked. Creates a follow request notification
   * for the selected user.
   */
  fun pressFollowButton() {
    viewModelScope.launch {
      try {
        // OverrideUser is only used for the preview screen for easier testing
        val myUID = if (overrideUser) testingUsername else (Firebase.auth.currentUser?.uid ?: "")

        if (myUID == "") {
          throw IllegalStateException("The user is not authenticated")
        }

        if (_uiState.value.selectedUser == null) {
          // This will not happen, there is already a check when building the UserProfileCard
          // However, it is required by SonarCloud.
          throw IllegalStateException("There is no selected user")
        }

        val friendID = _uiState.value.selectedUser?.uid ?: ""

        if (_uiState.value.isSelectedUserFollowed) {
          // User is already followed, so unfollow
          accountRepository.removeFriend(myUID, friendID)
          _uiState.value = _uiState.value.copy(isSelectedUserFollowed = false, errorMessage = null)
        } else if (_uiState.value.hasRequestPending) {
          // Request already sent do nothing.
        } else {
          // Send a new follow request notification
          val notification =
              Notification(
                  uid = notificationRepository.getNewUid(),
                  senderId = myUID,
                  receiverId = friendID,
                  type = NOTIFICATION_TYPE_FOLLOW_REQUEST,
                  content = "wants to follow you")

          notificationRepository.addNotification(notification)

          _uiState.value = _uiState.value.copy(hasRequestPending = true, errorMessage = null)
        }
      } catch (e: Exception) {
        Log.e("UserSearchViewModel", "Error in pressFollowButton: ${e.message}", e)
        _uiState.value =
            _uiState.value.copy(
                errorMessage = "Something went wrong. Please check your connection and try again.")
      }
    }
  }

  fun suggestionsDismissed() {
    _uiState.value = _uiState.value.copy(suggestionsExpanded = false)
  }

  fun clearError() {
    _uiState.value = _uiState.value.copy(errorMessage = null)
  }
}
