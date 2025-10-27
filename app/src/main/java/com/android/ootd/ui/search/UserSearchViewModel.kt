package com.android.ootd.ui.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.account.AccountRepositoryProvider
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

data class SearchUserUIState(
    val username: String = "",
    val userSuggestions: List<User> = emptyList(),
    val selectedUser: User? = null,
    val isSelectedUserFollowed: Boolean = false,
    val suggestionsExpanded: Boolean = false
)

class UserSearchViewModel(
    private val userRepository: UserRepository = UserRepositoryProvider.repository,
    private val accountRepository: AccountRepository = AccountRepositoryProvider.repository,
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
            _uiState.value.copy(userSuggestions = suggestions, suggestionsExpanded = true)
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
      _uiState.value =
          _uiState.value.copy(
              username = user.username,
              selectedUser = user,
              userSuggestions = emptyList(),
              isSelectedUserFollowed =
                  accountRepository.isMyFriend(userID = myUID, friendID = user.uid),
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

      if (!_uiState.value.isSelectedUserFollowed) {
        accountRepository.addFriend(myUID, friendID)
      } else {
        accountRepository.removeFriend(myUID, friendID)
      }

      _uiState.value =
          _uiState.value.copy(isSelectedUserFollowed = !_uiState.value.isSelectedUserFollowed)
    }
  }

  fun suggestionsDismissed() {
    _uiState.value = _uiState.value.copy(suggestionsExpanded = false)
  }
}
