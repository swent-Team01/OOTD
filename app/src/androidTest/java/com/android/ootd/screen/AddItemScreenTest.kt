package com.android.ootd.screen

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.core.net.toUri
import com.android.ootd.model.ItemsRepositoryProvider
import com.android.ootd.ui.post.AddItemScreenTestTags
import com.android.ootd.ui.post.AddItemsScreen
import com.android.ootd.ui.post.AddItemsViewModel
import com.android.ootd.utils.InMemoryItem
import com.android.ootd.utils.ItemsTest
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AddItemScreenTest : ItemsTest by InMemoryItem {

  @get:Rule val composeTestRule = createComposeRule()

  // Initialize ViewModel here to access its state in tests
  private lateinit var viewModel: AddItemsViewModel
  override val repository = ItemsRepositoryProvider.repository

  @Before
  override fun setUp() {
    super.setUp()
    viewModel = AddItemsViewModel(repository)
    composeTestRule.setContent { AddItemsScreen(viewModel) }
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
        .assertIsDisplayed()
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
  fun canEnterPhoto() {
    val uri = "content://dummy/photo.jpg".toUri()
    composeTestRule.enterAddItemPhoto(uri)
    composeTestRule.checkPhotoPreviewDisplayed()
  }

  @Test
  fun imageUploadButtonIsVisibleAndClickable() {
    composeTestRule.checkImageUploadButtonIsDisplayed()
    composeTestRule.checkImageUploadButtonClickable()
  }

  @Test
  fun enteringInvalidCategoryShowsErrorMessage() {
    val invalidCategory = "InvalidCategory"
    composeTestRule.enterAddItemCategory(invalidCategory)
    composeTestRule
        .onNodeWithTag(AddItemScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertIsDisplayed() // Adjust this assertion based on actual error handling
  }

  @Test
  fun addButtonDisabledWhenRequiredFieldsMissing() {
    // Only set one required field
    composeTestRule.enterAddItemType("T-shirt")
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.ADD_ITEM_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun addButtonDisabledWhenRequiredCategoryFieldMissing() {
    // Only set one required field
    composeTestRule.enterAddItemPhoto("content://dummy/photo.jpg".toUri())
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.ADD_ITEM_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun enteringInvalidCategoryShowsErrorMessageThenTheCorrectCategory() {
    // viewModel = AddItemsViewModel(repository)
    // Type invalid category
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_CATEGORY).performTextInput("random")

    composeTestRule.runOnIdle {
      assert(viewModel.uiState.value.invalidCategory != null)
      assertTrue(viewModel.uiState.value.invalidCategory?.contains("Clothes") == true)
    }

    composeTestRule
        .onNodeWithTag(AddItemScreenTestTags.INPUT_CATEGORY)
        .performTextReplacement("Clothes")

    composeTestRule.runOnIdle { assert(viewModel.uiState.value.invalidCategory == null) }
  }

  @Test
  fun imageIsDisplayedAfterUpload() {
    val uri = Uri.parse("content://dummy/photo.jpg")

    // Make sure AddItemsScreen is composed
    // Run the actual image-upload verification
    composeTestRule.verifyImageUploadFlow(viewModel, uri)
  }
}
