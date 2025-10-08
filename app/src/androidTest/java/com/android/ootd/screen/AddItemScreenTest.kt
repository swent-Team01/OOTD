package com.android.ootd.screen

import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.android.ootd.ui.post.AddItemScreenTestTags
import com.android.ootd.ui.post.AddItemsScreen
import com.android.ootd.utils.InMemoryItem
import com.android.ootd.utils.ItemsTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AddItemScreenTest : ItemsTest by InMemoryItem {

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  override fun setUp() {
    super.setUp()
    composeTestRule.setContent { AddItemsScreen() }
  }

  @Test
  fun displayAllComponents() {
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.ADD_ITEM_BUTTON).assertExists()
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_TYPE).assertExists()
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_CATEGORY).assertExists()
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_BRAND).assertExists()
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_PRICE).assertExists()
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_LINK).assertExists()
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_MATERIAL).assertExists()
    composeTestRule
        .onNodeWithTag(AddItemScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertDoesNotExist()
  }

  @Test
  fun canEnterType() {
    val text = "Jacket"
    composeTestRule.enterAddItemType(text)
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_TYPE)
  }

  @Test
  fun canEnterCategory() {
    val text = "Category"
    composeTestRule.enterAddItemCategory(text)
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_CATEGORY).assertTextContains(text)
    composeTestRule
        .onNodeWithTag(AddItemScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertIsNotDisplayed()
  }

  @Test
  fun canEnterBrand() {
    val text = "Brand"
    composeTestRule.enterAddItemBrand(text)
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_BRAND).assertTextContains(text)
  }

  @Test
  fun canEnterPrice() {
    val text = 99.99
    composeTestRule.enterAddItemPrice(text)
    composeTestRule
        .onNodeWithTag(AddItemScreenTestTags.INPUT_PRICE)
        .assertTextContains(text.toString())
  }

  @Test
  fun canEnterLink() {
    val text = "www.ootd.com"
    composeTestRule.enterAddItemLink(text)
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_LINK).assertTextContains(text)
  }

  @Test
  fun canEnterMaterial() {
    val text = "Cotton"
    composeTestRule.enterAddItemMaterial(text)
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_MATERIAL).assertTextContains(text)
  }

  @Test
  fun imageUploadButtonIsVisibleAndClickable() {
    composeTestRule.checkImageUploadButtonIsDisplayed()
    composeTestRule.checkImageUploadButtonClickable()
  }
}
