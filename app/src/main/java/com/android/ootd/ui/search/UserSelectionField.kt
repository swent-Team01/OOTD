package com.android.ootd.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.ootd.model.user.User
import com.android.ootd.ui.theme.Background
import com.android.ootd.ui.theme.OnSurfaceVariant
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Secondary
import com.android.ootd.ui.theme.Typography
import com.android.ootd.utils.composables.BackArrow
import com.android.ootd.utils.composables.ProfilePicture

object UserSelectionFieldTestTags {
  const val INPUT_USERNAME = "inputUsername"
  const val USERNAME_SUGGESTION = "usernameSuggestion"
  const val NO_RESULTS_MESSAGE = "noResultsMessage"
  const val BACK_BUTTON = "backButton"
}

@Composable
fun UserSelectionField(
    usernameText: String,
    onUsernameTextChanged: (String) -> Unit,
    onUserSuggestionClicked: (String) -> Unit,
    usernameSuggestions: List<User>,
    expanded: Boolean,
    onBackPressed: () -> Unit = {}
) {
  Column(modifier = Modifier.fillMaxSize()) {
    // Search bar
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically) {
          BackArrow(
              onBackClick = onBackPressed,
              modifier = Modifier.testTag(UserSelectionFieldTestTags.BACK_BUTTON))

          Spacer(modifier = Modifier.width(8.dp))

          OutlinedTextField(
              value = usernameText,
              onValueChange = { onUsernameTextChanged(it) },
              placeholder = {
                Text("Username", color = OnSurfaceVariant, style = Typography.bodyMedium)
              },
              trailingIcon = {
                if (usernameText.isNotEmpty()) {
                  IconButton(onClick = { onUsernameTextChanged("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = OnSurfaceVariant)
                  }
                }
              },
              colors =
                  OutlinedTextFieldDefaults.colors(
                      focusedBorderColor = Primary,
                      unfocusedBorderColor = Secondary,
                      focusedContainerColor = Background,
                      unfocusedContainerColor = Background),
              shape = RoundedCornerShape(24.dp),
              modifier =
                  Modifier.weight(1f)
                      .height(56.dp)
                      .testTag(UserSelectionFieldTestTags.INPUT_USERNAME),
              singleLine = true)
        }

    // Results list
    if (expanded && usernameText.isNotEmpty()) { // Added check for non-empty text
      if (usernameSuggestions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text(
              "No users found",
              color = OnSurfaceVariant,
              style = Typography.bodyMedium,
              modifier = Modifier.testTag(UserSelectionFieldTestTags.NO_RESULTS_MESSAGE))
        }
      } else {
        Column(modifier = Modifier.fillMaxWidth()) {
          usernameSuggestions.forEach { user ->
            UserSuggestionItem(
                user = user,
                onClick = { onUserSuggestionClicked(user.uid) },
                modifier = Modifier.testTag(UserSelectionFieldTestTags.USERNAME_SUGGESTION))
          }
        }
      }
    }
  }
}

@Composable
fun UserSuggestionItem(user: User, onClick: () -> Unit, modifier: Modifier = Modifier) {
  Row(
      modifier =
          modifier
              .fillMaxWidth()
              .clickable(onClick = onClick)
              .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically) {
        // Profile image
        ProfilePicture(
            size = 48.dp,
            profilePicture = user.profilePicture,
            username = user.username,
            textStyle = Typography.bodyMedium)

        Spacer(modifier = Modifier.width(12.dp))

        // Username and post count
        Column {
          Text(
              text = user.username,
              style = Typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
              color = MaterialTheme.colorScheme.onSurface)
        }
      }
}
