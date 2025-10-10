package com.android.ootd.screen

import android.content.res.Configuration
import android.net.Uri
import android.util.Log
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.core.net.toUri
import androidx.test.platform.app.InstrumentationRegistry
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

    composeTestRule.enterAddItemPhoto("content://dummy/photo.jpg".toUri())
    composeTestRule.enterAddItemCategory("Clothes")
    composeTestRule.enterAddItemType("Jacket")
    composeTestRule.enterAddItemBrand("Brand")
    composeTestRule.enterAddItemPrice(99.99)
    composeTestRule.enterAddItemLink("www.ootd.com")
    composeTestRule.enterAddItemMaterial("Cotton 80%, Polyester 20%")
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
    val text = "clothes"
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
  }

  @Test
  fun canEnterLink() {
    val text = "www.ootd.com"
    composeTestRule.enterAddItemLink(text)
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_LINK).assertTextContains(text)
  }

  @Test
  fun canEnterMaterial() {
    val text = "Cotton 80%, Polyester 20%"
    composeTestRule.enterAddItemMaterial(text)
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

    composeTestRule.ensureVisible(AddItemScreenTestTags.ADD_ITEM_BUTTON)

    composeTestRule.onNodeWithTag(AddItemScreenTestTags.ADD_ITEM_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun addButtonDisabledWhenRequiredCategoryFieldMissing() {
    // Only set one required field
    composeTestRule.enterAddItemPhoto("content://dummy/photo.jpg".toUri())
    composeTestRule.ensureVisible(AddItemScreenTestTags.ADD_ITEM_BUTTON)

    composeTestRule.onNodeWithTag(AddItemScreenTestTags.ADD_ITEM_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun enteringInvalidCategoryShowsErrorMessageThenTheCorrectCategory() {
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
    composeTestRule.verifyImageUploadFlow(viewModel, uri)
  }

  @Test
  fun clickingAddItemReturns() {
    val item = ItemsTest.item4
    composeTestRule.enterAddItemDetails(item)

    composeTestRule.runOnIdle { viewModel.setPhoto(item.image) }

    composeTestRule.waitForIdle()

    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
          .onAllNodesWithTag(AddItemScreenTestTags.ADD_ITEM_BUTTON)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(AddItemScreenTestTags.ADD_ITEM_BUTTON)
        .assertExists()
        .performClick()

    composeTestRule.onNodeWithTag(AddItemScreenTestTags.ADD_ITEM_BUTTON).assertIsDisplayed()
  }

  @Test
  fun printDeviceConfiguration() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val metrics = context.resources.displayMetrics
    Log.i(
        "DEVICE_INFO",
        "width=${metrics.widthPixels}, height=${metrics.heightPixels}, densityDpi=${metrics.densityDpi}")

    val config = context.resources.configuration
    Log.i(
        "DEVICE_INFO",
        "screenLayout=${config.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK}")
  }

  @Test
  fun dropdownMenuShowsTypeSuggestionsAndSelectsOne() {
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_CATEGORY).performTextInput("Clothes")

    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_TYPE).performTextInput("J")

    composeTestRule.waitForIdle()

    composeTestRule.waitUntil(timeoutMillis = 3_000) {
      composeTestRule
          .onAllNodesWithTag(AddItemScreenTestTags.TYPE_SUGGESTIONS, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule.onNodeWithText("Jacket", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun dropdownMenuShowsCategorySuggestionsAndSelectsOne() {
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_CATEGORY).performTextInput("C")

    composeTestRule.waitForIdle()

    composeTestRule.waitUntil(timeoutMillis = 3_000) {
      composeTestRule
          .onAllNodesWithTag(AddItemScreenTestTags.CATEGORY_SUGGESTION, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule.onNodeWithText("Clothes", useUnmergedTree = true).assertIsDisplayed()
  }
}
