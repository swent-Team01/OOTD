package com.android.ootd.ui.feed

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.android.ootd.model.items.ImageData
import com.android.ootd.model.items.Item
import com.android.ootd.model.items.Material
import com.android.ootd.ui.theme.OOTDTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SeeFitPriceChipTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val baseItem =
      Item(
          itemUuid = "item-1",
          postUuids = emptyList(),
          image = ImageData("img-1", "https://example.com/image.jpg"),
          category = "Jacket",
          type = "Bomber",
          brand = "Brand",
          price = 45.0,
          currency = "USD",
          material = listOf(Material("cotton", 100.0)),
          link = "https://shop/item-1",
          ownerId = "owner-1")

  @Test
  fun priceChip_showsFormattedPrice_whenPriceAndCurrencyPresent() {
    composeTestRule.setContent {
      OOTDTheme {
        ItemCard(
            item = baseItem,
            onClick = {},
            onEditClick = {},
            isOwner = false,
            isStarred = false,
            onToggleStar = {},
            showStarToggle = false)
      }
    }

    composeTestRule.onNodeWithText("45 USD", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun priceChip_hidden_whenPriceMissing() {
    composeTestRule.setContent {
      OOTDTheme {
        ItemCard(
            item = baseItem.copy(price = null),
            onClick = {},
            onEditClick = {},
            isOwner = false,
            isStarred = false,
            onToggleStar = {},
            showStarToggle = false)
      }
    }

    composeTestRule.onAllNodesWithText("45 USD", useUnmergedTree = true).assertCountEquals(0)
  }
}
