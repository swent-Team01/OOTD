package com.android.ootd.ui.register

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.account.AccountRepositoryProvider
import com.android.ootd.model.account.MissingLocationException
import com.android.ootd.model.account.TakenUserException
import com.android.ootd.model.authentication.AccountService
import com.android.ootd.model.authentication.AccountServiceFirebase
import com.android.ootd.model.map.emptyLocation
import com.android.ootd.model.user.TakenUsernameException
import com.android.ootd.model.user.User
import com.android.ootd.model.user.UserRepository
import com.android.ootd.model.user.UserRepositoryProvider
import com.android.ootd.ui.map.LocationSelectionViewModel
import com.android.ootd.utils.UsernameValidator
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
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
    val registered: Boolean = false,
)

/**
 * ViewModel for managing user registration state and logic.
 *
 * This ViewModel handles user registration by coordinating with the [UserRepository], managing UI
 * state, and handling errors such as duplicate usernames.
 *
 * @property userRepository The repository used to create new users. Defaults to the provided
 *   instance.
 * @property accountRepository The repository used to create accounts.
 * @property locationSelectionViewModel The ViewModel handling location selection logic.
 * @property auth Firebase authentication instance.
 */
class RegisterViewModel(
    private val userRepository: UserRepository = UserRepositoryProvider.repository,
    private val accountRepository: AccountRepository = AccountRepositoryProvider.repository,
    private val accountService: AccountService = AccountServiceFirebase(),
    val locationSelectionViewModel: LocationSelectionViewModel = LocationSelectionViewModel(),
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

    fun setProfilePicture(profilePic: String){
        _uiState.value = _uiState.value.copy(profilePicture = profilePic)
    }

    fun clearProfilePicture() {
        _uiState.value = _uiState.value.copy(profilePicture = "")
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
    _uiState.value = RegisterUserViewModel()
    locationSelectionViewModel.clearSelectedLocation()
  }

  /**
   * Marks the registration success flag as handled. This should be called after the UI has
   * responded to a successful registration.
   */
  fun markRegisteredHandled() {
    _uiState.value = _uiState.value.copy(registered = false)
  }

  /**
   * Validates that the user is at least 13 years old.
   *
   * @param dateOfBirth The date of birth in "dd/MM/yyyy" format.
   * @return Error message if validation fails, null otherwise.
   */
  private fun validateAge(dateOfBirth: String): String? {
    if (dateOfBirth.isBlank()) {
      return "Date of birth is required"
    }

    return try {
      val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
      val birthDate = LocalDate.parse(dateOfBirth, formatter)
      val today = LocalDate.now()
      val age = Period.between(birthDate, today).years

      if (age < 13) {
        "User must be at least 13"
      } else {
        null
      }
    } catch (_: DateTimeParseException) {
      "Invalid date format"
    }
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

    val ageError = validateAge(uiState.value.dateOfBirth)
    if (ageError != null) {
      _uiState.value = _uiState.value.copy(errorMsg = ageError)
      return
    }

    showLoading(true)
    loadUser(uname, uiState.value.profilePicture)
  }

  /**
   * Attempts to create a user with the given username. Handles success, [TakenUsernameException],
   * and other exceptions.
   *
   * @param username The username to register.
   */
  private fun loadUser(username: String, profilePicture: String) {
    viewModelScope.launch {
      try {
        val userId = auth.currentUser!!.uid
        val user = User(uid = userId, ownerId = userId, username = username, profilePicture = profilePicture)
        val email = auth.currentUser!!.email.orEmpty()
        val location = locationSelectionViewModel.uiState.value.selectedLocation ?: emptyLocation
        if (location == emptyLocation) throw MissingLocationException()
        accountRepository.createAccount(user, email,profilePicture = profilePicture, uiState.value.dateOfBirth, location)
        userRepository.createUser(
            username,
            userId,
            ownerId = userId,
            profilePicture =
                profilePicture) // TODO: Add profile picture to register (empty string for now)
        _uiState.value = _uiState.value.copy(registered = true, username = username)
      } catch (e: Exception) {
        when (e) {
          is MissingLocationException -> {
            _uiState.value =
                _uiState.value.copy(
                    errorMsg = "Please select a location before registering", registered = false)
          }
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

  // ----------------- GPS Location helpers -----------------
  /** Called when location permission is granted by the user. Initiates GPS location retrieval. */
  @Suppress("MissingPermission")
  fun onLocationPermissionGranted() {
    locationSelectionViewModel.onLocationPermissionGranted(
        onError = { errorMessage -> _uiState.value = _uiState.value.copy(errorMsg = errorMessage) })
  }

  /** Called when location permission is denied by the user. */
  fun onLocationPermissionDenied() {
    locationSelectionViewModel.onLocationPermissionDenied(
        onDenied = {
          _uiState.value = _uiState.value.copy(errorMsg = "Location permission is required")
        })
  }
}
