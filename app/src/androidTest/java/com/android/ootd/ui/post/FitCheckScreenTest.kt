package com.android.ootd.ui.post

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

/** DISCLAIMER: These tests were generated with the help of AI and verified by human */
class FitCheckScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun fitCheckScreen_showsPlaceholder_andAddPhotoButton() {
    composeTestRule.setContent { FitCheckScreen() }

    composeTestRule.onNodeWithTag(FitCheckScreenTestTags.PLACEHOLDER_ICON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(FitCheckScreenTestTags.ADD_PHOTO_BUTTON).assertIsDisplayed()
  }

  @Test
  fun clickingAddPhotoButton_opensDialog() {
    composeTestRule.setContent { FitCheckScreen() }

    composeTestRule.onNodeWithTag(FitCheckScreenTestTags.ADD_PHOTO_BUTTON).performClick()
    composeTestRule.onNodeWithTag(FitCheckScreenTestTags.ALERT_DIALOG).assertIsDisplayed()
  }

  @Test
  fun dialog_containsCameraAndGalleryButtons() {
    composeTestRule.setContent { FitCheckScreen() }

    composeTestRule.onNodeWithTag(FitCheckScreenTestTags.ADD_PHOTO_BUTTON).performClick()
    composeTestRule.onNodeWithTag(FitCheckScreenTestTags.TAKE_PHOTO_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(FitCheckScreenTestTags.CHOOSE_GALLERY_BUTTON).assertIsDisplayed()
  }

  @Test
  fun descriptionAndCounter_areVisibleOnScreen() {
    composeTestRule.setContent { FitCheckScreen() }

    composeTestRule.onNodeWithTag(FitCheckScreenTestTags.DESCRIPTION_INPUT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(FitCheckScreenTestTags.DESCRIPTION_COUNTER).assertIsDisplayed()
  }

  @Test
  fun clickingNextButtonWithoutPhoto_showsErrorMessage() {
    composeTestRule.setContent { FitCheckScreen() }

    composeTestRule.onAllNodesWithTag(FitCheckScreenTestTags.ERROR_MESSAGE).assertCountEquals(0)

    composeTestRule.onNodeWithTag(FitCheckScreenTestTags.NEXT_BUTTON).performClick()

    composeTestRule
        .onNodeWithTag(FitCheckScreenTestTags.ERROR_MESSAGE)
        .assertIsDisplayed()
        .assertTextEquals("Please select a photo before continuing.")
  }
}
