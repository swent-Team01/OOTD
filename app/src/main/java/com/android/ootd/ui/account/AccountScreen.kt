package com.android.ootd.ui.account
/*
 * DISCLAIMER: This file was created/modified with the assistance of GitHub Copilot.
 * Copilot provided suggestions which were reviewed and adapted by the developer.
 */
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.ootd.ui.theme.LightColorScheme
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Secondary
import com.android.ootd.ui.theme.Typography

// Test tag constants for UI tests
object UiTestTags {
  const val TAG_ACCOUNT_BACK = "account_back"
  const val TAG_ACCOUNT_TITLE = "account_title"
  const val TAG_ACCOUNT_AVATAR_CONTAINER = "account_avatar_container"
  const val TAG_ACCOUNT_AVATAR_IMAGE = "account_avatar_image"
  const val TAG_ACCOUNT_AVATAR_LETTER = "account_avatar_letter"
  const val TAG_ACCOUNT_EDIT = "account_edit_button"
  const val TAG_USERNAME_FIELD = "account_username_field"
  const val TAG_USERNAME_EDIT = "account_username_edit"
  const val TAG_USERNAME_CANCEL = "account_username_cancel"
  const val TAG_USERNAME_SAVE = "account_username_save"
  const val TAG_GOOGLE_FIELD = "account_google_field"
  const val TAG_SIGNOUT_BUTTON = "account_signout_button"
  const val TAG_ACCOUNT_LOADING = "account_loading"
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
  val scrollState = rememberScrollState()
  val colors = LightColorScheme
  val typography = Typography
  val context = LocalContext.current
  val uiState by accountViewModel.uiState.collectAsState()
  val username = uiState.username
  val email = uiState.googleAccountName
  val dateOfBirth = uiState.dateOfBirth
  val avatarUri = uiState.profilePicture

  // State for username editing
  var isEditingUsername by remember { mutableStateOf(false) }
  var editedUsername by remember { mutableStateOf("") }

  // Image picker launcher
  val imagePickerLauncher =
      rememberLauncherForActivityResult(
          contract = ActivityResultContracts.PickVisualMedia(),
          onResult = { uri ->
            uri?.let {
              accountViewModel.uploadProfilePicture(it.toString())
              Toast.makeText(context, "Uploading profile picture...", Toast.LENGTH_SHORT).show()
            }
          })

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

  val defaultAvatarPainter = rememberVectorPainter(Icons.Default.AccountCircle)

  Column(
      modifier =
          Modifier.fillMaxSize()
              .verticalScroll(scrollState)
              .padding(horizontal = 24.dp, vertical = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
          IconButton(onClick = onBack, modifier = Modifier.testTag(UiTestTags.TAG_ACCOUNT_BACK)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = colors.onBackground,
                modifier = Modifier.size(48.dp))
          }
        }

        Text(
            text = "My Account",
            style = typography.displayLarge,
            color = colors.primary,
            textAlign = TextAlign.Center,
            modifier =
                Modifier.fillMaxWidth()
                    .padding(top = 4.dp, bottom = 20.dp)
                    .testTag(UiTestTags.TAG_ACCOUNT_TITLE))

        // Avatar area
        Box(
            modifier = Modifier.fillMaxWidth().testTag(UiTestTags.TAG_ACCOUNT_AVATAR_CONTAINER),
            contentAlignment = Alignment.Center) {
              if (avatarUri.isNotBlank()) {
                AsyncImage(
                    model = avatarUri,
                    contentDescription = "Avatar",
                    placeholder = defaultAvatarPainter,
                    error = defaultAvatarPainter,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier.size(180.dp)
                            .clip(CircleShape)
                            .testTag(UiTestTags.TAG_ACCOUNT_AVATAR_IMAGE))
              } else {
                Box(
                    modifier =
                        Modifier.size(180.dp)
                            .clip(CircleShape)
                            .background(Primary)
                            .pointerHoverIcon(icon = PointerIcon.Hand),
                    contentAlignment = Alignment.Center) {
                      Text(
                          text = username.firstOrNull()?.uppercase() ?: "",
                          style = typography.headlineLarge,
                          color = Secondary,
                          modifier = Modifier.testTag(UiTestTags.TAG_ACCOUNT_AVATAR_LETTER))
                    }
              }
            }

