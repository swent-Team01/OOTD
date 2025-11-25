package com.android.ootd.ui.feed

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SeeFitItemDialogTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun preview_dialog_rendersImageAndMetadata() {
    composeTestRule.setContent { SeeItemDetailsDialogPreview() }

    composeTestRule.onNodeWithTag(SeeFitScreenTestTags.ITEM_DETAILS_DIALOG).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SeeFitScreenTestTags.ITEM_IMAGE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SeeFitScreenTestTags.ITEM_CATEGORY).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SeeFitScreenTestTags.ITEM_LINK, useUnmergedTree = true)
        .assertExists()
  }
}
