package com.android.ootd.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.ootd.model.items.ImageData
import com.android.ootd.model.items.Item
import com.android.ootd.ui.post.items.OotdItemCard
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OotdItemCardTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val testItem =
      Item(
          itemUuid = "item1",
          postUuids = emptyList(),
          image = ImageData("id", "url"),
          category = "Clothing",
          type = "T-Shirt",
          brand = "Nike",
          price = 10.0,
          currency = "USD",
          ownerId = "owner")

  @Test
  fun ootdItemCard_displaysCorrectly() {
    composeTestRule.setContent {
      OotdItemCard(item = testItem, onClick = {}, showPrice = true, showCategory = true)
    }

    composeTestRule.onNodeWithText("CLOTHING").assertIsDisplayed()
    composeTestRule.onNodeWithText("T-Shirt").assertIsDisplayed()
    composeTestRule.onNodeWithText("Nike").assertIsDisplayed()
    composeTestRule.onNodeWithText("10 USD").assertIsDisplayed()
  }

  @Test
  fun ootdItemCard_hidesElements() {
    composeTestRule.setContent {
      OotdItemCard(item = testItem, onClick = {}, showPrice = false, showCategory = false)
    }

    composeTestRule.onNodeWithText("CLOTHING").assertDoesNotExist()
    composeTestRule.onNodeWithText("10 USD").assertDoesNotExist()
    composeTestRule.onNodeWithText("T-Shirt").assertIsDisplayed()
  }
}
