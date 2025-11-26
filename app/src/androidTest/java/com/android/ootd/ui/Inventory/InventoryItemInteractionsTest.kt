package com.android.ootd.ui.Inventory

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.center
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.ootd.model.items.ImageData
import com.android.ootd.model.items.Item
import com.android.ootd.ui.theme.OOTDTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InventoryItemInteractionsTest {

  @get:Rule val composeRule = createComposeRule()

  private val sampleItem =
      Item(
          itemUuid = "sample",
          postUuids = emptyList(),
          image = ImageData("img-sample", "https://example.com/sample.jpg"),
          category = "Clothing",
          type = "T-Shirt",
          brand = "Brand",
          price = 10.0,
          material = emptyList(),
          link = "",
          ownerId = "owner")

  @Test
  fun inventoryItemCard_onClickInvokesCallback() {
    var clicked = false
    composeRule.setContent {
      OOTDTheme {
        InventoryItemCard(
            item = sampleItem, onClick = { clicked = true }, isStarred = false, onToggleStar = {})
      }
    }

    composeRule
        .onNodeWithTag("${InventoryScreenTestTags.ITEM_CARD}_${sampleItem.itemUuid}")
        .assertIsDisplayed()
        .performTouchInput { click(center) }

    composeRule.runOnIdle { assertTrue(clicked) }
  }

  @Test
  fun inventoryGrid_clickingItemPropagatesCallback() {
    val items = listOf(sampleItem)
    var clicked: Item? = null
    composeRule.setContent {
      OOTDTheme {
        InventoryGrid(
            items = items,
            onItemClick = { clicked = it },
            starredItemIds = emptySet(),
            onToggleStar = {})
      }
    }

    composeRule
        .onNodeWithTag("${InventoryScreenTestTags.ITEM_CARD}_${items[0].itemUuid}")
        .assertIsDisplayed()
        .performTouchInput { click(center) }

    composeRule.runOnIdle { assertEquals(items[0], clicked) }
  }
}
