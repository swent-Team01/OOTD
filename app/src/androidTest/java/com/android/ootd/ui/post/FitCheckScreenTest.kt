package com.android.ootd.ui.post

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

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
}
