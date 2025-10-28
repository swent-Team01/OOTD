package com.android.ootd.ui.account
/*
 * DISCLAIMER: This file was created/modified with the assistance of GitHub Copilot.
 * Copilot provided suggestions which were reviewed and adapted by the developer.
 */
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.account.AccountRepositoryProvider
import com.android.ootd.model.authentication.AccountService
import com.android.ootd.model.authentication.AccountServiceFirebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the Account screen.
 *
 * @param username display name shown in the UI.
 * @param googleAccountName Google email associated with the signed-in account.
 * @param profilePicture URL string for the profile picture (empty if none).
 * @param errorMsg transient error message to show to the user.
 * @param signedOut set to true when sign-out completed.
 * @param isLoading true while loading remote data.
 */
data class AccountViewState(
    val username: String = "",
    val googleAccountName: String = "",
    val profilePicture: String = "",
    val errorMsg: String? = null,
    val signedOut: Boolean = false,
    val isLoading: Boolean = false,
    val isPrivate: Boolean = false,
)

/**
 * ViewModel that exposes [AccountViewState] and handles account-related actions.
 *
 * It observes the current authenticated user from [AccountService], loads additional account data
 * from [AccountRepository], and provides methods to refresh the state, sign out and clear transient
 * errors.
 *
 * @param accountService source of authentication state (defaults to Firebase).
 * @param accountRepository source of account profile data.
 */
class AccountViewModel(
    private val accountService: AccountService = AccountServiceFirebase(),
    private val accountRepository: AccountRepository = AccountRepositoryProvider.repository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(AccountViewState())
  val uiState: StateFlow<AccountViewState> = _uiState.asStateFlow()

  // Track authenticated user id for actions (don't rely on a last-loaded value)
  private var authenticatedUserId: String? = null
  private var isSigningOut: Boolean = false
  private var isTogglingPrivacy: Boolean = false

  init {
    observeAuthState()
  }

  /** Start observing auth state and update the UI when the current account changes. */
  private fun observeAuthState() {
    viewModelScope.launch {
      // Avoid redundant loads by only reacting when the uid changes
      accountService.currentUser
          .distinctUntilChangedBy { it?.uid }
          .collect { account ->
            if (account != null) {
              authenticatedUserId = account.uid
              // Pass Google photo URL as String fallback (may be null)
              loadAccountData(account.uid, account.email.orEmpty(), account.photoUrl?.toString())
            } else {
              authenticatedUserId = null
              // Don't reset state if we're in the process of signing out
              if (!isSigningOut) {
                _uiState.update { AccountViewState() }
              }
            }
          }
    }
  }

  /**
   * Load account profile from the repository and update [uiState].
   *
   * Runs inside a coroutine scope.
   */
  private suspend fun loadAccountData(uid: String, email: String, googlePhotoUrl: String?) {
    _uiState.update { it.copy(googleAccountName = email, isLoading = true) }

    try {
      val currentAccount = accountRepository.getAccount(uid)

      // Use account profilePicture if available, otherwise fall back to Google photo URL
      val profilePictureString =
          if (currentAccount.profilePicture.isNotBlank()) {
            currentAccount.profilePicture
          } else {
            googlePhotoUrl ?: ""
          }

      _uiState.update {
        it.copy(
            username = currentAccount.username,
            profilePicture = profilePictureString,
            isPrivate = currentAccount.isPrivate,
            errorMsg = null,
            isLoading = false)
      }
    } catch (e: Exception) {
      _uiState.update {
        it.copy(
            errorMsg = e.localizedMessage ?: "Failed to load account data",
            isLoading = false,
            googleAccountName = email,
            profilePicture = googlePhotoUrl ?: "")
      }
    }
  }

  /** Toggle the account's privacy setting and update [uiState] accordingly. */
  fun onTogglePrivacy() {
    val uid = authenticatedUserId

    if (uid == null) {
      _uiState.update { it.copy(errorMsg = "No authenticated user") }
      return
    }

    if (isTogglingPrivacy) return
    isTogglingPrivacy = true

    val previous = _uiState.value.isPrivate
    _uiState.update { it.copy(isPrivate = !previous) }

    viewModelScope.launch {
      try {
        val newValue = accountRepository.togglePrivacy(uid)
        _uiState.update { it.copy(isPrivate = newValue, errorMsg = null) }
      } catch (e: Exception) {
        _uiState.update {
          it.copy(isPrivate = previous, errorMsg = e.localizedMessage ?: "Failed to toggle privacy")
        }
      } finally {
        isTogglingPrivacy = false
      }
    }
  }

  /**
   * Sign out the current account and clear platform credentials.
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
