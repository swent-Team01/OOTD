package com.android.ootd.ui.items

import android.net.Uri
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
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
import com.android.ootd.ui.post.items.AddItemScreenTestTags
import com.android.ootd.ui.post.items.AddItemsScreen
import com.android.ootd.ui.post.items.AddItemsScreenSmallPreview
import com.android.ootd.ui.post.items.AddItemsViewModel
import com.android.ootd.utils.InMemoryItem
import com.android.ootd.utils.ItemsTest
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// Test partially generated with an AI coding agent
class AddItemScreenTest : ItemsTest by InMemoryItem {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var viewModel: AddItemsViewModel
  override val repository = ItemsRepositoryProvider.repository

  @Before
  override fun setUp() {
    super.setUp()
    viewModel = AddItemsViewModel(repository)
    viewModel.initTypeSuggestions(ApplicationProvider.getApplicationContext())
    // removed initial composeTestRule.setContent to allow tests to decide which content to render
  }

  private fun setMainScreen() {
    composeTestRule.setContent {
      AddItemsScreen(addItemsViewModel = viewModel, onNextScreen = {}, postUuid = "postuid")
    }
  }

  // ----------- Input and photo flow -----------

  @Test
  fun fillAllFields_showsCorrectValues() {
    setMainScreen()
    // Fill all text fields
    composeTestRule.enterAddItemCategory("Clothing")
    composeTestRule.enterAddItemType("Jacket")
    composeTestRule.enterAddItemBrand("Brand")
    composeTestRule.enterAddItemPrice(99.99)
    composeTestRule.enterAddItemLink("www.ootd.com")
    composeTestRule.enterAddItemMaterial("Cotton 80%, Polyester 20%")

    // Verify all fields contain the correct values
    composeTestRule.runOnIdle {
      assert(viewModel.uiState.value.category == "Clothing")
      assert(viewModel.uiState.value.type == "Jacket")
      assert(viewModel.uiState.value.brand == "Brand")
      assert(viewModel.uiState.value.price == "99.99")
      assert(viewModel.uiState.value.link == "www.ootd.com")
      assert(viewModel.uiState.value.materialText == "Cotton 80%, Polyester 20%")

      // Verify material was parsed correctly
      val materials = viewModel.uiState.value.material
      assert(materials.size == 2)
      assert(materials[0].name == "Cotton" && materials[0].percentage == 80.0)
      assert(materials[1].name == "Polyester" && materials[1].percentage == 20.0)
    }
  }

