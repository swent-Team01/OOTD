package com.android.ootd.ui.account
/*
 * DISCLAIMER: This file was created/modified with the assistance of GitHub Copilot.
 * Copilot provided suggestions which were reviewed and adapted by the developer.
 */
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

/**
 * UI state for the Account screen.
 *
 * @param username display name shown in the UI.
 * @param googleAccountName Google email associated with the signed-in account.
 * @param profilePicture optional URI for the profile picture.
 * @param errorMsg transient error message to show to the user.
 * @param signedOut set to true when sign-out completed.
 * @param isLoading true while loading remote data.
 */
data class AccountViewState(
    val username: String = "",
    val googleAccountName: String = "",
    val profilePicture: Uri? = null,
    val errorMsg: String? = null,
    val signedOut: Boolean = false,
    val isLoading: Boolean = false
)

/**
 * ViewModel that exposes [AccountViewState] and handles account-related actions.
 *
 * It observes the current authenticated user from [AccountService], loads additional user data from
 * [UserRepository], and provides methods to refresh the state, sign out and clear transient errors.
 *
 * @param accountService source of authentication state (defaults to Firebase).
 * @param userRepository source of user profile data.
 */
class AccountViewModel(
    private val accountService: AccountService = AccountServiceFirebase(),
    private val userRepository: UserRepository = UserRepositoryProvider.repository
) : ViewModel() {

  private val _uiState = MutableStateFlow(AccountViewState())
  val uiState: StateFlow<AccountViewState> = _uiState.asStateFlow()

  private var lastLoadedUserId: String? = null
  private var isSigningOut: Boolean = false

  init {
    observeAuthState()
  }

  /** Start observing auth state and update the UI when the current user changes. */
  private fun observeAuthState() {
    viewModelScope.launch {
      accountService.currentUser.collect { user ->
        if (user != null) {
          // Only load user data if we haven't loaded this user yet or it's a different user
          if (lastLoadedUserId != user.uid) {
            lastLoadedUserId = user.uid
            loadUserData(user.uid, user.email.orEmpty(), user.photoUrl)
          }
        } else {
          lastLoadedUserId = null
          // Don't reset state if we're in the process of signing out
          if (!isSigningOut) {
            _uiState.update { AccountViewState() }
          }
        }
      }
    }
  }

  /**
   * Load user profile from the repository and update [uiState].
   *
   * Runs inside a coroutine scope.
   */
  private suspend fun loadUserData(uid: String, email: String, googlePhotoUri: Uri?) {
    _uiState.update { it.copy(googleAccountName = email, isLoading = true) }

    try {
      val currentUser = userRepository.getUser(uid)
      _uiState.update {
        it.copy(
            username = currentUser.username,
            profilePicture =
                currentUser.profilePicture.takeIf { it != Uri.EMPTY } ?: googlePhotoUri,
            errorMsg = null,
            isLoading = false)
      }
    } catch (e: Exception) {
      _uiState.update {
        it.copy(
            errorMsg = e.localizedMessage ?: "Failed to load user data",
            isLoading = false,
            googleAccountName = email,
            profilePicture = googlePhotoUri)
      }
    }
  }

  /**
   * Sign out the current user and clear platform credentials.
   *
   * @param credentialManager used to clear stored credentials after sign-out.
   */
  fun signOut(credentialManager: CredentialManager) {
    viewModelScope.launch {
      isSigningOut = true
      accountService
          .signOut()
          .fold(
              onSuccess = { _uiState.update { it.copy(signedOut = true) } },
              onFailure = { throwable ->
                isSigningOut = false
                _uiState.update { it.copy(errorMsg = throwable.localizedMessage) }
              })
      credentialManager.clearCredentialState(ClearCredentialStateRequest())
    }
  }

  /** Clear any transient error message shown in the UI. */
  fun clearErrorMsg() {
    _uiState.update { it.copy(errorMsg = null) }
  }
}
