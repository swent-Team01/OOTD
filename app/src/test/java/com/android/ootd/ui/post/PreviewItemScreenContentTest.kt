package com.android.ootd.ui.post

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.android.ootd.model.items.ImageData
import com.android.ootd.model.items.Item
import com.android.ootd.model.items.Material
import com.android.ootd.ui.theme.OOTDTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PreviewItemScreenContentTest {

  @get:Rule val composeRule = createComposeRule()

  private val sampleItem =
      Item(
          itemUuid = "item-1",
          postUuids = listOf("post-1"),
          image = ImageData("img", "url"),
          category = "Top",
          type = "Shirt",
          brand = "Brand",
          price = 10.0,
          material = listOf(Material("cotton", 100.0)),
          ownerId = "owner")

  @Test
  @OptIn(ExperimentalMaterial3Api::class)
  fun previewItemList_showsEmptyPlaceholderWhenNoItems() {
    composeRule.setContent {
      OOTDTheme {
        PreviewItemScreenContent(
            ui = PreviewUIState(items = emptyList()),
            scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(),
            onEditItem = {},
            onRemoveItem = {},
            onAddItem = {},
            onSelectFromInventory = {},
            onPublish = {},
            onGoBack = {},
            enablePreview = true)
      }
    }

    composeRule.onNodeWithTag(PreviewItemScreenTestTags.EMPTY_ITEM_LIST_MSG).assertIsDisplayed()
  }

  @Test
  @OptIn(ExperimentalMaterial3Api::class)
  fun previewItemList_showsListAndActionsWhenItemsPresent() {
    composeRule.setContent {
      OOTDTheme {
        PreviewItemScreenContent(
            ui = PreviewUIState(items = listOf(sampleItem)),
            scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(),
            onEditItem = {},
            onRemoveItem = {},
            onAddItem = {},
            onSelectFromInventory = {},
            onPublish = {},
            onGoBack = {},
            enablePreview = true)
      }
    }

    composeRule.onNodeWithTag(PreviewItemScreenTestTags.ITEM_LIST).assertIsDisplayed()
    composeRule.onNodeWithTag(PreviewItemScreenTestTags.REMOVE_ITEM_BUTTON).assertIsDisplayed()
  }
}
