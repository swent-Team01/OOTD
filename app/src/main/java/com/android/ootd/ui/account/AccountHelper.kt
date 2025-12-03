package com.android.ootd.ui.account

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.android.ootd.ui.camera.CameraScreen
import com.android.ootd.ui.theme.Bodoni
import com.android.ootd.ui.theme.LightColorScheme
import com.android.ootd.ui.theme.Typography
import com.android.ootd.utils.composables.ActionButton
import com.android.ootd.utils.composables.ProfilePicture

/**
 * Handles the result from the image picker by processing the URI and uploading the image.
 *
 * @param uri The URI returned from the image picker, or null if no image was selected
 * @param upload The upload function from the ViewModel
 * @param editProfilePicture The function to update the profile picture in the ViewModel
 * @param context The context for showing Toast messages
 */
internal fun handleImagePickerResult(
    uri: Uri?,
    upload: (String, (String) -> Unit, (Throwable) -> Unit) -> Unit,
    editProfilePicture: (String) -> Unit,
    context: Context
) {
  uri?.let {
    handlePickedProfileImage(
        it.toString(), upload = upload, editProfilePicture = editProfilePicture, context = context)
  }
}

/**
 * Composable function that provides profile picture editing functionality.
 *
 * @param context The context for showing Toast messages
 * @param uploadProfilePicture Function to upload the profile picture to storage. Takes the image
 *   URI string, a success callback with the download URL, and an error callback
 * @param editProfilePicture Function to update the profile picture URL in the ViewModel
 * @param showImageSourceDialog Whether to show the image source selection dialog (camera or
 *   gallery)
 * @param onShowImageSourceDialogChange Callback to update the showImageSourceDialog state
 */
@Composable
fun ProfilePictureEditor(
    context: Context,
    uploadProfilePicture: (String, (String) -> Unit, (Throwable) -> Unit) -> Unit = { _, _, _ -> },
    editProfilePicture: (String) -> Unit = {},
    showImageSourceDialog: Boolean,
    onShowImageSourceDialogChange: (Boolean) -> Unit
) {
  // State for camera view
  var showCamera by remember { mutableStateOf(false) }

  val imagePickerLauncher =
      rememberLauncherForActivityResult(
          contract = ActivityResultContracts.PickVisualMedia(),
          onResult = { uri ->
            handleImagePickerResult(
                uri,
                upload = uploadProfilePicture,
                editProfilePicture = { url -> editProfilePicture(url) },
                context = context)
          })

  if (showCamera) {
    CameraScreen(
        onImageCaptured = { uri ->
          handlePickedProfileImage(
              uri.toString(),
              upload = uploadProfilePicture,
              editProfilePicture = { url -> editProfilePicture(url) },
              context = context)
          showCamera = false
        },
        onDismiss = { showCamera = false })
  }

  if (showImageSourceDialog) {
    AlertDialog(
        onDismissRequest = { onShowImageSourceDialogChange(false) },
        title = { Text(text = "Select Image") },
        text = {
          Column {
            TextButton(
                onClick = {
                  onShowImageSourceDialogChange(false)
                  showCamera = true
                }) {
                  Text("Take a Photo")
                }
            TextButton(
                onClick = {
                  onShowImageSourceDialogChange(false)
                  imagePickerLauncher.launch(
                      PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) {
                  Text("Choose from Gallery")
                }
          }
        },
        confirmButton = {},
        dismissButton = {})
  }
}

@Composable
fun AvatarSection(
    avatarUri: String,
    username: String,
    onEditClick: () -> Unit,
    deleteProfilePicture: () -> Unit,
    modifier: Modifier = Modifier,
    context: Context = LocalContext.current
) {
    val colors = LightColorScheme
    val typography = Typography

    Column(
        modifier = modifier
            .testTag(UiTestTags.TAG_ACCOUNT_AVATAR_CONTAINER)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally) {
        // Avatar image/letter
        val tag =
            UiTestTags.TAG_ACCOUNT_AVATAR_IMAGE.takeIf { avatarUri.isNotBlank() }
                ?: UiTestTags.TAG_ACCOUNT_AVATAR_LETTER
        ProfilePicture(
            Modifier.testTag(tag),
            120.dp,
            avatarUri,
            username,
            typography.headlineMedium.copy(fontFamily = Bodoni))

        Spacer(modifier = Modifier.height(8.dp))

        val editProfilePicture = if (avatarUri.isNotBlank()) "Edit" else "Upload"

        // Edit and Delete buttons under avatar
        Row(
            modifier = Modifier.wrapContentWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically) {
            ActionButton(
                onButtonClick = onEditClick,
                modifier = Modifier.testTag(UiTestTags.TAG_ACCOUNT_EDIT),
                buttonText = editProfilePicture)

            // Delete button - only show if user has a profile picture
            if (avatarUri.isNotBlank()) {
                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = {
                        deleteProfilePicture()
                        Toast.makeText(context, "Profile picture removed", Toast.LENGTH_SHORT).show()
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.tertiary),
                    modifier = Modifier.testTag(UiTestTags.TAG_ACCOUNT_DELETE)) {
                    Text(
                        text = "Delete",
                        color = colors.onError,
                        style = typography.titleMedium.copy(fontFamily = Bodoni))
                }
            }
        }
    }
}