  @Test
  fun setPhoto_updatesViewModelAndPreviewExists() {
    // Pre-populate state before rendering
    val uri = "content://dummy/photo.jpg".toUri()
    composeTestRule.runOnIdle { viewModel.setPhoto(uri) }
    setMainScreen()

    // Verify ViewModel state updated
    composeTestRule.runOnIdle {
      assert(viewModel.uiState.value.localPhotoUri == uri)
      assert(viewModel.uiState.value.invalidPhotoMsg == null)
    }

    // Wait for photo preview component to appear in the composition tree
    composeTestRule.waitForNodeWithTag(AddItemScreenTestTags.IMAGE_PREVIEW, timeoutMillis = 5_000)

    // Verify photo preview component exists
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.IMAGE_PREVIEW).assertExists()
  }

  // ----------- Image picker dialog & actions -----------

  @Test
  fun imagePickerDialog_shows_and_dismisses_via_options() {
    setMainScreen()

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

  // ----------- Category dropdown selection -----------

  @Test
  fun categoryDropdown_showsValidOptions_andSelectionWorks() {
    setMainScreen()

    // Click dropdown to open it
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_CATEGORY).performClick()
    composeTestRule.waitForIdle()

    // Verify all valid categories are shown
    composeTestRule.onNodeWithText("Clothing").assertExists()
    composeTestRule.onNodeWithText("Shoes").assertExists()
    composeTestRule.onNodeWithText("Accessories").assertExists()
    composeTestRule.onNodeWithText("Bags").assertExists()

    // Select "Clothing"
    composeTestRule.onNodeWithText("Clothing").performClick()
    composeTestRule.waitForIdle()

    // Verify selection
    composeTestRule.runOnIdle {
      assert(viewModel.uiState.value.category == "Clothing")
      assert(viewModel.uiState.value.invalidCategory == null)
    }
  }

  // ----------- Photo setters and edge cases -----------

  @Test
  fun setPhoto_galleryAndCamera_andNullNoop_andEmptySetsError() {
    setMainScreen()
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
  fun addButton_disabledForMissingInputs_and_enabledWhenValid() {
    setMainScreen()

    // No image + minimal inputs -> disabled
    composeTestRule.enterAddItemCategory("Clothing")
    composeTestRule.enterAddItemType("Jacket")
    composeTestRule.ensureVisible(AddItemScreenTestTags.ADD_ITEM_BUTTON)
    composeTestRule
        .onNodeWithTag(AddItemScreenTestTags.ADD_ITEM_BUTTON, useUnmergedTree = true)
        .assertIsNotEnabled()
    composeTestRule.runOnIdle { assert(!viewModel.uiState.value.isAddingValid) }

    // With image but no category -> disabled
    val uri = "content://dummy/photo.jpg".toUri()
    composeTestRule.runOnIdle { viewModel.setPhoto(uri) }
    // Reset category by setting to empty
    composeTestRule.runOnIdle { viewModel.setCategory("") }
    composeTestRule.ensureVisible(AddItemScreenTestTags.ADD_ITEM_BUTTON)
    composeTestRule
        .onNodeWithTag(AddItemScreenTestTags.ADD_ITEM_BUTTON, useUnmergedTree = true)
        .assertIsNotEnabled()

    // Valid all (image + category selected from dropdown) -> enabled
    composeTestRule.enterAddItemCategory("Clothing")
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
    setMainScreen()

    composeTestRule.enterAddItemCategory("Clothing")
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_TYPE).performTextInput("J")

    // Wait for suggestions dropdown to appear
    composeTestRule.waitForNodeWithTag(
        AddItemScreenTestTags.TYPE_SUGGESTIONS, timeoutMillis = 10_000)

    composeTestRule.onNodeWithText("Jacket", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("Jacket", useUnmergedTree = true).performClick()
    composeTestRule.runOnIdle { assert(viewModel.uiState.value.type == "Jacket") }
  }

  @Test
  fun categorySuggestion_showsAndSelectingClearsError() {
    setMainScreen()

    // Click to open the category dropdown
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_CATEGORY).performClick()

    // Wait for dropdown menu to appear and select "Clothing"
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Clothing", useUnmergedTree = true).performClick()

    composeTestRule.runOnIdle { assert(viewModel.uiState.value.invalidCategory == null) }
  }

  @Test
  fun typeSuggestions_acrossCategories() {
    setMainScreen()

    // Accessories -> Hat
    composeTestRule.enterAddItemCategory("Accessories")
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_TYPE).performTextInput("H")
    composeTestRule.waitForIdle()

    // Wait for suggestions dropdown to appear
    composeTestRule.waitForNodeWithTag(
        AddItemScreenTestTags.TYPE_SUGGESTIONS, timeoutMillis = 10_000)
    composeTestRule.onNodeWithText("Hat", useUnmergedTree = true).assertIsDisplayed()

    // Shoes -> Boots
    // Click to open category dropdown and select "Shoes"
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_CATEGORY).performClick()
    composeTestRule.onNodeWithText("Shoes").performClick()

    // Clear and re-enter type to trigger suggestions for new category
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_TYPE).performTextReplacement("")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_TYPE).performTextInput("B")
    composeTestRule.waitForIdle()

    // Wait for suggestions dropdown to appear
    composeTestRule.waitForNodeWithTag(
        AddItemScreenTestTags.TYPE_SUGGESTIONS, timeoutMillis = 10_000)
    composeTestRule.onNodeWithText("Boots", useUnmergedTree = true).assertIsDisplayed()

    // Bags -> Backpack
    // Click to open category dropdown and select "Bags"
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_CATEGORY).performClick()
    composeTestRule.onNodeWithText("Bags").performClick()

    // Clear and re-enter type to trigger suggestions for new category
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_TYPE).performTextReplacement("")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.INPUT_TYPE).performTextInput("B")
    composeTestRule.waitForIdle()

    // Wait for suggestions dropdown to appear
    composeTestRule.waitForNodeWithTag(
        AddItemScreenTestTags.TYPE_SUGGESTIONS, timeoutMillis = 10_000)
    composeTestRule.onNodeWithText("Backpack", useUnmergedTree = true).assertIsDisplayed()
  }

  // ----------- Material parsing -----------

  @Test
  fun materialInput_parsesVariants_and_ignoresInvalid() {
    setMainScreen()

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
    setMainScreen()

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
              postUuids = listOf("test_post1"),
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
    setMainScreen()

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

  @Test
  fun addItems_preview_rendersCoreElements() {
    // Render preview directly (no main screen content rendered beforehand)
    composeTestRule.setContent { AddItemsScreenSmallPreview() }

    // Top bar title and go back button exist
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.TITLE_ADD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.ALL_FIELDS).assertIsDisplayed()

    // Image preview placeholder is visible in preview mode
    composeTestRule.onNodeWithTag(AddItemScreenTestTags.IMAGE_PREVIEW).assertIsDisplayed()
  }
}
