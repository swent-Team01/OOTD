package com.android.ootd.ui.account

/*
 * DISCLAIMER: This file was created/modified with the assistance of GitHub Copilot.
 * Copilot provided suggestions which were reviewed and adapted by the developer.
 */
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.ootd.model.map.emptyLocation
import com.android.ootd.ui.map.LocationSelectionSection
import com.android.ootd.ui.register.RegisterScreenTestTags
import com.android.ootd.ui.theme.OOTDTheme
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Secondary

// Test tag constants for UI tests
object UiTestTags {
  const val TAG_ACCOUNT_BACK = "account_back"
  const val TAG_ACCOUNT_TITLE = "account_title"
  const val TAG_ACCOUNT_AVATAR_CONTAINER = "account_avatar_container"
  const val TAG_ACCOUNT_AVATAR_IMAGE = "account_avatar_image"
  const val TAG_ACCOUNT_AVATAR_LETTER = "account_avatar_letter"
  const val TAG_ACCOUNT_EDIT = "account_edit_button"
  const val TAG_ACCOUNT_DELETE = "account_delete_button"
  const val TAG_USERNAME_FIELD = "account_username_field"
  const val TAG_USERNAME_EDIT = "account_username_edit"
  const val TAG_USERNAME_CANCEL = "account_username_cancel"
  const val TAG_USERNAME_SAVE = "account_username_save"
  const val TAG_GOOGLE_FIELD = "account_google_field"
  const val TAG_SIGNOUT_BUTTON = "account_signout_button"
  const val TAG_PRIVACY_TOGGLE = "account_privacy_toggle"
  const val TAG_ACCOUNT_LOADING = "account_loading"
  const val TAG_PRIVACY_HELP_ICON = "account_privacy_help_icon"
  const val TAG_PRIVACY_HELP_MENU = "account_privacy_help_menu"
}

/**
 * Account screen UI.
 *
 * Shows the current account information (username, Google email, profile picture) and provides
 * actions to edit the avatar, go back, and sign out.
 *
 * @param accountViewModel supplies [AccountViewState] and handles business logic.
 * @param credentialManager used when signing out to clear platform credentials.
 * @param onBack callback invoked when the back button is pressed.
 * @param onSignOut callback invoked when the view model signals a successful sign-out.
 */
@Composable
fun AccountScreen(
    accountViewModel: AccountViewModel = viewModel(),
    credentialManager: CredentialManager = CredentialManager.create(LocalContext.current),
    onBack: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
  val uiState by accountViewModel.uiState.collectAsState()
  val context = LocalContext.current

  LaunchedEffect(uiState.signedOut) {
    if (uiState.signedOut) {
      onSignOut()
      Toast.makeText(context, "Logout successful", Toast.LENGTH_SHORT).show()
    }
  }

  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let { message ->
      Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
      accountViewModel.clearErrorMsg()
    }
  }

  AccountScreenContent(
      accountViewModel = accountViewModel,
      uiState = uiState,
      onBack = onBack,
      onSignOutClick = { accountViewModel.signOut(credentialManager) },
      onToggle = { accountViewModel.onTogglePrivacy() },
      onHelpClick = { accountViewModel.onPrivacyHelpClick() },
      onHelpDismiss = { accountViewModel.onPrivacyHelpDismiss() })
}

