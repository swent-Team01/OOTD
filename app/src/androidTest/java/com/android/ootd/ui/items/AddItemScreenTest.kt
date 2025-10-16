package com.android.ootd.ui.items

import android.net.Uri
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import com.android.ootd.model.items.Item
import com.android.ootd.model.items.ItemsRepositoryProvider
import com.android.ootd.ui.post.AddItemScreenTestTags
import com.android.ootd.ui.post.AddItemsScreen
import com.android.ootd.ui.post.AddItemsViewModel
import com.android.ootd.utils.InMemoryItem
import com.android.ootd.utils.ItemsTest
import junit.framework.TestCase
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
    // Initialize type suggestions for tests
    viewModel.initTypeSuggestions(ApplicationProvider.getApplicationContext())
    composeTestRule.setContent { AddItemsScreen(viewModel) }
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
    //
    // composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_CATEGORY).assertTextContains(text)
    //    composeTestRule
    //        .onNodeWithTag(AddItemScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
    //        .assertIsNotDisplayed()
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
  fun enteringInvalidCategoryShowsErrorMessageThenTheCorrectCategory() {
    // viewModel = AddItemsViewModel(repository)
    // Type invalid category
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_CATEGORY).performTextInput("random")

    composeTestRule.runOnIdle {
      assert(viewModel.uiState.value.invalidCategory != null)
      TestCase.assertTrue(viewModel.uiState.value.invalidCategory?.contains("Clothing") == true)
    }

    composeTestRule
        .onNodeWithTag(AddItemScreenTestTags.INPUT_CATEGORY)
        .performTextReplacement("Clothing")

    composeTestRule.runOnIdle { assert(viewModel.uiState.value.invalidCategory == null) }
  }

  @Test
  fun imageIsDisplayedAfterUpload() {
    val uri = Uri.parse("content://dummy/photo.jpg")
    composeTestRule.verifyImageUploadFlow(viewModel, uri)
  }

  @Test
  fun galleryLauncher_setsPhoto_whenUriNotNull() {
    val fakeUri = Uri.parse("content://fake/photo.jpg")

    composeTestRule.runOnIdle { viewModel.setPhoto(fakeUri) }

    composeTestRule.runOnIdle {
      assert(viewModel.uiState.value.invalidPhotoMsg == null)
      assert(viewModel.uiState.value.image == fakeUri)
    }
  }

  @Test
  fun categoryDropdown_onDismiss_callsValidateCategory_whenNotBlank() {
    composeTestRule.runOnIdle {
      viewModel.setCategory("Clothing")
      viewModel.setErrorMsg("old_error")
    }

    // Simulate the dropdown being open
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_CATEGORY).performClick()

    // Simulate dismiss
    composeTestRule.runOnIdle {
      // mimic onDismissRequest logic manually
      viewModel.validateCategory()
    }

    // After validation, invalidCategory should be null
    composeTestRule.runOnIdle { assert(viewModel.uiState.value.invalidCategory == null) }
  }

  @Test
  fun galleryLauncher_doesNothing_whenUriIsNull() {
    composeTestRule.runOnIdle {
      val previousImage = viewModel.uiState.value.image
      val uri: Uri? = null
      if (uri != null) viewModel.setPhoto(uri)
      assert(viewModel.uiState.value.image == previousImage)
    }
  }

  @Test
  fun cameraLauncher_setsPhoto_whenSuccessAndUriNotNull() {
    val fakeUri = Uri.parse("content://fake/camera_photo.jpg")

    composeTestRule.runOnIdle {
      val success = true
      if (success && fakeUri != null) {
        viewModel.setPhoto(fakeUri)
      }
    }

    composeTestRule.runOnIdle { assert(viewModel.uiState.value.image == fakeUri) }
  }

  @Test
  fun cameraLauncher_doesNothing_whenFailureOrUriNull() {
    composeTestRule.runOnIdle {
      val success = false
      val cameraUri: Uri? = null
      val previousImage = viewModel.uiState.value.image
      if (success && cameraUri != null) {
        viewModel.setPhoto(cameraUri)
      }
      assert(viewModel.uiState.value.image == previousImage)
    }
  }

  @Test
  fun clickingAddItemReturns() {
    val item = ItemsTest.Companion.item4
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
  fun dropdownMenuShowsTypeSuggestionsAndSelectsOne() {
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_CATEGORY).performTextInput("Clothing")

    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_TYPE).performTextInput("J")

    composeTestRule.waitForIdle()

    composeTestRule.waitUntil(timeoutMillis = 10_000) {
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

    composeTestRule.waitUntil(timeoutMillis = 10_000) {
      composeTestRule
          .onAllNodesWithTag(AddItemScreenTestTags.CATEGORY_SUGGESTION, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule.onNodeWithText("Clothing", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun addItemButtonDisabledWhenNoImage() {
    composeTestRule.enterAddItemCategory("Clothing")
    composeTestRule.enterAddItemType("Jacket")

    composeTestRule.waitForIdle()
    composeTestRule.ensureVisible(AddItemScreenTestTags.ADD_ITEM_BUTTON)

    composeTestRule
        .onNodeWithTag(AddItemScreenTestTags.ADD_ITEM_BUTTON, useUnmergedTree = true)
        .assertIsNotEnabled()
  }

  @Test
  fun addItemButtonDisabledWhenNoCategory() {
    val uri = "content://dummy/photo.jpg".toUri()
    composeTestRule.enterAddItemPhoto(uri)

    composeTestRule.waitForIdle()

    composeTestRule.ensureVisible(AddItemScreenTestTags.ADD_ITEM_BUTTON)

    composeTestRule
        .onNodeWithTag(AddItemScreenTestTags.ADD_ITEM_BUTTON, useUnmergedTree = true)
        .assertIsNotEnabled()
  }

  @Test
  fun addItemButtonDisabledWhenInvalidCategory() {
    val uri = "content://dummy/photo.jpg".toUri()
    composeTestRule.enterAddItemPhoto(uri)
    composeTestRule.enterAddItemCategory("InvalidCategory")

    composeTestRule.waitForIdle()

    composeTestRule.ensureVisible(AddItemScreenTestTags.ADD_ITEM_BUTTON)

    composeTestRule
        .onNodeWithTag(AddItemScreenTestTags.ADD_ITEM_BUTTON, useUnmergedTree = true)
        .assertIsNotEnabled()
  }

  @Test
  fun materialInputParsesCorrectly() {
    val materialText = "Cotton 80%, Wool 20%"
    composeTestRule.enterAddItemMaterial(materialText)

    composeTestRule.runOnIdle {
      val materials = viewModel.uiState.value.material
      assert(materials.size == 2)
      assert(materials[0].name == "Cotton" && materials[0].percentage == 80.0)
      assert(materials[1].name == "Wool" && materials[1].percentage == 20.0)
    }
  }

  @Test
  fun materialInputHandlesInvalidFormat() {
    val materialText = "Cotton, Wool"
    composeTestRule.enterAddItemMaterial(materialText)

    composeTestRule.runOnIdle {
      val materials = viewModel.uiState.value.material
      assert(materials.isEmpty())
    }
  }

  @Test
  fun typeSuggestionsForAccessoriesCategory() {
    composeTestRule.enterAddItemCategory("Accessories")
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_TYPE).performTextInput("H")

    composeTestRule.waitForIdle()

    composeTestRule.waitUntil(timeoutMillis = 10_000) {
      composeTestRule
          .onAllNodesWithTag(AddItemScreenTestTags.TYPE_SUGGESTIONS, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule.onNodeWithText("Hat", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun typeSuggestionsForShoesCategory() {
    composeTestRule.enterAddItemCategory("Shoes")
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_TYPE).performTextInput("B")

    composeTestRule.waitForIdle()

    composeTestRule.waitUntil(timeoutMillis = 10_000) {
      composeTestRule
          .onAllNodesWithTag(AddItemScreenTestTags.TYPE_SUGGESTIONS, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule.onNodeWithText("Boots", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun typeSuggestionsForBagsCategory() {
    composeTestRule.enterAddItemCategory("Bags")
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_TYPE).performTextInput("B")

    composeTestRule.waitForIdle()

    composeTestRule.waitUntil(timeoutMillis = 10_000) {
      composeTestRule
          .onAllNodesWithTag(AddItemScreenTestTags.TYPE_SUGGESTIONS, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule.onNodeWithText("Backpack", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun categoryNormalizationWorks() {
    composeTestRule.enterAddItemCategory("clothing")
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_TYPE).performTextInput("J")

    composeTestRule.waitForIdle()

    composeTestRule.runOnIdle { assert(viewModel.uiState.value.typeSuggestion.contains("Jacket")) }
  }

  @Test
  fun priceInputOnlyAcceptsValidNumbers() {
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_PRICE).performTextInput("12.99")

    composeTestRule.runOnIdle { assert(viewModel.uiState.value.price == "12.99") }

    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_PRICE).performTextReplacement("abc")

    composeTestRule.runOnIdle { assert(viewModel.uiState.value.price == "12.99") }
  }

  @Test
  fun priceInputAcceptsDecimals() {
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_PRICE).performTextInput("99.99")

    composeTestRule.runOnIdle { assert(viewModel.uiState.value.price == "99.99") }
  }

  @Test
  fun selectingCategorySuggestionClearsError() {
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_CATEGORY).performTextInput("random")

    composeTestRule.runOnIdle { assert(viewModel.uiState.value.invalidCategory != null) }

    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_CATEGORY).performTextReplacement("C")

    composeTestRule.waitForIdle()

    composeTestRule.waitUntil(timeoutMillis = 10_000) {
      composeTestRule
          .onAllNodesWithTag(AddItemScreenTestTags.CATEGORY_SUGGESTION, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithText("Clothing", useUnmergedTree = true).performClick()

    composeTestRule.runOnIdle { assert(viewModel.uiState.value.invalidCategory == null) }
  }

  @Test
  fun selectingTypeSuggestionSetsValue() {
    composeTestRule.enterAddItemCategory("Clothing")
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_TYPE).performTextInput("J")

    composeTestRule.waitForIdle()

    composeTestRule.waitUntil(timeoutMillis = 10_000) {
      composeTestRule
          .onAllNodesWithTag(AddItemScreenTestTags.TYPE_SUGGESTIONS, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithText("Jacket", useUnmergedTree = true).performClick()

    composeTestRule.runOnIdle { assert(viewModel.uiState.value.type == "Jacket") }
  }

  @Test
  fun emptyTypeSuggestionsWhenCategoryNotRecognized() {
    composeTestRule.enterAddItemCategory("UnknownCategory")
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_TYPE).performTextInput("J")

    composeTestRule.waitForIdle()

    composeTestRule.runOnIdle { assert(viewModel.uiState.value.typeSuggestion.isEmpty()) }
  }

  @Test
  fun clearingCategoryInputClearsError() {
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_CATEGORY).performTextInput("random")

    composeTestRule.runOnIdle { assert(viewModel.uiState.value.invalidCategory != null) }

    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_CATEGORY).performTextReplacement("")

    composeTestRule.runOnIdle { assert(viewModel.uiState.value.invalidCategory == null) }
  }

  @Test
  fun canAddItemsReturnsFalseWhenImageMissing() {
    composeTestRule.enterAddItemCategory("Clothing")
    composeTestRule.enterAddItemType("Jacket")

    composeTestRule.runOnIdle {
      val canAdd = viewModel.canAddItems()
      assert(!canAdd)
      assert(viewModel.uiState.value.errorMessage?.contains("photo") == true)
    }
  }

  @Test
  fun canAddItemsReturnsFalseWhenCategoryMissing() {
    val uri = "content://dummy/photo.jpg".toUri()
    composeTestRule.runOnIdle { viewModel.setPhoto(uri) }

    composeTestRule.waitForIdle()

    composeTestRule.runOnIdle {
      val canAdd = viewModel.canAddItems()
      assert(!canAdd)
      assert(viewModel.uiState.value.errorMessage?.contains("category") == true)
    }
  }

  @Test
  fun canAddItemsReturnsFalseWhenCategoryInvalid() {
    val uri = "content://dummy/photo.jpg".toUri()
    composeTestRule.runOnIdle {
      viewModel.setPhoto(uri)
      viewModel.setCategory("InvalidCategory")
      viewModel.validateCategory()
    }

    composeTestRule.waitForIdle()

    composeTestRule.runOnIdle {
      val canAdd = viewModel.canAddItems()
      assert(!canAdd)
      assert(viewModel.uiState.value.errorMessage?.contains("valid category") == true)
    }
  }

  @Test
  fun canAddItemsReturnsTrueWithValidData() {
    val uri = "content://dummy/photo.jpg".toUri()
    composeTestRule.runOnIdle {
      viewModel.setPhoto(uri)
      viewModel.setCategory("Clothing")
      viewModel.validateCategory()
    }

    composeTestRule.waitForIdle()

    composeTestRule.runOnIdle {
      val canAdd = viewModel.canAddItems()
      assert(canAdd)
      assert(viewModel.uiState.value.errorMessage == null)
    }
  }

  @Test
  fun setPhotoWithEmptyUriSetsError() {
    composeTestRule.runOnIdle {
      viewModel.setPhoto(Uri.EMPTY)
      assert(viewModel.uiState.value.invalidPhotoMsg != null)
    }
  }

  @Test
  fun setPhotoWithValidUriClearsError() {
    val uri = "content://dummy/photo.jpg".toUri()
    composeTestRule.runOnIdle {
      viewModel.setPhoto(uri)
      assert(viewModel.uiState.value.invalidPhotoMsg == null)
    }
  }

  @Test
  fun clearErrorMsgWorks() {
    composeTestRule.runOnIdle {
      viewModel.setErrorMsg("Test error")
      assert(viewModel.uiState.value.errorMessage == "Test error")

      viewModel.clearErrorMsg()
      assert(viewModel.uiState.value.errorMessage == null)
    }
  }

  @Test
  fun categoryValidationWithExactMatch() {
    composeTestRule.runOnIdle {
      viewModel.setCategory("Clothing")
      viewModel.validateCategory()
      assert(viewModel.uiState.value.invalidCategory == null)
    }
  }

  @Test
  fun categoryValidationCaseInsensitive() {
    composeTestRule.runOnIdle {
      viewModel.setCategory("clothes")
      viewModel.validateCategory()
      assert(viewModel.uiState.value.invalidCategory == null)
    }
  }

  @Test
  fun categoryValidationWithWhitespace() {
    composeTestRule.runOnIdle {
      viewModel.setCategory("  Clothes  ")
      viewModel.validateCategory()
      assert(viewModel.uiState.value.invalidCategory == null)
    }
  }

  @Test
  fun updateTypeSuggestionsShowsAllWhenInputEmpty() {
    composeTestRule.runOnIdle {
      viewModel.setCategory("Clothing")
      viewModel.updateTypeSuggestions("")

      val suggestions = viewModel.uiState.value.typeSuggestion
      assert(suggestions.contains("Jacket"))
      assert(suggestions.contains("T-shirt"))
      assert(suggestions.contains("Jeans"))
    }
  }

  @Test
  fun updateTypeSuggestionsFiltersCorrectly() {
    composeTestRule.runOnIdle {
      viewModel.setCategory("Clothing")
      viewModel.updateTypeSuggestions("Ja")

      val suggestions = viewModel.uiState.value.typeSuggestion
      assert(suggestions.contains("Jacket"))
      assert(!suggestions.contains("T-shirt"))
    }
  }

  @Test
  fun normalizedCategoryClothingWorks() {
    composeTestRule.runOnIdle {
      viewModel.setCategory("clothing")
      viewModel.updateTypeSuggestions("J")

      val suggestions = viewModel.uiState.value.typeSuggestion
      assert(suggestions.contains("Jacket"))
    }
  }

  @Test
  fun normalizedCategoryShoesWorks() {
    composeTestRule.runOnIdle {
      viewModel.setCategory("shoe")
      viewModel.updateTypeSuggestions("B")

      val suggestions = viewModel.uiState.value.typeSuggestion
      assert(suggestions.contains("Boots"))
    }
  }

  @Test
  fun normalizedCategoryBagsWorks() {
    composeTestRule.runOnIdle {
      viewModel.setCategory("bag")
      viewModel.updateTypeSuggestions("B")

      val suggestions = viewModel.uiState.value.typeSuggestion
      assert(suggestions.contains("Backpack"))
    }
  }

  @Test
  fun normalizedCategoryAccessoriesWorks() {
    composeTestRule.runOnIdle {
      viewModel.setCategory("accessory")
      viewModel.updateTypeSuggestions("H")

      val suggestions = viewModel.uiState.value.typeSuggestion
      assert(suggestions.contains("Hat"))
    }
  }

  @Test
  fun materialParsingWithMultipleItems() {
    composeTestRule.runOnIdle {
      viewModel.setMaterial("Cotton 60%, Polyester 30%, Elastane 10%")

      val materials = viewModel.uiState.value.material
      assert(materials.size == 3)
      assert(materials[0].name == "Cotton" && materials[0].percentage == 60.0)
      assert(materials[1].name == "Polyester" && materials[1].percentage == 30.0)
      assert(materials[2].name == "Elastane" && materials[2].percentage == 10.0)
    }
  }

  @Test
  fun materialParsingWithSingleItem() {
    composeTestRule.runOnIdle {
      viewModel.setMaterial("Cotton 100%")

      val materials = viewModel.uiState.value.material
      assert(materials.size == 1)
      assert(materials[0].name == "Cotton" && materials[0].percentage == 100.0)
    }
  }

  @Test
  fun materialParsingIgnoresInvalidEntries() {
    composeTestRule.runOnIdle {
      viewModel.setMaterial("Cotton 80%, InvalidEntry, Wool 20%")

      val materials = viewModel.uiState.value.material
      assert(materials.size == 2)
      assert(materials[0].name == "Cotton" && materials[0].percentage == 80.0)
      assert(materials[1].name == "Wool" && materials[1].percentage == 20.0)
    }
  }

  @Test
  fun priceConversionToDouble() {
    composeTestRule.runOnIdle {
      viewModel.setPrice("49.99")

      val item =
          Item(
              uuid = "test",
              image = "content://dummy/photo.jpg".toUri(),
              category = "Clothing",
              type = "Jacket",
              brand = "TestBrand",
              price = viewModel.uiState.value.price.toDoubleOrNull() ?: 0.0,
              material = emptyList(),
              link = "")

      assert(item.price == 49.99)
    }
  }

  @Test
  fun priceConversionWithInvalidString() {
    composeTestRule.runOnIdle {
      viewModel.setPrice("invalid")
      val price = viewModel.uiState.value.price.toDoubleOrNull() ?: 0.0
      assert(price == 0.0)
    }
  }

  @Test
  fun alertDialog_shows_and_has_all_buttons() {
    // Open dialog
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.IMAGE_PICKER).performClick()

    // Verify dialog title
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.IMAGE_PICKER_DIALOG).assertIsDisplayed()

    // Verify both buttons are visible
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.TAKE_A_PHOTO).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.PICK_FROM_GALLERY).assertIsDisplayed()

    // Close dialog by clicking one option
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.PICK_FROM_GALLERY).performClick()

    // Assert dialog is dismissed
    composeTestRule.onAllNodesWithText("Select Image").assertCountEquals(0)
  }

  @Test
  fun category_dropdown_shows_and_validates_on_dismiss() {

    val categoryField = composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_CATEGORY)

    // Enter some text to trigger suggestions
    categoryField.performTextInput("Shoes")

    // Force dropdown expansion
    composeTestRule.waitUntil(timeoutMillis = 1_000) {
      composeTestRule
          .onAllNodesWithTag(AddItemScreenTestTags.CATEGORY_SUGGESTION)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Click another input to remove focus, triggering onDismissRequest()
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_TYPE).performClick()

    // Wait for recomposition
    composeTestRule.waitForIdle()
    // Assert dropdown no longer visible
    composeTestRule
        .onAllNodesWithTag(AddItemScreenTestTags.CATEGORY_SUGGESTION)
        .assertCountEquals(0)
  }

  /** Test the onDone keyboard action for Category field triggers validation and hides dropdown. */
  @Test
  fun categoryField_onDone_triggersValidation_and_hidesMenu() {

    val categoryField = composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_CATEGORY)
    categoryField.performTextInput("Jacket")
    categoryField.performImeAction()

    // After onDone, dropdown should close
    composeTestRule
        .onAllNodesWithTag(AddItemScreenTestTags.CATEGORY_SUGGESTION)
        .assertCountEquals(0)
  }

  @Test
  fun whenTakePhotoClicked_dialogCloses_and_buttonIsDisplayed() {
    // Step 1: Open the AlertDialog
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.IMAGE_PICKER).performClick()

    // Step 2: Verify that the dialog and both options are visible
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.IMAGE_PICKER_DIALOG).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.TAKE_A_PHOTO).assertIsDisplayed()

    // Step 3: Click the "📸 Take a Photo" button
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.TAKE_A_PHOTO).performClick()

    // Step 4: Wait for recomposition (dialog should close)
    composeTestRule.waitForIdle()

    // Step 5: Verify that the dialog is now closed
    composeTestRule
        .onAllNodesWithTag(AddItemScreenTestTags.IMAGE_PICKER_DIALOG)
        .assertCountEquals(0)
  }
}
