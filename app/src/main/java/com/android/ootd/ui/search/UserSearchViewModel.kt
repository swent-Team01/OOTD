package com.android.ootd.ui.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.user.User
import com.android.ootd.model.user.UserRepository
import com.android.ootd.model.user.UserRepositoryProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

val MAX_NUMBER_SUGGESTIONS = 5

data class SearchUserUIState(
    val username: String = "",
    val userSuggestions: List<User> = emptyList(),
    val selectedUser: User? = null,
    val isSelectedUserFollowed: Boolean = false,
    val suggestionsExpanded: Boolean = false
)

class UserSearchViewModel(
    private val userRepository: UserRepository = UserRepositoryProvider.repository,
    private val overrideUser: Boolean = false
) : ViewModel() {
  private val _uiState = MutableStateFlow(SearchUserUIState())
  val uiState: StateFlow<SearchUserUIState> = _uiState.asStateFlow()

  fun updateUsername(username: String) {
    _uiState.value =
        _uiState.value.copy(
            username = username,
            selectedUser = if (username.isBlank()) null else _uiState.value.selectedUser,
        )
    searchUsernames(username)
    _uiState.value =
        // Whenever they are searching we should show suggestions
        _uiState.value.copy(suggestionsExpanded = true)
  }

  private fun searchUsernames(query: String) {
    _uiState.value = _uiState.value.copy(userSuggestions = emptyList())
    viewModelScope.launch {
      try {
        val allUsers = userRepository.getAllUsers()
        val suggestions =
            allUsers
                .filter { it.username.startsWith(query, ignoreCase = true) }
                .take(MAX_NUMBER_SUGGESTIONS)
        _uiState.value = _uiState.value.copy(userSuggestions = suggestions)
      } catch (e: Exception) {
        Log.e("UserSearchViewModel", "Username search failed: ${e.message}")
        _uiState.value = _uiState.value.copy(userSuggestions = emptyList())
      }
    }
  }

  fun selectUsername(user: User) {
    viewModelScope.launch {
      _uiState.value =
          _uiState.value.copy(
              username = user.username,
              selectedUser = user,
              userSuggestions = emptyList(),
              isSelectedUserFollowed = userRepository.isMyFriend(user.uid),
              suggestionsExpanded = false)
    }
  }

  /**
   * Enacts what happens when the follow button is clicked depending on whether the user is already
   * followed or not by the authenticated user.
   */
  fun pressFollowButton() {
    viewModelScope.launch {

      // OverrideUser is only used for the preview screen for easier testing
      val myUID = if (overrideUser) "user1" else (Firebase.auth.currentUser?.uid ?: "")

      if (myUID == "") {
        throw IllegalStateException("The user is not authenticated")
      }

      if (_uiState.value.selectedUser == null) {
        // This will not happen, there is already a check when building the UserProfileCard
        // However, it is required by SonarCloud.
        throw IllegalStateException("There is no selected user")
      }

      val friendID = _uiState.value.selectedUser?.uid ?: ""
      val friendUsername = _uiState.value.selectedUser?.username ?: ""

      if (!_uiState.value.isSelectedUserFollowed) {
        userRepository.addFriend(myUID, friendID, friendUsername)
      } else {
        userRepository.removeFriend(myUID, friendID, friendUsername)
      }

      _uiState.value =
          _uiState.value.copy(isSelectedUserFollowed = !_uiState.value.isSelectedUserFollowed)
    }
  }

  fun suggestionsDismissed() {
    _uiState.value = _uiState.value.copy(suggestionsExpanded = false)
  }
}