@Composable
private fun AccountScreenContent(
    accountViewModel: AccountViewModel = viewModel(),
    uiState: AccountViewState,
    onBack: () -> Unit,
    onSignOutClick: () -> Unit,
    onToggle: () -> Unit,
    onHelpClick: () -> Unit,
    onHelpDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val scrollState = rememberScrollState()
  // Max width for centered content (keeps avatar and inputs aligned)
  val contentMaxWidth = 560.dp
  val contentModifier = Modifier.fillMaxWidth().widthIn(max = contentMaxWidth)

  // State for username editing
  var isEditingUsername by remember { mutableStateOf(false) }
  var editedUsername by remember { mutableStateOf("") }

  // Image picker launcher
  val imagePickerLauncher =
      rememberLauncherForActivityResult(
          contract = ActivityResultContracts.PickVisualMedia(),
          onResult = { uri ->
            uri?.let {
              handlePickedProfileImage(
                  it.toString(),
                  upload = accountViewModel::uploadImageToStorage,
                  editProfilePicture = { accountViewModel.editUser(profilePicture = it) },
                  context = context)
            }
          })

  Column(
      modifier =
          Modifier.fillMaxSize()
              .verticalScroll(scrollState)
              .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 72.dp),
      horizontalAlignment = Alignment.CenterHorizontally) {
        BackButton(onBack = onBack)

        AccountTitle()

        Box(modifier = contentModifier) {
          AvatarSection(
              avatarUri = uiState.profilePicture,
              username = uiState.username,
              onEditClick = {
                imagePickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
              },
              accountViewModel,
              context)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = contentModifier) {
          UsernameField(
              username = uiState.username,
              isEditing = isEditingUsername,
              editedValue = editedUsername,
              onValueChange = { editedUsername = it },
              onEditClick = {
                isEditingUsername = true
                editedUsername = uiState.username
              },
              onCancelClick = {
                isEditingUsername = false
                editedUsername = ""
              },
              onSaveClick = {
                if (editedUsername.isNotBlank() && editedUsername != uiState.username) {
                  accountViewModel.editUser(newUsername = editedUsername)
                  isEditingUsername = false
                }
              })
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = contentModifier) { GoogleAccountField(email = uiState.googleAccountName) }

        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = contentModifier) {
          LocationField(uiState = uiState, viewModel = accountViewModel)
        }

        Box(modifier = contentModifier) {
          PrivacyToggleRow(
              isPrivate = uiState.isPrivate,
              onToggle = onToggle,
              showPrivacyHelp = uiState.showPrivacyHelp,
              onHelpClick = onHelpClick,
              onHelpDismiss = onHelpDismiss,
              modifier =
                  Modifier.fillMaxWidth()
                      .padding(top = 4.dp)
                      .testTag(UiTestTags.TAG_PRIVACY_TOGGLE))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(modifier = contentModifier, contentAlignment = Alignment.Center) {
          SignOutButton(onClick = onSignOutClick)
        }
      }

  if (uiState.isLoading) {
    LoadingOverlay()
  }
}

@Composable
private fun LocationField(uiState: AccountViewState, viewModel: AccountViewModel) {
  val hasLocationError =
      uiState.locationFieldTouched &&
          uiState.locationFieldLeft &&
          uiState.locationQuery.isNotEmpty() &&
          (uiState.location.name != uiState.locationQuery || uiState.location == emptyLocation)

  LocationSelectionSection(
      textGPSButton = "Update Location (GPS)",
      textLocationField = "Location",
      locationQuery = uiState.locationQuery,
      selectedLocation = uiState.location,
      suggestions = uiState.locationSuggestions,
      isLoadingLocation = uiState.isLoadingLocations,
      onLocationQueryChange = viewModel::setLocationQuery,
      onLocationSelect = viewModel::setLocation,
      onGPSClick = { /* TODO: Implement GPS functionality */ },
      onClearSuggestions = viewModel::clearLocationSuggestions,
      isError = hasLocationError,
      onFocusChanged = viewModel::onLocationFieldFocusChanged,
      modifier =
          Modifier.padding(vertical = 8.dp).testTag(RegisterScreenTestTags.INPUT_REGISTER_LOCATION))
}

@Composable
private fun BackButton(onBack: () -> Unit) {
  val colors = MaterialTheme.colorScheme
  Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
    IconButton(onClick = onBack, modifier = Modifier.testTag(UiTestTags.TAG_ACCOUNT_BACK)) {
      Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back",
          tint = colors.onBackground,
          modifier = Modifier.size(48.dp))
    }
  }
}

@Composable
private fun AccountTitle() {
  val colors = MaterialTheme.colorScheme
  val typography = MaterialTheme.typography
  Text(
      text = "My Account",
      style = typography.displayMedium,
      color = colors.primary,
      textAlign = TextAlign.Center,
      modifier =
          Modifier.fillMaxWidth()
              .padding(top = 4.dp, bottom = 12.dp)
              .testTag(UiTestTags.TAG_ACCOUNT_TITLE))
}