        Spacer(modifier = Modifier.height(12.dp))

        // Edit button under avatar
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
          Button(
              onClick = {
                imagePickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
              },
              shape = CircleShape,
              colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
              modifier = Modifier.testTag(UiTestTags.TAG_ACCOUNT_EDIT)) {
                Text(text = "Edit", color = colors.onPrimary, style = typography.titleLarge)
              }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Username field (outlined) with colored rounded label
        OutlinedTextField(
            value = if (isEditingUsername) editedUsername else username,
            onValueChange = { editedUsername = it },
            label = {
              Box(
                  modifier =
                      Modifier.background(colors.secondary, RoundedCornerShape(4.dp))
                          .padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(text = "Username", style = typography.bodySmall, color = colors.tertiary)
                  }
            },
            readOnly = !isEditingUsername,
            textStyle = typography.bodyLarge,
            trailingIcon = {
              if (isEditingUsername) {
                Row {
                  IconButton(
                      onClick = {
                        isEditingUsername = false
                        editedUsername = ""
                      },
                      modifier = Modifier.testTag(UiTestTags.TAG_USERNAME_CANCEL)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = colors.error)
                      }
                  IconButton(
                      onClick = {
                        if (editedUsername.isNotBlank() && editedUsername != username) {
                          accountViewModel.editUser(newUsername = editedUsername)
                          isEditingUsername = false
                        }
                      },
                      modifier = Modifier.testTag(UiTestTags.TAG_USERNAME_SAVE)) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save",
                            tint = colors.primary)
                      }
                }
              } else {
                IconButton(
                    onClick = {
                      isEditingUsername = true
                      editedUsername = username
                    },
                    modifier = Modifier.testTag(UiTestTags.TAG_USERNAME_EDIT)) {
                      Icon(
                          imageVector = Icons.Default.Edit,
                          contentDescription = "Edit username",
                          tint = colors.onSurface.copy(alpha = 0.7f))
                    }
              }
            },
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.primary,
                    unfocusedTextColor = colors.primary,
                ),
            modifier = Modifier.fillMaxWidth().testTag(UiTestTags.TAG_USERNAME_FIELD))

        Spacer(modifier = Modifier.height(18.dp))

        // Google Account field (outlined)
        OutlinedTextField(
            value = email,
            onValueChange = {},
            label = {
              Box(
                  modifier =
                      Modifier.background(colors.secondary, RoundedCornerShape(6.dp))
                          .padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(
                        text = "Google Account",
                        style = typography.bodySmall,
                        color = colors.tertiary)
                  }
            },
            readOnly = true,
            textStyle = typography.bodyLarge, // 16/24 for input text
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.primary,
                    unfocusedTextColor = colors.primary,
                ),
            modifier = Modifier.fillMaxWidth().testTag(UiTestTags.TAG_GOOGLE_FIELD))

        Spacer(modifier = Modifier.weight(1f))

        // Sign out button at bottom center
        Box(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            contentAlignment = Alignment.Center) {
              Button(
                  onClick = { accountViewModel.signOut(credentialManager) },
                  shape = CircleShape,
                  colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                  modifier = Modifier.testTag(UiTestTags.TAG_SIGNOUT_BUTTON)) {
                    Text(text = "Sign Out", color = colors.onPrimary, style = typography.titleLarge)
                  }
            }
      }

  // Loading indicator (circular progress) in the center of the screen
  if (uiState.isLoading) {
    Box(
        modifier = Modifier.fillMaxSize().background(colors.onBackground.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center) {
          CircularProgressIndicator(
              modifier = Modifier.testTag(UiTestTags.TAG_ACCOUNT_LOADING), color = colors.primary)
        }
  }
}
