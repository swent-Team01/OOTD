package com.android.ootd.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.android.ootd.model.user.User

object UserSelectionFieldTestTags {
  const val INPUT_USERNAME = "inputUsername"
  const val USERNAME_SUGGESTION = "usernameSuggestion"
  const val NO_RESULTS_MESSAGE = "noResultsMessage"
}

@Composable
fun UserSelectionField(
    usernameText: String,
    onUsernameTextChanged: (String) -> Unit,
    usernameSuggestions: List<User>,
    onUsernameSuggestionSelected: (User) -> Unit,
    onSuggestionsDismissed: () -> Unit,
    expanded: Boolean
) {
  Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Icon(
        imageVector = Icons.Default.Search,
        contentDescription = "Search",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(40.dp))

    Spacer(modifier = Modifier.width(8.dp))

    Box(modifier = Modifier.weight(1f)) {
      OutlinedTextField(
          value = usernameText,
          onValueChange = { onUsernameTextChanged(it) },
          placeholder = { Text("Username", color = MaterialTheme.colorScheme.onSurfaceVariant) },
          trailingIcon = {
            if (usernameText.isNotEmpty()) {
              IconButton(onClick = { onUsernameTextChanged("") }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          },
          colors =
              OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = MaterialTheme.colorScheme.outline,
                  unfocusedBorderColor = MaterialTheme.colorScheme.primary,
                  focusedContainerColor = MaterialTheme.colorScheme.surface,
                  unfocusedContainerColor = MaterialTheme.colorScheme.surface),
          shape = RoundedCornerShape(24.dp),
          modifier = Modifier.fillMaxWidth().testTag(UserSelectionFieldTestTags.INPUT_USERNAME),
          singleLine = true)

      DropdownMenu(
          expanded = expanded || usernameText.isNotEmpty(),
          onDismissRequest = onSuggestionsDismissed,
          modifier = Modifier.fillMaxWidth(0.95f)) {
            if (usernameSuggestions.isEmpty()) {
              DropdownMenuItem(
                  text = {
                    Text(
                        "No users found",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium)
                  },
                  onClick = {},
                  enabled = false,
                  modifier = Modifier.testTag(UserSelectionFieldTestTags.NO_RESULTS_MESSAGE))
            } else {
              usernameSuggestions.forEach { user ->
                DropdownMenuItem(
                    text = { Text(user.username) },
                    onClick = { onUsernameSuggestionSelected(user) },
                    modifier = Modifier.testTag(UserSelectionFieldTestTags.USERNAME_SUGGESTION))
              }
            }
          }
    }
  }
}