@Composable
private fun AvatarSection(
    avatarUri: String,
    username: String,
    onEditClick: () -> Unit,
    accountViewModel: AccountViewModel,
    context: Context = LocalContext.current,
    modifier: Modifier = Modifier
) {
  val colors = MaterialTheme.colorScheme
  val typography = MaterialTheme.typography
  val defaultAvatarPainter = rememberVectorPainter(Icons.Default.AccountCircle)

  Column(
      modifier = modifier.testTag(UiTestTags.TAG_ACCOUNT_AVATAR_CONTAINER).fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally) {
        // Avatar image/letter
        if (avatarUri.isNotBlank()) {
          AsyncImage(
              model = avatarUri,
              contentDescription = "Avatar",
              placeholder = defaultAvatarPainter,
              error = defaultAvatarPainter,
              contentScale = ContentScale.Crop,
              modifier =
                  Modifier.size(120.dp)
                      .clip(CircleShape)
                      .testTag(UiTestTags.TAG_ACCOUNT_AVATAR_IMAGE))
        } else {
          Box(
              modifier =
                  Modifier.size(120.dp)
                      .clip(CircleShape)
                      .background(Primary)
                      .pointerHoverIcon(icon = PointerIcon.Hand),
              contentAlignment = Alignment.Center) {
                Text(
                    text = username.firstOrNull()?.uppercase() ?: "",
                    style = typography.headlineMedium,
                    color = Secondary,
                    modifier = Modifier.testTag(UiTestTags.TAG_ACCOUNT_AVATAR_LETTER))
              }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val editProfilePicture = if (avatarUri.isNotBlank()) "Edit" else "Upload"

        // Edit and Delete buttons under avatar
        Row(
            modifier = Modifier.wrapContentWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically) {
              Button(
                  onClick = onEditClick,
                  shape = CircleShape,
                  colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                  modifier = Modifier.testTag(UiTestTags.TAG_ACCOUNT_EDIT)) {
                    Text(
                        text = editProfilePicture,
                        color = colors.onPrimary,
                        style = typography.titleMedium)
                  }

              // Delete button - only show if user has a profile picture
              if (avatarUri.isNotBlank()) {
                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = {
                      accountViewModel.deleteProfilePicture()
                      Toast.makeText(context, "Profile picture removed", Toast.LENGTH_SHORT).show()
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.tertiary),
                    modifier = Modifier.testTag(UiTestTags.TAG_ACCOUNT_DELETE)) {
                      Text(text = "Delete", color = colors.onError, style = typography.titleMedium)
                    }
              }
            }
      }
}

@Composable
private fun UsernameField(
    username: String,
    isEditing: Boolean,
    editedValue: String,
    onValueChange: (String) -> Unit,
    onEditClick: () -> Unit,
    onCancelClick: () -> Unit,
    onSaveClick: () -> Unit
) {
  val colors = MaterialTheme.colorScheme
  val typography = MaterialTheme.typography

  OutlinedTextField(
      value = if (isEditing) editedValue else username,
      onValueChange = onValueChange,
      label = {
        Box(
            modifier =
                Modifier.background(colors.secondary, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)) {
              Text(text = "Username", style = typography.bodySmall, color = colors.tertiary)
            }
      },
      readOnly = !isEditing,
      textStyle = typography.bodyLarge,
      trailingIcon = {
        if (isEditing) {
          UsernameEditActions(onCancelClick = onCancelClick, onSaveClick = onSaveClick)
        } else {
          UsernameEditButton(onClick = onEditClick)
        }
      },
      colors =
          OutlinedTextFieldDefaults.colors(
              focusedTextColor = colors.primary,
              unfocusedTextColor = colors.primary,
          ),
      modifier = Modifier.fillMaxWidth().testTag(UiTestTags.TAG_USERNAME_FIELD))
}

@Composable
private fun UsernameEditActions(onCancelClick: () -> Unit, onSaveClick: () -> Unit) {
  val colors = MaterialTheme.colorScheme
  Row {
    IconButton(
        onClick = onCancelClick, modifier = Modifier.testTag(UiTestTags.TAG_USERNAME_CANCEL)) {
          Icon(
              imageVector = Icons.Default.Close, contentDescription = "Cancel", tint = colors.error)
        }
    IconButton(onClick = onSaveClick, modifier = Modifier.testTag(UiTestTags.TAG_USERNAME_SAVE)) {
      Icon(imageVector = Icons.Default.Check, contentDescription = "Save", tint = colors.primary)
    }
  }
}

@Composable
private fun UsernameEditButton(onClick: () -> Unit) {
  val colors = MaterialTheme.colorScheme
  IconButton(onClick = onClick, modifier = Modifier.testTag(UiTestTags.TAG_USERNAME_EDIT)) {
    Icon(
        imageVector = Icons.Default.Edit,
        contentDescription = "Edit username",
        tint = colors.onSurface.copy(alpha = 0.7f))
  }
}

