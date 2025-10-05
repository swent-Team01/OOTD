package com.android.ootd.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.ootd.model.user.UserRepositoryInMemory

object UserSearchScreenTestTags {
  const val INPUT_USERNAME = "inputUsername"
  const val USERNAME_SUGGESTION = "usernameSuggestion"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchScreen(viewModel: UserSearchViewModel = viewModel()) {
  val uiState by viewModel.uiState.collectAsState()

  Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(16.dp)) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = { /* Handle back */}) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = Color.Gray)
      }

      Spacer(modifier = Modifier.weight(1f))

      Text(text = "OOTD", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4D5FFF))

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
        expanded = uiState.suggestionsExpanded,
        testTagInput = UserSearchScreenTestTags.INPUT_USERNAME,
        testTagSuggestion = UserSearchScreenTestTags.USERNAME_SUGGESTION)

    Spacer(modifier = Modifier.height(24.dp))

    UserProfileCard(
        modifier = Modifier.fillMaxWidth().weight(1f), selectedUser = uiState.selectedUser)
  }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun UserSearchScreenPreview() {
    // Create a mock ViewModel or pass mock state directly
    val mockViewModel = UserSearchViewModel(userRepository = UserRepositoryInMemory()) // or however you initialize it
    UserSearchScreen(viewModel = mockViewModel)
}