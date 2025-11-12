package com.android.ootd.ui.post

import android.net.Uri
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Tests for FitCheckScreen UI behavior. */
class FitCheckScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var viewModel: FitCheckViewModel

  @Before
  fun setUp() {
    viewModel = FitCheckViewModel()
    composeTestRule.setContent { FitCheckScreen(fitCheckViewModel = viewModel) }
  }

  // ---------- Basic layout ----------

  @Test
  fun fitCheckScreen_showsPlaceholder_andAddPhotoButton() {
    composeTestRule.onNodeWithTag(FitCheckScreenTestTags.PLACEHOLDER_ICON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(FitCheckScreenTestTags.ADD_PHOTO_BUTTON).assertIsDisplayed()
  }

  @Test
  fun descriptionAndCounter_areVisibleOnScreen() {
    composeTestRule.onNodeWithTag(FitCheckScreenTestTags.DESCRIPTION_INPUT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(FitCheckScreenTestTags.DESCRIPTION_COUNTER).assertIsDisplayed()
  }

  // ---------- Dialog and buttons ----------

  @Test
  fun clickingAddPhotoButton_opensDialog() {
    composeTestRule.onNodeWithTag(FitCheckScreenTestTags.ADD_PHOTO_BUTTON).performClick()
    composeTestRule.onNodeWithTag(FitCheckScreenTestTags.ALERT_DIALOG).assertIsDisplayed()
  }

  @Test
  fun dialog_containsCameraAndGalleryButtons() {
    composeTestRule.onNodeWithTag(FitCheckScreenTestTags.ADD_PHOTO_BUTTON).performClick()
    composeTestRule.onNodeWithTag(FitCheckScreenTestTags.TAKE_PHOTO_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(FitCheckScreenTestTags.CHOOSE_GALLERY_BUTTON).assertIsDisplayed()
  }

  @Test
  fun clickingChooseFromGallery_dismissesDialog() {
    composeTestRule.onNodeWithTag(FitCheckScreenTestTags.ADD_PHOTO_BUTTON).performClick()
    composeTestRule.onNodeWithTag(FitCheckScreenTestTags.CHOOSE_GALLERY_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onAllNodesWithTag(FitCheckScreenTestTags.ALERT_DIALOG).assertCountEquals(0)
  }

  @Test
  fun clickingAddPhotoButton_thenTakePhoto_opensCameraScreen() {
    composeTestRule.onNodeWithTag(FitCheckScreenTestTags.ADD_PHOTO_BUTTON).performClick()
    composeTestRule.onNodeWithTag(FitCheckScreenTestTags.TAKE_PHOTO_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Camera screen branch should be reached (no crash)
    composeTestRule.runOnIdle { assert(!viewModel.uiState.value.isPhotoValid) }
  }

  // ---------- Next button & error ----------

  @Test
  fun clickingNextButtonWithoutPhoto_showsErrorMessage() {
    composeTestRule.onAllNodesWithTag(FitCheckScreenTestTags.ERROR_MESSAGE).assertCountEquals(0)

    composeTestRule.onNodeWithTag(FitCheckScreenTestTags.NEXT_BUTTON).performClick()

    composeTestRule
        .onNodeWithTag(FitCheckScreenTestTags.ERROR_MESSAGE)
        .assertIsDisplayed()
        .assertExists()
  }

  @Test
  fun setPhoto_updatesViewModelAndShowsPreview() {
    val uri = Uri.parse("content://dummy/photo.jpg")

    composeTestRule.runOnIdle { viewModel.setPhoto(uri) }
    composeTestRule.waitForIdle()

    composeTestRule.runOnIdle {
      assert(viewModel.uiState.value.image == uri)
      assert(viewModel.uiState.value.isPhotoValid)
    }

    composeTestRule.onNodeWithTag(FitCheckScreenTestTags.IMAGE_PREVIEW).assertExists()
  }
}
