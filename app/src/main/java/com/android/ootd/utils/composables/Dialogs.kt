package com.android.ootd.utils.composables

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

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
