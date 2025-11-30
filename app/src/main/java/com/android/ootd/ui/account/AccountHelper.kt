package com.android.ootd.ui.account

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.android.ootd.ui.camera.CameraScreen

/**
 * Composable function that provides profile picture editing functionality.
 *
 * @param viewModel The AccountViewModel to handle image uploads and profile updates
 * @param context The context for showing Toast messages
 * @param showImageSourceDialog Whether to show the image source selection dialog (camera or
 *   gallery)
 * @param onShowImageSourceDialogChange Callback to update the showImageSourceDialog state
 */
@Composable
fun ProfilePictureEditor(
    viewModel: AccountViewModel,
    context: Context,
    showImageSourceDialog: Boolean,
    onShowImageSourceDialogChange: (Boolean) -> Unit
) {
  // State for camera view
  var showCamera by remember { mutableStateOf(false) }

  val imagePickerLauncher =
      rememberLauncherForActivityResult(
          contract = ActivityResultContracts.PickVisualMedia(),
          onResult = { uri ->
            uri?.let {
              handlePickedProfileImage(
                  it.toString(),
                  upload = viewModel::uploadImageToStorage,
                  editProfilePicture = { url -> viewModel.editUser(profilePicture = url) },
                  context = context)
            }
          })

  if (showCamera) {
    CameraScreen(
        onImageCaptured = { uri ->
          handlePickedProfileImage(
              uri.toString(),
              upload = viewModel::uploadImageToStorage,
              editProfilePicture = { url -> viewModel.editUser(profilePicture = url) },
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
