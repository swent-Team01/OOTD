package com.android.ootd.ui.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.authentication.AccountService
import com.android.ootd.model.authentication.AccountServiceFirebase
import kotlinx.coroutines.launch

class SplashViewModel(private val accountService: AccountService = AccountServiceFirebase()) :
    ViewModel() {

  fun onAppStart(
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
        onSignedIn()
      } else {
        onNotSignedIn()
      }
    }
  }
}
