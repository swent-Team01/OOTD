package com.android.ootd.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.ootd.model.account.AccountRepositoryInMemory
import com.android.ootd.model.user.UserRepositoryInMemory
import com.android.ootd.ui.theme.Background
import com.android.ootd.ui.theme.OOTDTheme

object SearchScreenTestTags {
  const val SEARCH_SCREEN = "searchScreen"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchScreen(
    viewModel: UserSearchViewModel = viewModel(),
    onUserClick: (String) -> Unit = {}
) {
  val uiState by viewModel.uiState.collectAsState()

  Column(
      modifier =
          Modifier.fillMaxSize()
              .background(Background)
              .verticalScroll(rememberScrollState())
              .testTag(SearchScreenTestTags.SEARCH_SCREEN),
      verticalArrangement = Arrangement.spacedBy(24.dp)) {

        // Search bar
        UserSelectionField(
            usernameText = uiState.username,
            onUsernameTextChanged = viewModel::updateUsername,
            onUserSuggestionClicked = onUserClick,
            usernameSuggestions = uiState.userSuggestions,
            expanded = uiState.suggestionsExpanded)

        Spacer(modifier = Modifier.height(24.dp))
      }
}

@Suppress("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
fun UserSearchScreenPreview() {
  OOTDTheme {
    val mockViewModel =
        UserSearchViewModel(
            userRepository = UserRepositoryInMemory(),
            accountRepository = AccountRepositoryInMemory(),
            overrideUser = true)
    UserSearchScreen(viewModel = mockViewModel, onUserClick = {})
  }
}
