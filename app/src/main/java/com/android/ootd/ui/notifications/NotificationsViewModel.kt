package com.android.ootd.ui.notifications

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

private const val NOTIFICATION_TYPE_FOLLOW_REQUEST = "FOLLOW_REQUEST"

data class FollowRequestItem(val notification: Notification, val senderUser: User)

data class NotificationsUIState(
    val followRequests: List<FollowRequestItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val overrideNotificationPopup: Boolean = false
)

class NotificationsViewModel(
    private val notificationRepository: NotificationRepository =
        NotificationRepositoryProvider.repository,
    private val userRepository: UserRepository = UserRepositoryProvider.repository,
    private val accountRepository: AccountRepository = AccountRepositoryProvider.repository,
    private val overrideUser: Boolean = false,
    private val testUserId: String = "user1",
    private val overrideNotificationPopup: Boolean = false
) : ViewModel() {

  private val _uiState =
      MutableStateFlow(NotificationsUIState(overrideNotificationPopup = overrideNotificationPopup))
  val uiState: StateFlow<NotificationsUIState> = _uiState.asStateFlow()

  init {
    loadFollowRequests()
  }

  fun loadFollowRequests() {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
      try {
        val currentUserId = if (overrideUser) testUserId else (Firebase.auth.currentUser?.uid ?: "")

        if (currentUserId.isBlank()) {
          _uiState.value =
              _uiState.value.copy(isLoading = false, errorMessage = "User not authenticated")
          return@launch
        }

        // Get all notifications for the current user
        val notifications = notificationRepository.getNotificationsForReceiver(currentUserId)

        // Filter only follow request notifications
        val followRequestNotifications =
            notifications.filter { it.type == NOTIFICATION_TYPE_FOLLOW_REQUEST }

        // Fetch sender user details for each notification
        val followRequests =
            followRequestNotifications.mapNotNull { notification ->
              try {
                val senderUser = userRepository.getUser(notification.senderId)
                FollowRequestItem(notification = notification, senderUser = senderUser)
              } catch (e: Exception) {
                Log.e(
                    "NotificationsViewModel",
                    "Error fetching user ${notification.senderId}: ${e.message}")
                null
              }
            }

        _uiState.value =
            _uiState.value.copy(
                followRequests = followRequests, isLoading = false, errorMessage = null)
      } catch (e: Exception) {
        Log.e("NotificationsViewModel", "Error loading follow requests: ${e.message}", e)
        _uiState.value =
            _uiState.value.copy(
                isLoading = false,
                errorMessage = "Failed to load notifications. Please check your connection.")
      }
    }
  }

  fun acceptFollowRequest(followRequestItem: FollowRequestItem) {
    viewModelScope.launch {
      try {
        val currentUserId = if (overrideUser) testUserId else (Firebase.auth.currentUser?.uid ?: "")

        if (currentUserId.isBlank()) {
          _uiState.value = _uiState.value.copy(errorMessage = "User not authenticated")
          return@launch
        }

        // Add the sender as a friend
        val wasAddedToBoth =
            accountRepository.addFriend(currentUserId, followRequestItem.notification.senderId)

        // If I could not update both friend lists
        // I throw an exception such that the notification does not disappear.
        // This will also help with offline mode
        check(wasAddedToBoth) { "Could not update both friend lists" }
        // Delete the notification
        notificationRepository.deleteNotification(followRequestItem.notification)

        // Remove from UI state
        _uiState.value =
            _uiState.value.copy(
                followRequests =
                    _uiState.value.followRequests.filter {
                      it.notification.uid != followRequestItem.notification.uid
                    },
                errorMessage = null)
      } catch (e: Exception) {
        Log.e("NotificationsViewModel", "Error accepting follow request: ${e.message}", e)
        _uiState.value =
            _uiState.value.copy(errorMessage = "Failed to accept request. Please try again.")
      }
    }
  }

  fun deleteFollowRequest(followRequestItem: FollowRequestItem) {
    viewModelScope.launch {
      try {
        // Delete the notification
        notificationRepository.deleteNotification(followRequestItem.notification)

        // Remove from UI state
        _uiState.value =
            _uiState.value.copy(
                followRequests =
                    _uiState.value.followRequests.filter {
                      it.notification.uid != followRequestItem.notification.uid
                    },
                errorMessage = null)
      } catch (e: Exception) {
        Log.e("NotificationsViewModel", "Error deleting follow request: ${e.message}", e)
        _uiState.value =
            _uiState.value.copy(errorMessage = "Failed to delete request. Please try again.")
      }
    }
  }

  fun clearError() {
    _uiState.value = _uiState.value.copy(errorMessage = null)
  }
}
