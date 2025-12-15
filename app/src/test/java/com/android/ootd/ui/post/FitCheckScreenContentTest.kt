package com.android.ootd.ui.post

import android.net.Uri
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.android.ootd.model.map.Location
import com.android.ootd.ui.theme.OOTDTheme
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FitCheckScreenContentTest {

  @get:Rule val composeRule = createComposeRule()

  @OptIn(ExperimentalMaterial3Api::class)
  @Test
  fun emptyState_showsPlaceholderIcon() {
    val uiState = FitCheckUIState(image = Uri.EMPTY, description = "", errorMessage = null)

    composeRule.setContent {
      OOTDTheme { FitCheckScreenContent(uiState = uiState, overridePhoto = true) }
    }

    composeRule.onNodeWithTag(FitCheckScreenTestTags.IMAGE_PREVIEW).assertIsDisplayed()
    composeRule.onNodeWithTag(FitCheckScreenTestTags.PLACEHOLDER_ICON).assertIsDisplayed()
  }

  @OptIn(ExperimentalMaterial3Api::class)
  @Test
  fun addPhotoButton_opensPhotoSelectionDialog() {
    val uiState = FitCheckUIState(image = Uri.EMPTY, description = "", errorMessage = null)

    composeRule.setContent {
      OOTDTheme { FitCheckScreenContent(uiState = uiState, overridePhoto = true) }
    }

    // Click Add Photo button
    composeRule.onNodeWithTag(FitCheckScreenTestTags.ADD_PHOTO_BUTTON).performClick()

    // Verify photo selection dialog appears
    composeRule.onNodeWithTag(FitCheckScreenTestTags.ALERT_DIALOG).assertIsDisplayed()
    composeRule.onNodeWithText("Select Photo").assertIsDisplayed()
  }

  @OptIn(ExperimentalMaterial3Api::class)
  @Test
  fun photoSelectionDialog_takePhotoOption_triggersCallback() {
    val uiState = FitCheckUIState(image = Uri.EMPTY, description = "", errorMessage = null)
    var takePhotoCalled = false

    composeRule.setContent {
      OOTDTheme {
        FitCheckScreenContent(
            uiState = uiState, onTakePhoto = { takePhotoCalled = true }, overridePhoto = true)
      }
    }

    // Open photo selection dialog
    composeRule.onNodeWithTag(FitCheckScreenTestTags.ADD_PHOTO_BUTTON).performClick()

    // Click Take Photo option
    composeRule.onNodeWithTag(FitCheckScreenTestTags.TAKE_PHOTO_BUTTON).performClick()

    // Verify callback was triggered
    assertTrue(takePhotoCalled)
  }

  @OptIn(ExperimentalMaterial3Api::class)
  @Test
  fun photoSelectionDialog_chooseGalleryOption_triggersCallback() {
    val uiState = FitCheckUIState(image = Uri.EMPTY, description = "", errorMessage = null)
    var chooseGalleryCalled = false

    composeRule.setContent {
      OOTDTheme {
        FitCheckScreenContent(
            uiState = uiState,
            onChooseFromGallery = { chooseGalleryCalled = true },
            overridePhoto = true)
      }
    }

    // Open photo selection dialog
    composeRule.onNodeWithTag(FitCheckScreenTestTags.ADD_PHOTO_BUTTON).performClick()

    // Click Choose from Gallery option
    composeRule.onNodeWithTag(FitCheckScreenTestTags.CHOOSE_GALLERY_BUTTON).performClick()

    // Verify callback was triggered
    assertTrue(chooseGalleryCalled)
  }

  @OptIn(ExperimentalMaterial3Api::class)
  @Test
  fun descriptionInput_acceptsText() {
    val uiState = FitCheckUIState(image = Uri.EMPTY, description = "", errorMessage = null)
    var capturedDescription = ""

    composeRule.setContent {
      OOTDTheme {
        FitCheckScreenContent(
            uiState = uiState,
            onDescriptionChange = { capturedDescription = it },
            overridePhoto = true)
      }
    }

    // Type in description
    composeRule
        .onNodeWithTag(FitCheckScreenTestTags.DESCRIPTION_INPUT)
        .performTextInput("Test description")

    // Verify callback was triggered
    assertEquals("Test description", capturedDescription)
  }

  @OptIn(ExperimentalMaterial3Api::class)
  @Test
  fun descriptionCounter_displaysCorrectCount() {
    val uiState = FitCheckUIState(image = Uri.EMPTY, description = "Hello", errorMessage = null)

    composeRule.setContent {
      OOTDTheme { FitCheckScreenContent(uiState = uiState, overridePhoto = true) }
    }

    // Verify counter shows remaining characters (100 - 5 = 95)
    composeRule.onNodeWithTag(FitCheckScreenTestTags.DESCRIPTION_COUNTER).assertIsDisplayed()
    composeRule.onNodeWithText("95/100 characters left").assertIsDisplayed()
  }

  @OptIn(ExperimentalMaterial3Api::class)
  @Test
  fun errorMessage_displaysWhenPresent() {
    val uiState =
        FitCheckUIState(image = Uri.EMPTY, description = "", errorMessage = "Error: No photo added")

    composeRule.setContent {
      OOTDTheme { FitCheckScreenContent(uiState = uiState, overridePhoto = true) }
    }

    // Verify error message is displayed
    composeRule.onNodeWithTag(FitCheckScreenTestTags.ERROR_MESSAGE).assertIsDisplayed()
    composeRule.onNodeWithText("Error: No photo added").assertIsDisplayed()
  }

  @OptIn(ExperimentalMaterial3Api::class)
  @Test
  fun nextButton_withPhoto_triggersCallback() {
    val mockUri = Uri.parse("content://test/image.jpg")
    val uiState = FitCheckUIState(image = mockUri, description = "Test outfit", errorMessage = null)
    var nextClickCalled = false
    var capturedImageUri = ""
    var capturedDescription = ""
    var capturedLocation: Location? = null

    composeRule.setContent {
      OOTDTheme {
        FitCheckScreenContent(
            uiState = uiState,
            onNextClick = { imageUri, description, location ->
              nextClickCalled = true
              capturedImageUri = imageUri
              capturedDescription = description
              capturedLocation = location
            },
            overridePhoto = true)
      }
    }

    // Click Next button
    composeRule.onNodeWithTag(FitCheckScreenTestTags.NEXT_BUTTON).performClick()

    // Verify callback was triggered with correct parameters
    assertTrue(nextClickCalled)
    assertEquals(mockUri.toString(), capturedImageUri)
    assertEquals("Test outfit", capturedDescription)
  }

  @OptIn(ExperimentalMaterial3Api::class)
  @Test
  fun nextButton_withoutPhoto_showsMissingPhotoWarning() {
    val uiState = FitCheckUIState(image = Uri.EMPTY, description = "Test", errorMessage = null)

    composeRule.setContent {
      OOTDTheme { FitCheckScreenContent(uiState = uiState, overridePhoto = false) }
    }

    // Click Next button without photo
    composeRule.onNodeWithTag(FitCheckScreenTestTags.NEXT_BUTTON).performClick()

    // Verify missing photo warning dialog appears
    composeRule.onNodeWithTag(FitCheckScreenTestTags.MISSING_PHOTO_WARNING).assertIsDisplayed()
    composeRule.onNodeWithText("Add a Photo").assertIsDisplayed()
    composeRule
        .onNodeWithText("Please add a photo before continuing to add items.")
        .assertIsDisplayed()
  }

  @OptIn(ExperimentalMaterial3Api::class)
  @Test
  fun missingPhotoWarning_addPhotoButton_opensPhotoSelectionDialog() {
    val uiState = FitCheckUIState(image = Uri.EMPTY, description = "", errorMessage = null)

    composeRule.setContent {
      OOTDTheme { FitCheckScreenContent(uiState = uiState, overridePhoto = false) }
    }

    // Click Next button to show warning
    composeRule.onNodeWithTag(FitCheckScreenTestTags.NEXT_BUTTON).performClick()
    composeRule.onNodeWithTag(FitCheckScreenTestTags.MISSING_PHOTO_WARNING).assertIsDisplayed()

    // Click "Add Photo" button in warning dialog
    composeRule
        .onNodeWithTag(FitCheckScreenTestTags.MISSING_PHOTO_WARNING_ADD_BUTTON)
        .performClick()

    // Verify photo selection dialog appears
    composeRule.onNodeWithTag(FitCheckScreenTestTags.ALERT_DIALOG).assertIsDisplayed()
  }

  @OptIn(ExperimentalMaterial3Api::class)
  @Test
  fun missingPhotoWarning_cancelButton_dismissesDialog() {
    val uiState = FitCheckUIState(image = Uri.EMPTY, description = "", errorMessage = null)

    composeRule.setContent {
      OOTDTheme { FitCheckScreenContent(uiState = uiState, overridePhoto = false) }
    }

    // Click Next button to show warning
    composeRule.onNodeWithTag(FitCheckScreenTestTags.NEXT_BUTTON).performClick()
    composeRule.onNodeWithTag(FitCheckScreenTestTags.MISSING_PHOTO_WARNING).assertIsDisplayed()

    // Click Cancel button
    composeRule
        .onNodeWithTag(FitCheckScreenTestTags.MISSING_PHOTO_WARNING_CANCEL_BUTTON)
        .performClick()

    // Verify warning dialog is dismissed
    composeRule.onNodeWithTag(FitCheckScreenTestTags.MISSING_PHOTO_WARNING).assertDoesNotExist()
  }

  @OptIn(ExperimentalMaterial3Api::class)
  @Test
  fun backButton_triggersCallback() {
    val uiState = FitCheckUIState(image = Uri.EMPTY, description = "", errorMessage = null)
    var backClickCalled = false

    composeRule.setContent {
      OOTDTheme {
        FitCheckScreenContent(
            uiState = uiState, onBackClick = { backClickCalled = true }, overridePhoto = true)
      }
    }

    // Click back button
    composeRule.onNodeWithTag(FitCheckScreenTestTags.BACK_BUTTON).performClick()

    // Verify callback was triggered
    assertTrue(backClickCalled)
  }

  @OptIn(ExperimentalMaterial3Api::class)
  @Test
  fun withPhoto_displaysImagePreview() {
    val mockUri = Uri.parse("content://test/image.jpg")
    val uiState = FitCheckUIState(image = mockUri, description = "", errorMessage = null)

    composeRule.setContent {
      OOTDTheme { FitCheckScreenContent(uiState = uiState, overridePhoto = true) }
    }

    // Verify image preview is displayed (placeholder icon should not be visible)
    composeRule.onNodeWithTag(FitCheckScreenTestTags.IMAGE_PREVIEW).assertIsDisplayed()
  }

  @OptIn(ExperimentalMaterial3Api::class)
  @Test
  fun overridePhoto_allowsNextWithoutPhoto() {
    val uiState = FitCheckUIState(image = Uri.EMPTY, description = "", errorMessage = null)
    var nextClickCalled = false

    composeRule.setContent {
      OOTDTheme {
        FitCheckScreenContent(
            uiState = uiState,
            onNextClick = { _, _, _ -> nextClickCalled = true },
            overridePhoto = true)
      }
    }

    // Click Next button without photo but with override
    composeRule.onNodeWithTag(FitCheckScreenTestTags.NEXT_BUTTON).performClick()

    // Verify callback was triggered (no warning shown)
    assertTrue(nextClickCalled)
  }

  @OptIn(ExperimentalMaterial3Api::class)
  @Test
  fun clearError_triggersCallback() {
    val uiState = FitCheckUIState(image = Uri.EMPTY, description = "", errorMessage = "Test error")
    var clearErrorCalled = false

    composeRule.setContent {
      OOTDTheme {
        FitCheckScreenContent(
            uiState = uiState,
            onClearError = { clearErrorCalled = true },
            onNextClick = { _, _, _ -> },
            overridePhoto = true)
      }
    }

    // Click Next button (should call clearError before proceeding)
    composeRule.onNodeWithTag(FitCheckScreenTestTags.NEXT_BUTTON).performClick()

    // Verify clearError was called
    assertTrue(clearErrorCalled)
  }

  @OptIn(ExperimentalMaterial3Api::class)
  @Test
  fun topBar_displaysCorrectTitle() {
    val uiState = FitCheckUIState(image = Uri.EMPTY, description = "", errorMessage = null)

    composeRule.setContent {
      OOTDTheme { FitCheckScreenContent(uiState = uiState, overridePhoto = true) }
    }

    // Verify top bar is displayed with correct title
    composeRule.onNodeWithTag(FitCheckScreenTestTags.TOP_BAR).assertIsDisplayed()
    composeRule.onNodeWithText("FitCheck").assertIsDisplayed()
  }

  @OptIn(ExperimentalMaterial3Api::class)
  @Test
  fun allScreenElements_areVisible() {
    val uiState = FitCheckUIState(image = Uri.EMPTY, description = "", errorMessage = null)

    composeRule.setContent {
      OOTDTheme { FitCheckScreenContent(uiState = uiState, overridePhoto = true) }
    }

    // Verify all main screen elements are visible
    composeRule.onNodeWithTag(FitCheckScreenTestTags.SCREEN).assertIsDisplayed()
    composeRule.onNodeWithTag(FitCheckScreenTestTags.TOP_BAR).assertIsDisplayed()
    composeRule.onNodeWithTag(FitCheckScreenTestTags.IMAGE_PREVIEW).assertIsDisplayed()
    composeRule.onNodeWithTag(FitCheckScreenTestTags.DESCRIPTION_INPUT).assertIsDisplayed()
    composeRule.onNodeWithTag(FitCheckScreenTestTags.DESCRIPTION_COUNTER).assertIsDisplayed()
    composeRule.onNodeWithTag(FitCheckScreenTestTags.ADD_PHOTO_BUTTON).assertIsDisplayed()
    composeRule.onNodeWithTag(FitCheckScreenTestTags.NEXT_BUTTON).assertIsDisplayed()
  }
}
