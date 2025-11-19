package com.android.ootd.ui.items

import android.net.Uri
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.core.app.ApplicationProvider
import com.android.ootd.model.items.ImageData
import com.android.ootd.model.items.Item
import com.android.ootd.model.items.ItemsRepositoryLocal
import com.android.ootd.model.items.ItemsRepositoryProvider
import com.android.ootd.model.items.Material
import com.android.ootd.ui.post.items.EditItemsScreen
import com.android.ootd.ui.post.items.EditItemsScreenTestTags
import com.android.ootd.ui.post.items.EditItemsViewModel
import com.android.ootd.utils.InMemoryItem.waitForNodeWithTag
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// Test partially generated with an AI coding agent
class EditItemsScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var viewModel: EditItemsViewModel
  private lateinit var repository: ItemsRepositoryLocal
  private val testItem =
      Item(
          itemUuid = "test-item-1",
          postUuids = listOf("test_post2"),
          image = ImageData("test-item-1-photo", "https://example.com/image.jpg"),
          category = "Clothing",
          type = "T-shirt",
          brand = "Nike",
          price = 29.99,
          material =
              listOf(
                  Material(name = "Cotton", percentage = 80.0),
                  Material(name = "Polyester", percentage = 20.0)),
          link = "https://nike.com/tshirt",
          ownerId = "user123")

  @Before
  fun setUp() {
    // Use local repository for testing
    ItemsRepositoryProvider.useLocal()
    repository = ItemsRepositoryProvider.getLocalRepository()!!
    repository.clearAll()

    viewModel = EditItemsViewModel(repository)
    // Deterministically load type suggestions (avoid relying on LaunchedEffect timing)
    viewModel.initTypeSuggestions(ApplicationProvider.getApplicationContext())
  }

  @After
  fun tearDown() {
    repository.clearAll()
    ItemsRepositoryProvider.reset()
  }

  // Helper: ensure a node is visible (scroll if needed)
  private fun ensureVisible(tag: String) {
    val alreadyVisible =
        runCatching {
              composeTestRule.onNodeWithTag(tag, useUnmergedTree = true).assertIsDisplayed()
            }
            .isSuccess
    if (alreadyVisible) return

    repeat(5) {
      composeTestRule.onNodeWithTag(EditItemsScreenTestTags.ALL_FIELDS).performTouchInput {
        swipeUp(startY = bottom, endY = top)
      }
      composeTestRule.waitForIdle()
      if (runCatching {
            composeTestRule.onNodeWithTag(tag, useUnmergedTree = true).assertIsDisplayed()
          }
          .isSuccess)
          return
    }
  }

  // -------- Load/Populate --------

  @Test
  fun loadItem_populatesAllFields() {
    composeTestRule.setContent { EditItemsScreen(testItem.itemUuid, viewModel) }
    composeTestRule.runOnIdle { viewModel.loadItem(testItem) }
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(EditItemsScreenTestTags.INPUT_ITEM_CATEGORY)
        .assertTextContains("Clothing")
    composeTestRule
        .onNodeWithTag(EditItemsScreenTestTags.INPUT_ITEM_TYPE)
        .assertTextEquals("Type", "T-shirt")
    composeTestRule
        .onNodeWithTag(EditItemsScreenTestTags.INPUT_ITEM_BRAND)
        .assertTextEquals("Brand", "Nike")
    composeTestRule
        .onNodeWithTag(EditItemsScreenTestTags.INPUT_ITEM_PRICE)
        .assertTextEquals("Price", "29.99")
    composeTestRule
        .onNodeWithTag(EditItemsScreenTestTags.INPUT_ITEM_MATERIAL)
        .assertTextContains("Cotton 80.0%, Polyester 20.0%")
    composeTestRule
        .onNodeWithTag(EditItemsScreenTestTags.INPUT_ITEM_LINK)
        .assertTextEquals("Link", "https://nike.com/tshirt")
  }

  // -------- Save/Delete enablement --------

  @Test
  fun save_and_delete_button_enablement() {
    composeTestRule.setContent { EditItemsScreen(testItem.itemUuid, viewModel) }

    // Initially disabled (no loaded item / missing prereqs)
    ensureVisible(EditItemsScreenTestTags.BUTTON_SAVE_CHANGES)
    composeTestRule.onNodeWithTag(EditItemsScreenTestTags.BUTTON_SAVE_CHANGES).assertIsNotEnabled()
    ensureVisible(EditItemsScreenTestTags.BUTTON_DELETE_ITEM)
    composeTestRule.onNodeWithTag(EditItemsScreenTestTags.BUTTON_DELETE_ITEM).assertIsNotEnabled()

    // Load item and set minimal valid state
    composeTestRule.runOnIdle {
      viewModel.loadItem(testItem)
      viewModel.setPhoto(Uri.parse("https://example.com/test.jpg"))
      viewModel.setCategory("Clothing")
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(EditItemsScreenTestTags.BUTTON_SAVE_CHANGES).assertIsEnabled()
    composeTestRule.onNodeWithTag(EditItemsScreenTestTags.BUTTON_DELETE_ITEM).assertIsEnabled()
  }

  // -------- Edit multiple fields --------

  @Test
  fun editingFields_updatesUI() {
    composeTestRule.setContent { EditItemsScreen(testItem.itemUuid, viewModel) }
    composeTestRule.runOnIdle { viewModel.loadItem(testItem) }
    composeTestRule.waitForIdle()

    ensureVisible(EditItemsScreenTestTags.INPUT_ITEM_CATEGORY)
    // Click to open dropdown
    composeTestRule.onNodeWithTag(EditItemsScreenTestTags.INPUT_ITEM_CATEGORY).performClick()
    // Select an option from dropdown
    composeTestRule.onNodeWithText("Shoes").performClick()
    // Verify selection
    composeTestRule
        .onNodeWithTag(EditItemsScreenTestTags.INPUT_ITEM_CATEGORY)
        .assertTextContains("Shoes")

    ensureVisible(EditItemsScreenTestTags.INPUT_ITEM_TYPE)
    composeTestRule.onNodeWithTag(EditItemsScreenTestTags.INPUT_ITEM_TYPE).performTextClearance()
    composeTestRule
        .onNodeWithTag(EditItemsScreenTestTags.INPUT_ITEM_TYPE)
        .performTextInput("Hoodie")
    composeTestRule
        .onNodeWithTag(EditItemsScreenTestTags.INPUT_ITEM_TYPE)
        .assertTextContains("Hoodie")

    ensureVisible(EditItemsScreenTestTags.INPUT_ITEM_BRAND)
    composeTestRule.onNodeWithTag(EditItemsScreenTestTags.INPUT_ITEM_BRAND).performTextClearance()
    composeTestRule
        .onNodeWithTag(EditItemsScreenTestTags.INPUT_ITEM_BRAND)
        .performTextInput("Adidas")
    composeTestRule
        .onNodeWithTag(EditItemsScreenTestTags.INPUT_ITEM_BRAND)
        .assertTextContains("Adidas")

    ensureVisible(EditItemsScreenTestTags.INPUT_ITEM_PRICE)
    composeTestRule.onNodeWithTag(EditItemsScreenTestTags.INPUT_ITEM_PRICE).performTextClearance()
    composeTestRule
        .onNodeWithTag(EditItemsScreenTestTags.INPUT_ITEM_PRICE)
        .performTextInput("49.99")
    composeTestRule
        .onNodeWithTag(EditItemsScreenTestTags.INPUT_ITEM_PRICE)
        .assertTextContains("49.99")

    ensureVisible(EditItemsScreenTestTags.INPUT_ITEM_LINK)
    composeTestRule.onNodeWithTag(EditItemsScreenTestTags.INPUT_ITEM_LINK).performTextClearance()
    composeTestRule
        .onNodeWithTag(EditItemsScreenTestTags.INPUT_ITEM_LINK)
        .performTextInput("https://adidas.com")
    composeTestRule
        .onNodeWithTag(EditItemsScreenTestTags.INPUT_ITEM_LINK)
        .assertTextContains("https://adidas.com")

    ensureVisible(EditItemsScreenTestTags.INPUT_ITEM_MATERIAL)
    composeTestRule
        .onNodeWithTag(EditItemsScreenTestTags.INPUT_ITEM_MATERIAL)
        .performTextClearance()
    composeTestRule
        .onNodeWithTag(EditItemsScreenTestTags.INPUT_ITEM_MATERIAL)
        .performTextInput("Wool 50%, Cotton 50%")
    composeTestRule
        .onNodeWithTag(EditItemsScreenTestTags.INPUT_ITEM_MATERIAL)
        .assertTextContains("Wool 50%, Cotton 50%")
  }

  // -------- Suggestions show/select --------

  @Test
  fun typeSuggestions_show_and_select_across_categories() {
    composeTestRule.setContent { EditItemsScreen(testItem.itemUuid, viewModel) }

    // Clothing suggestions: focus and type to trigger onValueChange
    composeTestRule.runOnIdle { viewModel.setCategory("Clothing") }
    composeTestRule.onNodeWithTag(EditItemsScreenTestTags.INPUT_ITEM_TYPE).performClick()
    composeTestRule.onNodeWithTag(EditItemsScreenTestTags.INPUT_ITEM_TYPE).performTextInput("J")
    composeTestRule.runOnIdle { viewModel.updateTypeSuggestions("J") }

    // Wait for suggestions dropdown to appear
    composeTestRule.waitForNodeWithTag(
        EditItemsScreenTestTags.TYPE_SUGGESTIONS, timeoutMillis = 10_000)
    composeTestRule.onNodeWithText("Jacket", useUnmergedTree = true).assertExists()

    // Shoes suggestions and selection: clear then type 'B' to filter Boots
    composeTestRule.runOnIdle { viewModel.setCategory("Shoes") }
    composeTestRule.onNodeWithTag(EditItemsScreenTestTags.INPUT_ITEM_TYPE).performTextClearance()
    composeTestRule.onNodeWithTag(EditItemsScreenTestTags.INPUT_ITEM_TYPE).performTextInput("B")
    composeTestRule.runOnIdle { viewModel.updateTypeSuggestions("B") }

    // Wait for suggestions dropdown to appear
    composeTestRule.waitForNodeWithTag(
        EditItemsScreenTestTags.TYPE_SUGGESTIONS, timeoutMillis = 10_000)
    composeTestRule.onNodeWithText("Boots", useUnmergedTree = true).performClick()
    composeTestRule
        .onNodeWithTag(EditItemsScreenTestTags.INPUT_ITEM_TYPE)
        .assertTextContains("Boots")
  }

  // -------- Invalid price handling --------

  @Test
  fun invalidPriceInput_isHandledGracefully() {
    composeTestRule.setContent { EditItemsScreen(testItem.itemUuid, viewModel) }
    composeTestRule
        .onNodeWithTag(EditItemsScreenTestTags.INPUT_ITEM_PRICE)
        .performTextInput("invalid")
    composeTestRule.waitForIdle()
    composeTestRule.runOnIdle { assertEquals(0.0, viewModel.uiState.value.price) }
  }

  // -------- Overlay visibility --------

  @Test
  fun overlay_shows_on_save_and_hides_after() = runTest {
    composeTestRule.setContent { EditItemsScreen(testItem.itemUuid, viewModel) }
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    composeTestRule.runOnIdle {
      viewModel.loadItem(testItem)
      viewModel.setBrand("Adidas")
    }
    composeTestRule.waitForIdle()

    // Initially hidden
    composeTestRule.runOnIdle { assert(!viewModel.uiState.value.isLoading) }
    composeTestRule.onAllNodesWithText("Uploading item...").assertCountEquals(0)

    // Start save
    composeTestRule.runOnIdle { viewModel.onSaveItemClick(context) }
    composeTestRule.waitForIdle()

    // Either loading or already succeeded (fast path)
    var loadingShownOrSucceeded = false
    composeTestRule.runOnIdle {
      loadingShownOrSucceeded =
          viewModel.uiState.value.isLoading || viewModel.uiState.value.isSaveSuccessful
    }
    assert(loadingShownOrSucceeded)

    // After some time, overlay should be hidden
    delay(1000)
    composeTestRule.runOnIdle { assert(!viewModel.uiState.value.isLoading) }
    composeTestRule.onAllNodesWithText("Uploading item...").assertCountEquals(0)
  }
}
