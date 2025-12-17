package com.android.ootd.ui.search

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.ootd.model.user.UserRepositoryInMemory
import com.android.ootd.ui.theme.OOTDTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchScreen(
    viewModel: UserSearchViewModel = viewModel(),
    onUserClick: (String) -> Unit = {},
    onFindFriendsClick: () -> Unit = {}
) {
  val uiState by viewModel.uiState.collectAsState()

  // Search bar
  UserSelectionField(
      usernameText = uiState.username,
      onUsernameTextChanged = viewModel::updateUsername,
      onUserSuggestionClicked = onUserClick,
      onFindFriendsClick = onFindFriendsClick,
      usernameSuggestions = uiState.userSuggestions,
      expanded = uiState.suggestionsExpanded,
      currentUsername = viewModel.getCurrentUsername())
}

@Suppress("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
fun UserSearchScreenPreview() {
  OOTDTheme {
    val mockViewModel = UserSearchViewModel(userRepository = UserRepositoryInMemory())
    UserSearchScreen(viewModel = mockViewModel, onUserClick = {})
  }
}
