package com.android.ootd.ui.account

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AccountPage(
    accountModel: AccountPageViewModel = viewModel(),
    credentialManager: CredentialManager = CredentialManager.create(LocalContext.current),
    onEditAccount: () -> Unit = {},
) {
  val uiState by accountModel.uiState.collectAsState()
  val context = LocalContext.current
}
