package com.android.ootd.ui.register

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.account.AccountRepositoryProvider
import com.android.ootd.model.account.TakenUserException
import com.android.ootd.model.authentication.AccountService
import com.android.ootd.model.authentication.AccountServiceFirebase
import com.android.ootd.model.user.TakenUsernameException
import com.android.ootd.model.user.User
import com.android.ootd.model.user.UserRepository
import com.android.ootd.model.user.UserRepositoryProvider
import com.android.ootd.utils.UsernameValidator
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Data class representing the user registration state.
 *
 * This immutable state object holds all the information needed for the registration UI, including
 * user input fields, loading state, error messages, and registration status.
 *
 * @property uid The unique identifier of the user. Empty string by default.
 * @property username The username entered by the user. Must be 3-20 characters long and contain
 *   only letters, numbers, and underscores. Empty string by default.
 * @property dateOfBirth The user's date of birth in string format. Empty string by default.
 * @property userEmail The user's email address. Empty string by default.
 * @property profilePicture The URL or path to the user's profile picture. Empty string by default.
 * @property errorMsg An optional error message to display to the user when validation or
 *   registration fails. Null when no error is present.
 * @property isLoading Indicates whether a registration operation is currently in progress. False by
 *   default.
 * @property registered Indicates whether the user has been successfully registered. False by
 *   default. Should be reset after handling the successful registration event.
 */
data class RegisterUserViewModel(
    val uid: String = "",
    val username: String = "",
    val dateOfBirth: String = "",
    val userEmail: String = "",
    val profilePicture: String = "",
    val errorMsg: String? = null,
    val isLoading: Boolean = false,
    val registered: Boolean = false
)

/**
 * ViewModel for managing user registration state and logic.
 *
 * This ViewModel handles user registration by coordinating with the [UserRepository], managing UI
 * state, and handling errors such as duplicate usernames.
 *
 * @property userRepository The repository used to create new users. Defaults to the provided
 *   instance.
 */
class RegisterViewModel(
    private val userRepository: UserRepository = UserRepositoryProvider.repository,
    private val accountRepository: AccountRepository = AccountRepositoryProvider.repository,
    private val accountService: AccountService = AccountServiceFirebase(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

  private val _uiState = MutableStateFlow(RegisterUserViewModel())

  /**
   * A [StateFlow] representing the current registration UI state. Observers can collect this flow
   * to react to state changes.
   */
  val uiState: StateFlow<RegisterUserViewModel> = _uiState.asStateFlow()

  init {
    refresh()
  }

  /** Clears the current error message from the UI state. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /**
   * Sets the loading state.
   *
   * @param v True to show loading indicator, false to hide it.
   */
  fun showLoading(v: Boolean) {
    _uiState.value = _uiState.value.copy(isLoading = v)
  }

  /**
   * Emits an error message to be displayed in the UI.
   *
   * @param msg The error message to display.
   */
  fun emitError(msg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = msg)
  }

  /**
   * Updates the username in the UI state.
   *
   * @param uname The new username value.
   */
  fun setUsername(uname: String) {
    _uiState.value = _uiState.value.copy(username = uname)
  }

  /**
   * Updates the dateOfBirth in the UI state
   *
   * @param date The users date of birth
   */
  fun setDateOfBirth(date: String) {
    _uiState.value = _uiState.value.copy(dateOfBirth = date)
  }

  /**
   * Resets the UI state to its initial values. Clears any error messages and resets loading and
   * registration flags.
   */
  fun refresh() {
    clearErrorMsg()
    _uiState.value = _uiState.value.copy(isLoading = false, registered = false)
  }

  /**
   * Marks the registration success flag as handled. This should be called after the UI has
   * responded to a successful registration.
   */
  fun markRegisteredHandled() {
    _uiState.value = _uiState.value.copy(registered = false)
  }

  /**
   * Initiates the user registration process. Validates the username, starts loading state, and
   * attempts to create the user. If the users name is shorter than 3 characters, longer than 20 or
   * has anything else than letters, numbers, and underscores it will not be accepted
   */
  fun registerUser() {
    val uname = uiState.value.username.trim()
    clearErrorMsg()
    _uiState.value = _uiState.value.copy(registered = false)

    val validationError = UsernameValidator.errorMessage(uname)

    if (validationError != null) {
      _uiState.value = _uiState.value.copy(errorMsg = validationError)
      return
    }

    showLoading(true)
    loadUser(uname)
  }

  /**
   * Attempts to create a user with the given username. Handles success, [TakenUsernameException],
   * and other exceptions.
   *
   * @param username The username to register.
   */
  private fun loadUser(username: String) {
    viewModelScope.launch {
      try {
        val user = User(uid = auth.currentUser!!.uid, username = username)
        val email = auth.currentUser!!.email.orEmpty()
        accountRepository.createAccount(user, email, uiState.value.dateOfBirth)
        userRepository.createUser(
            username,
            auth.currentUser!!.uid,
            profilePicture =
                user.profilePicture) // TODO: Add profile picture to register (empty string for now)
        _uiState.value = _uiState.value.copy(registered = true, username = username)
      } catch (e: Exception) {
        when (e) {
          is TakenUsernameException -> {
            Log.e("RegisterViewModel", "Username taken", e)
            _uiState.value =
                _uiState.value.copy(
                    errorMsg = "This username has already been taken",
                    username = "",
                    registered = false)
          }
          is TakenUserException -> {
            Log.e("RegisterViewModel", "User already exists", e)
            _uiState.value =
                _uiState.value.copy(
                    errorMsg = "An account with this username already exists",
                    username = "",
                    registered = false)
          }
          else -> {
            Log.e("RegisterViewModel", "Error registering user", e)
            _uiState.value =
                _uiState.value.copy(
                    errorMsg = "Failed to register user: ${e.message}", registered = false)
          }
        }
      } finally {
        showLoading(false)
      }
    }
  }
}
