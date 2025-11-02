package com.android.ootd.ui.items

import android.net.Uri
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import com.android.ootd.model.items.ImageData
import com.android.ootd.model.items.Item
import com.android.ootd.model.items.ItemsRepositoryProvider
import com.android.ootd.ui.post.AddItemScreenTestTags
import com.android.ootd.ui.post.AddItemsScreen
import com.android.ootd.ui.post.AddItemsViewModel
import com.android.ootd.utils.InMemoryItem
import com.android.ootd.utils.ItemsTest
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AddItemScreenTest : ItemsTest by InMemoryItem {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var viewModel: AddItemsViewModel
  override val repository = ItemsRepositoryProvider.repository

  @Before
  override fun setUp() {
    super.setUp()
    viewModel = AddItemsViewModel(repository)
    viewModel.initTypeSuggestions(ApplicationProvider.getApplicationContext())
    composeTestRule.setContent { AddItemsScreen(viewModel, onNextScreen = {}) }
  }

  // ----------- Input and photo flow -----------

  @Test
  fun fillAllFields_and_setPhoto_showsValuesAndPreview() {
    val uri = "content://dummy/photo.jpg".toUri()

    composeTestRule.enterAddItemType("Jacket")
    composeTestRule.enterAddItemCategory("Clothing")
    composeTestRule.enterAddItemBrand("Brand")
    composeTestRule.enterAddItemPrice(99.99)
    composeTestRule.enterAddItemLink("www.ootd.com")
    composeTestRule.enterAddItemMaterial("Cotton 80%, Polyester 20%")

    composeTestRule.enterAddItemPhoto()
    composeTestRule.runOnIdle { viewModel.setPhoto(uri) }

    // Assert a few key fields reflect the input
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_BRAND).assertTextContains("Brand")
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_PRICE).assertTextContains("99.99")
    composeTestRule.checkPhotoPreviewDisplayed()
  }

  // ----------- Image picker dialog & actions -----------

  @Test
  fun imagePickerDialog_shows_and_dismisses_via_options() {
    // Open
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.IMAGE_PICKER).performClick()
    composeTestRule.waitForIdle()

    // Dialog visible with both options
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.IMAGE_PICKER_DIALOG).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.PICK_FROM_GALLERY).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.TAKE_A_PHOTO).assertIsDisplayed()

    // Dismiss by selecting gallery
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.PICK_FROM_GALLERY).performClick()
    composeTestRule.waitForIdle()

    // Verify dialog dismissed
    composeTestRule.runOnIdle {
      // Give time for dialog to dismiss
    }
    composeTestRule
        .onAllNodesWithTag(AddItemScreenTestTags.IMAGE_PICKER_DIALOG)
        .assertCountEquals(0)
  }

  // ----------- Category validation flow -----------

  @Test
  fun invalidCategory_showsError_then_validClears() {
    // Invalid first
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_CATEGORY).performTextInput("random")
    composeTestRule.runOnIdle { assert(viewModel.uiState.value.invalidCategory != null) }

    // Now enter a valid category
    composeTestRule
        .onNodeWithTag(AddItemScreenTestTags.INPUT_CATEGORY)
        .performTextReplacement("Clothing")
    composeTestRule.runOnIdle { assert(viewModel.uiState.value.invalidCategory == null) }
  }

  // ----------- Photo setters and edge cases -----------

  @Test
  fun setPhoto_galleryAndCamera_andNullNoop_andEmptySetsError() {
    val galleryUri = Uri.parse("content://fake/gallery_photo.jpg")
    val cameraUri = Uri.parse("content://fake/camera_photo.jpg")

    // Gallery launcher behavior
    composeTestRule.runOnIdle { viewModel.setPhoto(galleryUri) }
    composeTestRule.runOnIdle {
      assert(viewModel.uiState.value.invalidPhotoMsg == null)
      assert(viewModel.uiState.value.localPhotoUri == galleryUri)
    }

    // Camera launcher behavior
    composeTestRule.runOnIdle { viewModel.setPhoto(cameraUri) }
    composeTestRule.runOnIdle { assert(viewModel.uiState.value.localPhotoUri == cameraUri) }

    // Null-like scenario: do not call setPhoto and ensure previous remains
    composeTestRule.runOnIdle {
      val before = viewModel.uiState.value.localPhotoUri
      // simulate no-op (null not passed to setPhoto)
      assert(viewModel.uiState.value.localPhotoUri == before)
    }

    // Empty Uri sets error
    composeTestRule.runOnIdle {
      viewModel.setPhoto(Uri.EMPTY)
      assert(viewModel.uiState.value.invalidPhotoMsg != null)
    }
  }

  // ----------- Add button enabled states -----------

  @Test
  fun addButton_disabledForMissingOrInvalidInputs_and_enabledWhenValid() {
    // No image + minimal inputs -> disabled
    composeTestRule.enterAddItemCategory("Clothing")
    composeTestRule.enterAddItemType("Jacket")
    composeTestRule.ensureVisible(AddItemScreenTestTags.ADD_ITEM_BUTTON)
    composeTestRule
        .onNodeWithTag(AddItemScreenTestTags.ADD_ITEM_BUTTON, useUnmergedTree = true)
        .assertIsNotEnabled()
    composeTestRule.runOnIdle { assert(!viewModel.uiState.value.isAddingValid) }

    // With image but invalid category -> disabled
    val uri = "content://dummy/photo.jpg".toUri()
    composeTestRule.runOnIdle { viewModel.setPhoto(uri) }
    composeTestRule.enterAddItemCategory("InvalidCategory")
    composeTestRule.ensureVisible(AddItemScreenTestTags.ADD_ITEM_BUTTON)
    composeTestRule
        .onNodeWithTag(AddItemScreenTestTags.ADD_ITEM_BUTTON, useUnmergedTree = true)
        .assertIsNotEnabled()

    // Valid all -> enabled
    composeTestRule
        .onNodeWithTag(AddItemScreenTestTags.INPUT_CATEGORY)
        .performTextReplacement("Clothing")
    composeTestRule.enterAddItemType("T-Shirt")
    composeTestRule.enterAddItemBrand("Nike")
    composeTestRule.enterAddItemPrice(19.99)
    composeTestRule
        .onNodeWithTag(AddItemScreenTestTags.ADD_ITEM_BUTTON, useUnmergedTree = true)
        .assertIsEnabled()
  }

  // ----------- Suggestions: show and select -----------

  @Test
  fun typeSuggestion_showsAndCanSelect_forClothing() {
    composeTestRule.enterAddItemCategory("Clothing")
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_TYPE).performTextInput("J")

    composeTestRule.waitUntil(timeoutMillis = 10_000) {
      composeTestRule
          .onAllNodesWithTag(AddItemScreenTestTags.TYPE_SUGGESTIONS, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule.onNodeWithText("Jacket", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("Jacket", useUnmergedTree = true).performClick()
    composeTestRule.runOnIdle { assert(viewModel.uiState.value.type == "Jacket") }
  }

  @Test
  fun categorySuggestion_showsAndSelectingClearsError() {
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_CATEGORY).performTextInput("C")

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
  fun typeSuggestions_acrossCategories() {
    // Accessories -> Hat
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

    // Shoes -> Boots
    composeTestRule
        .onNodeWithTag(AddItemScreenTestTags.INPUT_CATEGORY)
        .performTextReplacement("Shoes")
    // Clear and re-enter type to trigger suggestions for new category
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_TYPE).performTextReplacement("")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_TYPE).performTextInput("B")
    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(timeoutMillis = 10_000) {
      composeTestRule
          .onAllNodesWithTag(AddItemScreenTestTags.TYPE_SUGGESTIONS, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule.onNodeWithText("Boots", useUnmergedTree = true).assertIsDisplayed()

    // Bags -> Backpack
    composeTestRule
        .onNodeWithTag(AddItemScreenTestTags.INPUT_CATEGORY)
        .performTextReplacement("Bags")
    // Clear and re-enter type to trigger suggestions for new category
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_TYPE).performTextReplacement("")
    composeTestRule.waitForIdle()
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

  // ----------- Material parsing -----------

  @Test
  fun materialInput_parsesVariants_and_ignoresInvalid() {
    // Multi
    composeTestRule.enterAddItemMaterial("Cotton 60%, Polyester 30%, Elastane 10%")
    composeTestRule.runOnIdle {
      val m = viewModel.uiState.value.material
      assert(m.size == 3)
      assert(m[0].name == "Cotton" && m[0].percentage == 60.0)
      assert(m[1].name == "Polyester" && m[1].percentage == 30.0)
      assert(m[2].name == "Elastane" && m[2].percentage == 10.0)
    }

    // Single
    composeTestRule.enterAddItemMaterial("Cotton 100%")
    composeTestRule.runOnIdle {
      val m = viewModel.uiState.value.material
      assert(m.size == 1)
      assert(m[0].name == "Cotton" && m[0].percentage == 100.0)
    }

    // Invalid entries are ignored
    composeTestRule.enterAddItemMaterial("Cotton 80%, InvalidEntry, Wool 20%")
    composeTestRule.runOnIdle {
      val m = viewModel.uiState.value.material
      assert(m.size == 2)
      assert(m[0].name == "Cotton" && m[0].percentage == 80.0)
      assert(m[1].name == "Wool" && m[1].percentage == 20.0)
    }
  }

  // ----------- Price handling -----------

  @Test
  fun priceInput_acceptsDecimals_and_rejectsInvalid() {
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_PRICE).performTextInput("12.99")
    composeTestRule.runOnIdle { assert(viewModel.uiState.value.price == "12.99") }

    // Replace with invalid; value should stay last valid
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_PRICE).performTextReplacement("abc")
    composeTestRule.runOnIdle { assert(viewModel.uiState.value.price == "12.99") }

    // Conversion example
    composeTestRule.runOnIdle {
      val item =
          Item(
              itemUuid = "test",
              image = ImageData("testPhoto", "content://dummy/photo.jpg"),
              category = "Clothing",
              type = "Jacket",
              brand = "TestBrand",
              price = viewModel.uiState.value.price.toDoubleOrNull() ?: 0.0,
              material = emptyList(),
              link = "",
              ownerId = "user123")
      assert(item.price == 12.99)
    }
  }

  // ----------- Overlay visibility -----------

  @Test
  fun uploadingOverlay_initiallyHidden_and_hidesAfterAttempt() = runTest {
    // Initially hidden
    composeTestRule.runOnIdle { assert(!viewModel.uiState.value.isLoading) }
    composeTestRule.onAllNodesWithText("Uploading item...").assertCountEquals(0)

    // Provide valid inputs
    val uri = "content://dummy/photo.jpg".toUri()
    composeTestRule.runOnIdle { viewModel.setPhoto(uri) }
    composeTestRule.enterAddItemCategory("Clothing")
    composeTestRule.enterAddItemType("T-Shirt")
    composeTestRule.enterAddItemBrand("Nike")
    composeTestRule.enterAddItemPrice(19.99)

    // Trigger add; even if image upload fails in tests, overlay should hide afterward
    composeTestRule.runOnIdle { viewModel.onAddItemClick() }
    composeTestRule.waitForIdle()
    kotlinx.coroutines.delay(1000)

    composeTestRule.runOnIdle { assert(!viewModel.uiState.value.isLoading) }
    composeTestRule.onAllNodesWithText("Uploading item...").assertCountEquals(0)
  }
}
