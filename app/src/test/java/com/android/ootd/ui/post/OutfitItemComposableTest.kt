package com.android.ootd.ui.post

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.android.ootd.model.items.ImageData
import com.android.ootd.model.items.Item
import com.android.ootd.model.items.Material
import com.android.ootd.ui.theme.OOTDTheme
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OutfitItemComposableTest {

  @get:Rule val composeRule = createComposeRule()

  private val item =
      Item(
          itemUuid = "item-1",
          postUuids = listOf("post-1"),
          image = ImageData("img", "url"),
          category = "Top",
          type = "Shirt",
          brand = "Brand",
          price = 10.0,
          material = listOf(Material("cotton", 100.0)),
          ownerId = "user")

  @Test
  fun removeIcon_showsAndTriggersCallback() {
    var removed = false

    composeRule.setContent {
      OOTDTheme { OutfitItem(item = item, onClick = {}, onRemove = { removed = true }) }
    }

    composeRule.onNodeWithTag(PreviewItemScreenTestTags.REMOVE_ITEM_BUTTON).assertIsDisplayed()
    composeRule.onNodeWithTag(PreviewItemScreenTestTags.REMOVE_ITEM_BUTTON).performClick()
    composeRule
        .onNodeWithTag(PreviewItemScreenTestTags.REMOVE_ITEM_CONFIRM_BUTTON)
        .assertIsDisplayed()
        .performClick()

    assertTrue(removed)
  }
}
