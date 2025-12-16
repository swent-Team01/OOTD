package com.android.ootd.ui.feed

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.android.ootd.ui.post.items.ItemsTestTags
import com.android.ootd.ui.post.items.SeeItemDetailsDialogPreview
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

    composeTestRule.onNodeWithTag(ItemsTestTags.ITEM_DETAILS_DIALOG).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ItemsTestTags.ITEM_IMAGE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ItemsTestTags.ITEM_CATEGORY).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ItemsTestTags.ITEM_LINK, useUnmergedTree = true).assertExists()
  }
}
