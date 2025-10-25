package com.android.ootd.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.ootd.model.account.AccountRepositoryInMemory
import com.android.ootd.model.user.UserRepositoryInMemory
import com.android.ootd.ui.theme.OOTDTheme

object SearchScreenTestTags {
  const val SEARCH_SCREEN = "searchScreen"
  const val GO_BACK_BUTTON = "goBackButton"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchScreen(viewModel: UserSearchViewModel = viewModel(), onBack: () -> Unit) {
  val uiState by viewModel.uiState.collectAsState()

  Column(
      modifier =
          Modifier.fillMaxSize()
              .background(MaterialTheme.colorScheme.background)
              .verticalScroll(rememberScrollState())
              .padding(vertical = 32.dp, horizontal = 20.dp)
              .testTag(SearchScreenTestTags.SEARCH_SCREEN)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
          IconButton(
              modifier =
                  Modifier.width(48.dp).height(48.dp).testTag(SearchScreenTestTags.GO_BACK_BUTTON),
              onClick = onBack) {
                Icon(
                    modifier = Modifier.width(48.dp).height(48.dp),
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary)
              }

          Spacer(modifier = Modifier.weight(1f))

          Text(
              text = "OOTD",
              style = MaterialTheme.typography.headlineLarge,
              color = MaterialTheme.colorScheme.primary)

          Spacer(modifier = Modifier.weight(1f))

          Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Search bar
        UserSelectionField(
            usernameText = uiState.username,
            onUsernameTextChanged = viewModel::updateUsername,
            usernameSuggestions = uiState.userSuggestions,
            onUsernameSuggestionSelected = viewModel::selectUsername,
            onSuggestionsDismissed = viewModel::suggestionsDismissed,
            expanded = uiState.suggestionsExpanded)

        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.selectedUser != null) {
          UserProfileCard(
              modifier = Modifier.fillMaxWidth().weight(1f),
              selectedUser = uiState.selectedUser,
              isSelectedUserFollowed = uiState.isSelectedUserFollowed,
              onFollowClick = { viewModel.pressFollowButton() })
        }
      }
}

@Preview(showBackground = true)
@Composable
fun UserSearchScreenPreview() {
  OOTDTheme {
    val mockViewModel =
        UserSearchViewModel(
            userRepository = UserRepositoryInMemory(),
            accountRepository = AccountRepositoryInMemory(),
            overrideUser = true)
    UserSearchScreen(viewModel = mockViewModel, onBack = {})
  }
}
