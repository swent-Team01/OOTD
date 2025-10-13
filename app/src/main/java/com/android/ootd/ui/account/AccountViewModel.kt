package com.android.ootd.ui.account

import android.net.Uri
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.authentication.AccountService
import com.android.ootd.model.authentication.AccountServiceFirebase
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
    val signedOut: Boolean = false
)

class AccountViewModel(private val accountService: AccountService = AccountServiceFirebase()) :
    ViewModel() {
  private val _uiState = MutableStateFlow(AccountViewState())
  val uiState: StateFlow<AccountViewState> = _uiState.asStateFlow()

  init {}

  fun refreshUIState() {}

  fun signOut(credentialManager: CredentialManager): Unit {
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
}
