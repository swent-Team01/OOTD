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

private const val MAX_NUMBER_SUGGESTIONS = 5

data class SearchUserUIState(
    val username: String = "",
    val userSuggestions: List<User> = emptyList(),
    val selectedUser: User? = null,
    val isSelectedUserFollowed: Boolean = false,
    val suggestionsExpanded: Boolean = false,
    val errorMessage: String? = null,
    val currentUsername: String = ""
)

class UserSearchViewModel(
    private val userRepository: UserRepository = UserRepositoryProvider.repository
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

  fun getCurrentUsername(): String {
    viewModelScope.launch {
      if (_uiState.value.currentUsername == "") {
        val currentUser = userRepository.getUser(Firebase.auth.currentUser?.uid ?: "")
        _uiState.value = _uiState.value.copy(currentUsername = currentUser.username)
      }
    }
    return _uiState.value.currentUsername
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

  fun clearError() {
    _uiState.value = _uiState.value.copy(errorMessage = null)
  }
}
