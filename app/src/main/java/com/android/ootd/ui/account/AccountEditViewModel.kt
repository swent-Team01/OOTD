package com.android.ootd.ui.account

/*
 * DISCLAIMER: This file was created/modified with the assistance of GitHub Copilot.
 * Copilot provided suggestions which were reviewed and adapted by the developer.
 */
import android.util.Log
import androidx.annotation.Keep
import androidx.core.net.toUri
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.account.AccountRepositoryProvider
import com.android.ootd.model.authentication.AccountService
import com.android.ootd.model.authentication.AccountServiceFirebase
import com.android.ootd.model.user.UserRepository
import com.android.ootd.model.user.UserRepositoryProvider
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Keep
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
    val dateOfBirth: String = "",
    val errorMsg: String? = null,
    val signedOut: Boolean = false,
    val isLoading: Boolean = false,
    val isPrivate: Boolean = false,
    val showPrivacyHelp: Boolean = false,
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
 * @param storage Firebase Storage instance for uploading profile pictures.
 */
class AccountEditViewModel(
    private val accountService: AccountService = AccountServiceFirebase(),
    private val accountRepository: AccountRepository = AccountRepositoryProvider.repository,
    private val userRepository: UserRepository = UserRepositoryProvider.repository,
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    // Uploader returns the download URL. Parameters: userId, localPath.
    private val uploader: suspend (String, String, FirebaseStorage) -> String = { uid, local, st ->
      val ref = st.reference.child("profile_pictures/$uid.jpg")
      val fileUri = local.toUri()
      ref.putFile(fileUri).await()
      ref.downloadUrl.await().toString()
    }
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

      _uiState.update {
        it.copy(
            username = currentAccount.username,
            isPrivate = currentAccount.isPrivate,
            profilePicture = currentAccount.profilePicture,
            dateOfBirth = currentAccount.birthday,
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

  /**
   * Update the username for the current account.
   *
   * @param newUsername the new username to set, blank by default
   * @param newDate the new date to set, blank by default
   * @param profilePicture the new profile picture to set, blank by default
   */
  fun editUser(newUsername: String = "", newDate: String = "", profilePicture: String = "") {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true) }

      try {
        val currentUserId = accountService.currentUserId

        accountRepository.editAccount(currentUserId, newUsername, newDate, profilePicture)
        userRepository.editUser(currentUserId, newUsername, profilePicture)
        // Update UI state with new values
        val updatedState = _uiState.value.copy(isLoading = false, errorMsg = null)
        _uiState.update {
          updatedState.copy(
              username = newUsername.ifBlank { it.username },
              dateOfBirth = newDate.ifBlank { it.dateOfBirth },
              profilePicture = profilePicture.ifBlank { it.profilePicture })
        }
      } catch (e: Exception) {
        _uiState.update {
          it.copy(errorMsg = e.localizedMessage ?: "Failed to update account", isLoading = false)
        }
      }
    }
  }

  // This implementation has been done with AI
  /**
   * Upload an image to Firebase Storage.
   *
   * @param localPath the local path/URI of the image
   * @param onResult invoked with the download URL upon success (or the original path if blank)
   * @param onError invoked with the error that occurred
   */
  fun uploadImageToStorage(
      localPath: String,
      onResult: (String) -> Unit,
      onError: (Throwable) -> Unit = {}
  ) {
    if (localPath.isBlank()) {
      onResult(localPath)
      return
    }

    val userId = authenticatedUserId
    if (userId == null) {
      val ex = IllegalStateException("No authenticated user")
      _uiState.update { it.copy(errorMsg = ex.localizedMessage) }
      onError(ex)
      return
    }

    viewModelScope.launch {
      try {
        val downloadUrl = uploader(userId, localPath, storage)
        onResult(downloadUrl)
      } catch (e: Exception) {
        Log.e("AccountViewModel", "Upload failed: ${e.message}", e)
        _uiState.update { it.copy(errorMsg = e.localizedMessage ?: "Failed to upload image") }
        onError(e)
      }
    }
  }

  fun deleteProfilePicture() {
    viewModelScope.launch {
      try {
        val userID = accountService.currentUserId
        accountRepository.deleteProfilePicture(userID)
        userRepository.deleteProfilePicture(userID)
        _uiState.update { it.copy(profilePicture = "") }
      } catch (e: Exception) {
        Log.e("AccountViewModel", "Deleting profilePicture failed: ${e.message}", e)
        _uiState.update {
          it.copy(
              errorMsg = e.localizedMessage ?: "Failed to delete profile picture",
              isLoading = false)
        }
      }
    }
  }

  // --- Help popup intents ---
  fun onPrivacyHelpClick() {
    _uiState.update { it.copy(showPrivacyHelp = !it.showPrivacyHelp) }
  }

  fun onPrivacyHelpDismiss() {
    _uiState.update { it.copy(showPrivacyHelp = false) }
  }
}