@Composable
private fun GoogleAccountField(email: String) {
  val colors = MaterialTheme.colorScheme
  val typography = MaterialTheme.typography

  OutlinedTextField(
      value = email,
      onValueChange = {},
      label = {
        Box(
            modifier =
                Modifier.background(colors.secondary, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)) {
              Text(text = "Google Account", style = typography.bodySmall, color = colors.tertiary)
            }
      },
      readOnly = true,
      textStyle = typography.bodyLarge,
      colors =
          OutlinedTextFieldDefaults.colors(
              focusedTextColor = colors.primary,
              unfocusedTextColor = colors.primary,
          ),
      modifier = Modifier.fillMaxWidth().testTag(UiTestTags.TAG_GOOGLE_FIELD))
}

@Composable
private fun SignOutButton(onClick: () -> Unit) {
  val colors = MaterialTheme.colorScheme
  val typography = MaterialTheme.typography

  Box(
      modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
      contentAlignment = Alignment.Center) {
        Button(
            onClick = onClick,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
            modifier = Modifier.testTag(UiTestTags.TAG_SIGNOUT_BUTTON)) {
              Text(text = "Sign Out", color = colors.onPrimary, style = typography.titleLarge)
            }
      }
}

@Composable
private fun LoadingOverlay() {
  val colors = MaterialTheme.colorScheme
  Box(
      modifier = Modifier.fillMaxSize().background(colors.onBackground.copy(alpha = 0.12f)),
      contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier.testTag(UiTestTags.TAG_ACCOUNT_LOADING), color = colors.primary)
      }
}

@Composable
private fun PrivacyToggleRow(
    isPrivate: Boolean,
    onToggle: () -> Unit,
    showPrivacyHelp: Boolean,
    onHelpClick: () -> Unit,
    onHelpDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
  val colors = MaterialTheme.colorScheme
  val typography = MaterialTheme.typography

  Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
          text = "Account Privacy",
          style = typography.titleLarge,
          color = colors.primary,
          modifier = Modifier.padding(start = 8.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Box {
        IconButton(
            onClick = onHelpClick, modifier = Modifier.testTag(UiTestTags.TAG_PRIVACY_HELP_ICON)) {
              Icon(imageVector = Icons.Outlined.Info, contentDescription = "Privacy help")
            }
        DropdownMenu(expanded = showPrivacyHelp, onDismissRequest = onHelpDismiss) {
          DropdownMenuItem(
              modifier = Modifier.testTag(UiTestTags.TAG_PRIVACY_HELP_MENU),
              text = {
                Text(
                    "Private: only you and mutual friends can view your posts. Public: everyone can view.",
                    style = typography.bodySmall,
                    color = colors.onSurface)
              },
              onClick = onHelpDismiss)
        }
      }
    }

    Spacer(modifier = Modifier.weight(1f))

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
          text = if (isPrivate) "Private" else "Public",
          style = typography.bodySmall,
          color = colors.onSurface.copy(alpha = 0.65f))
      Switch(
          checked = isPrivate,
          onCheckedChange = { onToggle() },
          colors =
              SwitchDefaults.colors(
                  checkedThumbColor = colors.onPrimary,
                  checkedTrackColor = colors.primary,
                  uncheckedThumbColor = colors.onPrimary,
                  uncheckedTrackColor = colors.outlineVariant))
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun AccountScreenPreview() {
  OOTDTheme {
    AccountScreenContent(
        uiState =
            AccountViewState(
                username = "Jane Doe",
                googleAccountName = "jane@google.com",
                profilePicture = "",
                isPrivate = false,
                showPrivacyHelp = true,
            ),
        onBack = {},
        onSignOutClick = {},
        onToggle = {},
        onHelpClick = {},
        onHelpDismiss = {},
    )
  }
}

/**
 * Helper function to handle the result of the image picker.
 *
 * @param localPath The local file path of the picked image.
 * @param upload The upload function to call with the image path.
 * @param editProfilePicture The function to call to update the profile picture URL.
 * @param context The context to use for showing Toast messages.
 */
@VisibleForTesting
internal fun handlePickedProfileImage(
    localPath: String,
    upload: (String, (String) -> Unit, (Throwable) -> Unit) -> Unit,
    editProfilePicture: (String) -> Unit,
    context: Context
) {
  if (localPath.isBlank()) return
  upload(
      localPath,
      { downloadedUrl ->
        editProfilePicture(downloadedUrl)
        Toast.makeText(context, "Uploading profile picture...", Toast.LENGTH_SHORT).show()
      },
      { error ->
        Toast.makeText(
                context, error.localizedMessage ?: "Failed to upload image", Toast.LENGTH_SHORT)
            .show()
      })
}
