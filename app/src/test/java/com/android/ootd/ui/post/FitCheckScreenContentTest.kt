package com.android.ootd.ui.post

import android.net.Uri
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.ootd.model.map.Location
import com.android.ootd.ui.theme.OOTDTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FitCheckScreenContentTest {

  @get:Rule val composeRule = createComposeRule()

  @OptIn(ExperimentalMaterial3Api::class)
  private fun setContentWithState(
      uiState: FitCheckUIState,
      onNextClick: (String, String, Location) -> Unit = { _, _, _ -> },
      onBackClick: () -> Unit = {},
      onChooseFromGallery: () -> Unit = {},
      onTakePhoto: () -> Unit = {},
      onDescriptionChange: (String) -> Unit = {},
      onClearError: () -> Unit = {},
      overridePhoto: Boolean = false
  ) {
    composeRule.setContent {
      OOTDTheme {
        FitCheckScreenContent(
            uiState = uiState,
            onNextClick = onNextClick,
            onBackClick = onBackClick,
            onChooseFromGallery = onChooseFromGallery,
            onTakePhoto = onTakePhoto,
            onDescriptionChange = onDescriptionChange,
            onClearError = onClearError,
            overridePhoto = overridePhoto)
      }
    }
  }

  private fun createEmptyState() =
      FitCheckUIState(image = Uri.EMPTY, description = "", errorMessage = null)

  @Test
  fun nextButton_withoutPhoto_showsMissingPhotoWarning() {
    setContentWithState(createEmptyState(), overridePhoto = false)
    composeRule.onNodeWithTag(FitCheckScreenTestTags.NEXT_BUTTON).performClick()

    composeRule.onNodeWithTag(FitCheckScreenTestTags.MISSING_PHOTO_WARNING).assertIsDisplayed()
    composeRule.onNodeWithText("Please add a photo before continuing.").assertIsDisplayed()
  }
}
