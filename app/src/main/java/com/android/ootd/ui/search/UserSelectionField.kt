package com.android.ootd.ui.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.android.ootd.model.user.User

@Composable
fun UserSelectionField(
    usernameText: String,
    onUsernameTextChanged: (String) -> Unit,
    usernameSuggestions: List<User>,
    onUsernameSuggestionSelected: (User) -> Unit,
    onSuggestionsDismissed: () -> Unit,
    testTagInput: String,
    testTagSuggestion: String,
    expanded: Boolean
) {
  // Should update suggestions always when suggestions is not empty.
  Box(modifier = Modifier.fillMaxWidth()) {
    OutlinedTextField(
        value = usernameText,
        onValueChange = {
          onUsernameTextChanged(
              it) // Here the expanded should change only when suggestions is not empty
        },
        label = { Text("Username") },
        modifier = Modifier.fillMaxWidth().testTag(testTagInput))

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onSuggestionsDismissed,
        modifier = Modifier.fillMaxWidth(0.95f)) {
          usernameSuggestions.forEach { user ->
            DropdownMenuItem(
                text = { Text(user.name) },
                onClick = {
                  onUsernameSuggestionSelected(user) // Here expanded should become false
                },
                modifier = Modifier.testTag(testTagSuggestion))
          }
        }
  }
}
