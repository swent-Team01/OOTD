package com.android.ootd.ui.register

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.user.TakenUsernameException
import com.android.ootd.model.user.UserRepository
import com.android.ootd.model.user.UserRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class User(
    val uid: String = "",
    val username: String = "",
    val errorMsg: String? = null,
    val isLoading: Boolean = false,
    val registered: Boolean = false
)

class RegisterViewModel(
    private val repository: UserRepository = UserRepositoryProvider.repository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(User())
  val uiState: StateFlow<User> = _uiState.asStateFlow()

  init {
    refresh()
  }

  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  fun showLoading(v: Boolean) {
    _uiState.value = _uiState.value.copy(isLoading = v)
  }

  fun emitError(msg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = msg)
  }

  fun setUsername(uname: String) {
    _uiState.value = _uiState.value.copy(username = uname)
  }

  fun refresh() {
    clearErrorMsg()
    _uiState.value = _uiState.value.copy(isLoading = false, registered = false)
  }

  fun markRegisteredHandled() {
    _uiState.value = _uiState.value.copy(registered = false)
  }

  fun registerUser() {
    val uname = uiState.value.username.trim()
    clearErrorMsg()
    _uiState.value = _uiState.value.copy(registered = false)
    // Start loading when registration kicks off
    showLoading(true)
    loadUser(uname)
  }

  private fun loadUser(username: String) {
    viewModelScope.launch {
      try {
        repository.createUser(username)
        _uiState.value = _uiState.value.copy(registered = true)
      } catch (e: Exception) {
        when (e) {
          is TakenUsernameException -> {
            Log.e("RegisterViewModel", "Username taken", e)
            _uiState.value =
                _uiState.value.copy(
                    errorMsg = "This username has already been taken", username = "")
          }
          else -> {
            Log.e("RegisterViewModel", "Error registering user", e)
            _uiState.value = _uiState.value.copy(errorMsg = "Failed to register user: ${e.message}")
          }
        }
      } finally {
        showLoading(false)
      }
    }
  }
}
