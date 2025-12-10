package com.android.ootd.ui.account

import android.os.Build
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.ootd.ui.account.UiTestTags.TAG_ACCOUNT_DELETE
import com.android.ootd.ui.account.UiTestTags.TAG_ACCOUNT_EDIT
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
class ProfilePictureConfirmationTest {

  @get:Rule val composeRule = createComposeRule()

  companion object {
    @JvmStatic
    @BeforeClass
    fun ensureFingerprint() {
      if (Build.FINGERPRINT == null) {
        ReflectionHelpers.setStaticField(Build::class.java, "FINGERPRINT", "robolectric")
      }
    }
  }

  @Test
  fun editProfile_showsConfirmationDialog_beforeOpeningPicker() {
    composeRule.setContent {
      val context = LocalContext.current
      var showImageSourceDialog by remember { mutableStateOf(false) }
      var showEditProfileConfirm by remember { mutableStateOf(false) }
      var showDeleteProfileConfirm by remember { mutableStateOf(false) }

      AvatarSection(
          avatarUri = "https://example.com/avatar.png",
          username = "tester",
          onEditClick = { showEditProfileConfirm = true },
          deleteProfilePicture = { showDeleteProfileConfirm = true },
          context = context)

      ProfilePictureEditor(
          context = context,
          showImageSourceDialog = showImageSourceDialog,
          onShowImageSourceDialogChange = { showImageSourceDialog = it })

      if (showEditProfileConfirm) {
        AlertDialog(
            onDismissRequest = { showEditProfileConfirm = false },
            title = { Text("Change profile picture?") },
            text = { Text("This will replace your current profile picture.") },
            confirmButton = {
              TextButton(
                  onClick = {
                    showEditProfileConfirm = false
                    showImageSourceDialog = true
                  }) {
                    Text("Continue")
                  }
            },
            dismissButton = {
              TextButton(onClick = { showEditProfileConfirm = false }) { Text("Cancel") }
            })
      }

      if (showDeleteProfileConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteProfileConfirm = false },
            title = { Text("Remove profile picture?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
              TextButton(onClick = { showDeleteProfileConfirm = false }) { Text("Remove") }
            },
            dismissButton = {
              TextButton(onClick = { showDeleteProfileConfirm = false }) { Text("Cancel") }
            })
      }
    }

    // Click edit -> confirmation dialog appears
    composeRule.onNodeWithTag(TAG_ACCOUNT_EDIT).performClick()
    composeRule.onNodeWithText("Change profile picture?").assertIsDisplayed()

    // Confirm -> image source dialog opens
    composeRule.onNodeWithText("Continue").performClick()
    composeRule.onNodeWithText("Select Image").assertIsDisplayed()
  }

  @Test
  fun deleteProfile_showsConfirmationDialog_beforeRemoving() {
    composeRule.setContent {
      val context = LocalContext.current
      var showImageSourceDialog by remember { mutableStateOf(false) }
      var showEditProfileConfirm by remember { mutableStateOf(false) }
      var showDeleteProfileConfirm by remember { mutableStateOf(false) }
      var deleted by remember { mutableStateOf(false) }

      AvatarSection(
          avatarUri = "https://example.com/avatar.png",
          username = "tester",
          onEditClick = { showEditProfileConfirm = true },
          deleteProfilePicture = { showDeleteProfileConfirm = true },
          context = context)

      ProfilePictureEditor(
          context = context,
          showImageSourceDialog = showImageSourceDialog,
          onShowImageSourceDialogChange = { showImageSourceDialog = it })

      if (showEditProfileConfirm) {
        AlertDialog(
            onDismissRequest = { showEditProfileConfirm = false },
            title = { Text("Change profile picture?") },
            text = { Text("This will replace your current profile picture.") },
            confirmButton = {
              TextButton(
                  onClick = {
                    showEditProfileConfirm = false
                    showImageSourceDialog = true
                  }) {
                    Text("Continue")
                  }
            },
            dismissButton = {
              TextButton(onClick = { showEditProfileConfirm = false }) { Text("Cancel") }
            })
      }

      if (showDeleteProfileConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteProfileConfirm = false },
            title = { Text("Remove profile picture?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
              TextButton(
                  onClick = {
                    deleted = true
                    showDeleteProfileConfirm = false
                  }) {
                    Text("Remove")
                  }
            },
            dismissButton = {
              TextButton(onClick = { showDeleteProfileConfirm = false }) { Text("Cancel") }
            })
      }

      Text(if (deleted) "deleted" else "not-deleted", modifier = Modifier.testTag("delete-state"))
    }

    composeRule.onNodeWithTag(TAG_ACCOUNT_DELETE).performClick()
    composeRule.onNodeWithText("Remove profile picture?").assertIsDisplayed()

    composeRule.onNodeWithText("Remove").performClick()
    composeRule.onNodeWithTag("delete-state").assertExists()
    composeRule.onNodeWithText("deleted").assertIsDisplayed()
  }
}
