package com.android.ootd.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.android.ootd.model.user.User
import com.android.ootd.ui.theme.Background
import com.android.ootd.ui.theme.OnSurfaceVariant
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Tertiary
import com.android.ootd.ui.theme.Typography
import com.android.ootd.utils.composables.OOTDTopBar
import com.android.ootd.utils.composables.ProfilePicture

object UserSelectionFieldTestTags {
  const val INPUT_USERNAME = "inputUsername"
  const val USERNAME_SUGGESTION = "usernameSuggestion"
  const val NO_RESULTS_MESSAGE = "noResultsMessage"
  const val USERS_CLOSE_TO_YOU = "usersCloseToYou"
  const val FIND_FRIENDS_TITLE = "findFriendsTitle"
  const val NO_RESULTS_ICON = "noResultsIcon"
  const val EMPTY_SEARCH_STATE = "emptySearchState"
}

@Composable
fun UserSelectionField(
    usernameText: String,
    onUsernameTextChanged: (String) -> Unit,
    onUserSuggestionClicked: (String) -> Unit,
    usernameSuggestions: List<User>,
    expanded: Boolean,
    onFindFriendsClick: () -> Unit = {}
) {
  Scaffold(
      topBar = {
        OOTDTopBar(
            centerText = "Find Friends",
            textModifier = Modifier.testTag(UserSelectionFieldTestTags.FIND_FRIENDS_TITLE))
      }) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
          UserSearchInput(
              usernameText = usernameText, onUsernameTextChanged = onUsernameTextChanged)

          // Results list
          Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (expanded && usernameText.isNotEmpty()) {
              if (usernameSuggestions.isEmpty()) {
                NoUsersFoundView(modifier = Modifier.fillMaxWidth().align(Alignment.Center))
              } else {
                UserSuggestionsList(
                    usernameSuggestions = usernameSuggestions,
                    onUserSuggestionClicked = onUserSuggestionClicked)
              }
            } else if (usernameText.isEmpty()) {
              EmptySearchStateView(
                  onFindFriendsClick = onFindFriendsClick,
                  modifier = Modifier.align(Alignment.Center))
            }
          }
        }
      }
}

@Composable
private fun UserSearchInput(usernameText: String, onUsernameTextChanged: (String) -> Unit) {
  OutlinedTextField(
      value = usernameText,
      onValueChange = { onUsernameTextChanged(it) },
      placeholder = { Text("Username", color = OnSurfaceVariant, style = Typography.bodyMedium) },
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
              unfocusedBorderColor = Tertiary,
              focusedContainerColor = Background,
              unfocusedContainerColor = Background),
      shape = RoundedCornerShape(24.dp),
      modifier =
          Modifier.fillMaxWidth()
              .padding(16.dp)
              .height(56.dp)
              .testTag(UserSelectionFieldTestTags.INPUT_USERNAME),
      singleLine = true)
}

@Composable
private fun NoUsersFoundView(modifier: Modifier = Modifier) {
  Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    Icon(
        imageVector = Icons.Default.Person,
        contentDescription = "No result",
        tint = Tertiary,
        modifier = Modifier.size(64.dp).testTag(UserSelectionFieldTestTags.NO_RESULTS_ICON))
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        "No users found",
        color = Tertiary,
        style = Typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.testTag(UserSelectionFieldTestTags.NO_RESULTS_MESSAGE))
  }
}

@Composable
private fun UserSuggestionsList(
    usernameSuggestions: List<User>,
    onUserSuggestionClicked: (String) -> Unit
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    usernameSuggestions.forEach { user ->
      UserSuggestionItem(
          user = user,
          onClick = { onUserSuggestionClicked(user.uid) },
          modifier = Modifier.testTag(UserSelectionFieldTestTags.USERNAME_SUGGESTION))
    }
  }
}

@Composable
private fun EmptySearchStateView(onFindFriendsClick: () -> Unit, modifier: Modifier = Modifier) {
  Column(
      modifier = modifier.testTag(UserSelectionFieldTestTags.EMPTY_SEARCH_STATE),
      horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Default.Groups,
            contentDescription = "Find Friends",
            tint = Tertiary,
            modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Search for your fit buddies",
            textAlign = TextAlign.Center,
            color = Tertiary,
            style = Typography.bodyLarge)
        Text(
            text = "Or click here to find them on the map",
            textAlign = TextAlign.Center,
            color = Tertiary,
            style = Typography.bodySmall.copy(textDecoration = TextDecoration.Underline),
            modifier =
                Modifier.clickable { onFindFriendsClick() }
                    .testTag(UserSelectionFieldTestTags.USERS_CLOSE_TO_YOU))
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
