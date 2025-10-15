package com.android.ootd.ui.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.authentication.AccountService
import com.android.ootd.model.authentication.AccountServiceFirebase
import com.android.ootd.model.user.UserRepository
import com.android.ootd.model.user.UserRepositoryProvider
import kotlinx.coroutines.launch

/**
 * ViewModel for the splash screen that checks authentication state on app start and invokes one of
 * the provided callbacks.
 *
 * @param accountService used to query authentication state; defaults to [AccountServiceFirebase].
 */
class SplashViewModel(private val accountService: AccountService = AccountServiceFirebase()) :
    ViewModel() {

  /**
   * Checks authentication status and invokes the appropriate callback.
   *
   * @param
   * @param onSignedIn invoked if a user is signed in.Should navigate to the feed.
   * @param onNotSignedIn invoked if no user is signed in or an error occurs. Should navigate to
   *   sign-in.
   */
  fun onAppStart(
      userRepository: UserRepository = UserRepositoryProvider.repository,
      onSignedIn: () -> Unit = {},
      onNotSignedIn: () -> Unit = {},
  ) {
    viewModelScope.launch {
      val hasUser =
          try {
            accountService.hasUser()
          } catch (_: Exception) {
            false
          }

      if (hasUser) {
        try {
          val userExists = userRepository.userExists(accountService.currentUserId)
          if (userExists) {
            onSignedIn()
          } else {
            onNotSignedIn()
          }
        } catch (e: Exception) {
          onNotSignedIn()
        }
      } else {
        onNotSignedIn()
      }
    }
  }
}
