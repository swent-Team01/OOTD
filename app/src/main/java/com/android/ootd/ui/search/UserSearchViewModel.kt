package com.android.ootd.ui.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.user.User
import com.android.ootd.model.user.UserRepository
import com.android.ootd.model.user.UserRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

val MAX_NUMBER_SUGGESTIONS = 5

data class SearchUserUIState(
    val username: String = "",
    val userSuggestions: List<User> = emptyList(),
    val selectedUser: User? = null,
    val suggestionsExpanded: Boolean = false
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
        )
    searchUsernames(username)
    _uiState.value =
        _uiState.value.copy(suggestionsExpanded = !_uiState.value.userSuggestions.isEmpty())
  }

  private fun searchUsernames(query: String) {
    _uiState.value = _uiState.value.copy(userSuggestions = emptyList())
    viewModelScope.launch {
      try {
        val allUsers = userRepository.getAllUsers()
        val suggestions = allUsers.filter { it.name.startsWith(query) }.take(MAX_NUMBER_SUGGESTIONS)
        _uiState.value = _uiState.value.copy(userSuggestions = suggestions)
      } catch (e: Exception) {
        Log.e("UserSearchViewModel", "Username search failed: ${e.message}")
        _uiState.value = _uiState.value.copy(userSuggestions = emptyList())
      }
    }
  }

  fun selectUsername(user: User) {
    _uiState.value =
        _uiState.value.copy(
            username = user.name,
            selectedUser = user,
            userSuggestions = emptyList(),
            suggestionsExpanded = false)
  }

  fun suggestionsDismissed() {
    _uiState.value = _uiState.value.copy(suggestionsExpanded = false)
  }
}
