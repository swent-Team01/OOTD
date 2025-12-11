package com.android.ootd.ui.inventory

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.ootd.model.items.ImageData
import com.android.ootd.model.items.Item
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InventoryGridTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val testItem =
      Item(
          itemUuid = "item1",
          postUuids = emptyList(),
          image = ImageData("id", "url"),
          category = "Clothing",
          ownerId = "owner")

  @Test
  fun inventoryGrid_collapsesAndExpandsCategory() {
    composeTestRule.setContent {
      InventoryGrid(
          items = listOf(testItem),
          onItemClick = {},
          starredItemIds = emptySet(),
          onToggleStar = {})
    }

    // Initial state: Expanded
    composeTestRule.onNodeWithTag("categoryHeader_Clothing").assertIsDisplayed()
    composeTestRule.onNodeWithTag("${InventoryScreenTestTags.ITEM_CARD}_item1").assertIsDisplayed()

    // Collapse
    composeTestRule.onNodeWithTag("categoryHeader_Clothing").performClick()
    composeTestRule
        .onNodeWithTag("${InventoryScreenTestTags.ITEM_CARD}_item1")
        .assertIsNotDisplayed()

    // Expand
    composeTestRule.onNodeWithTag("categoryHeader_Clothing").performClick()
    composeTestRule.onNodeWithTag("${InventoryScreenTestTags.ITEM_CARD}_item1").assertIsDisplayed()
  }
}
