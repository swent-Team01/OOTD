package com.android.ootd.utils.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

@Composable
fun ImageSelectionDialog(
    onDismissRequest: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickFromGallery: () -> Unit,
    modifier: Modifier = Modifier,
    takePhotoTag: String = "takePhoto",
    pickGalleryTag: String = "pickGallery"
) {
  AlertDialog(
      modifier = modifier,
      onDismissRequest = onDismissRequest,
      title = { Text(text = "Select Image") },
      text = {
        Column {
          TextButton(onClick = onTakePhoto, modifier = Modifier.testTag(takePhotoTag)) {
            Text("Take a Photo")
          }
          TextButton(onClick = onPickFromGallery, modifier = Modifier.testTag(pickGalleryTag)) {
            Text("Choose from Gallery")
          }
        }
      },
      confirmButton = {},
      dismissButton = {})
}

@Composable
fun ProfilePictureConfirmDialogs(
    showEdit: Boolean,
    showDelete: Boolean,
    onDismissEdit: () -> Unit,
    onDismissDelete: () -> Unit,
    onEditConfirmed: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    editTitle: String = "Change profile picture?",
    editText: String = "This will replace your current profile picture.",
    editConfirmText: String = "Continue",
    deleteTitle: String = "Remove profile picture?",
    deleteText: String = "This will clear the selected photo.",
    deleteConfirmText: String = "Remove"
) {
  if (showEdit) {
    AlertDialog(
        onDismissRequest = onDismissEdit,
        title = { Text(editTitle) },
        text = { Text(editText) },
        confirmButton = { TextButton(onClick = onEditConfirmed) { Text(editConfirmText) } },
        dismissButton = { TextButton(onClick = onDismissEdit) { Text("Cancel") } })
  }

  if (showDelete) {
    AlertDialog(
        onDismissRequest = onDismissDelete,
        title = { Text(deleteTitle) },
        text = { Text(deleteText) },
        confirmButton = { TextButton(onClick = onDeleteConfirmed) { Text(deleteConfirmText) } },
        dismissButton = { TextButton(onClick = onDismissDelete) { Text("Cancel") } })
  }
}
