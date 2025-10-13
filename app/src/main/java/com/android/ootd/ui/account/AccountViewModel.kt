package com.android.ootd.ui.account

import android.net.Uri
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.authentication.AccountService
import com.android.ootd.model.authentication.AccountServiceFirebase
import com.android.ootd.model.user.UserRepository
import com.android.ootd.model.user.UserRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountViewState(
    val username: String = "",
    val googleAccountName: String = "",
    val profilePicture: Uri? = null,
    val errorMsg: String? = null,
    val signedOut: Boolean = false,
    val isLoading: Boolean = false
)

class AccountViewModel(
    private val accountService: AccountService = AccountServiceFirebase(),
    private val userRepository: UserRepository = UserRepositoryProvider.repository
) : ViewModel() {

  private val _uiState = MutableStateFlow(AccountViewState())
  val uiState: StateFlow<AccountViewState> = _uiState.asStateFlow()

  init {
    refreshUIState()
  }

  fun refreshUIState() {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true) }
      accountService.currentUser.collect { user ->
        if (user != null) {
          _uiState.update {
            it.copy(
                googleAccountName = user.email.orEmpty(),
                // Initial profile picture from Google account
                profilePicture = user.photoUrl,
                isLoading = true)
          }

          try {
            val currentUser = userRepository.getUser(user.uid)
            _uiState.update { state ->
              state.copy(
                  username = currentUser.username,
                  // Use Firestore profile picture if available, otherwise keep Google's
                  profilePicture =
                      currentUser.profilePicture.takeIf { it != Uri.EMPTY } ?: state.profilePicture,
                  errorMsg = null,
                  isLoading = false)
            }
          } catch (e: Exception) {
            _uiState.update { state ->
              state.copy(
                  errorMsg = e.localizedMessage ?: "Failed to load user data", isLoading = false)
            }
          }
        } else {
          // No signed-in user: clear state and stop loading
          _uiState.update {
            it.copy(username = "", googleAccountName = "", profilePicture = null, isLoading = false)
          }
        }
      }
    }
  }

  fun signOut(credentialManager: CredentialManager) {
    viewModelScope.launch {
      accountService
          .signOut()
          .fold(
              onSuccess = { _uiState.update { it.copy(signedOut = true) } },
              onFailure = { throwable ->
                _uiState.update { it.copy(errorMsg = throwable.localizedMessage) }
              })
      credentialManager.clearCredentialState(ClearCredentialStateRequest())
    }
  }

  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }
}
