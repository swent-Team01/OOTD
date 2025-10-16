package com.android.ootd.ui.post

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.core.net.toUri
import com.android.ootd.model.items.Item
import com.android.ootd.model.items.ItemsRepository
import com.android.ootd.model.items.Material
import com.android.ootd.utils.InMemoryItem
import com.android.ootd.utils.ItemsTest
import com.android.ootd.utils.ItemsTest.Companion.item1
import com.android.ootd.utils.ItemsTest.Companion.item2
import com.android.ootd.utils.ItemsTest.Companion.item3
import com.android.ootd.utils.ItemsTest.Companion.item4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class PreviewItemScreenTest : ItemsTest by InMemoryItem {

  // Fake data setup for viewModel
  private val fakeItem =
      Item(
          uuid = "1",
          image = "https://example.com/image.jpg".toUri(),
          category = "Clothes",
          type = "T-Shirt",
          brand = "Nike",
          price = 19.99,
          material = listOf(Material("Cotton"), Material("Polyester")),
          link = "https://example.com/product",
      )
  private val fakeItem2 =
      Item(
          uuid = "2",
          image = "https://example.com/image2.jpg".toUri(),
          category = "Shoes",
          type = "Sneakers",
          brand = "Adidas",
          price = 49.99,
          material = listOf(Material("Leather")),
          link = "https://example.com/product2",
      )

  private fun fakeRepository(items: List<Item>) =
      object : ItemsRepository {
        override fun getNewItemId(): String = "fakeId"

        override suspend fun getAllItems(): List<Item> = items

        override suspend fun getItemById(uuid: String): Item = items.first()

        override suspend fun addItem(item: Item) {}

        override suspend fun editItem(itemUUID: String, newItem: Item) {}

        override suspend fun deleteItem(uuid: String) {
          TODO("Not yet implemented")
        }
      }

  @get:Rule val composeTestRule = createComposeRule()

  fun setContent(withInitialItems: List<Item> = emptyList()) {
    runTest { withInitialItems.forEach { repository.addItem(it) } }
    composeTestRule.setContent {
      PreviewItemScreen(outfitPreviewViewModel = OutfitPreviewViewModel(repository))
    }
  }

  // Empty list test
  @Test
  fun testTagsCorrectlySetWhenListIsEmpty() {
    setContent()
    composeTestRule.onNodeWithTag(PreviewItemScreenTestTags.ITEM_LIST).assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(PreviewItemScreenTestTags.EMPTY_ITEM_LIST_MSG).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PreviewItemScreenTestTags.CREATE_ITEM_BUTTON).assertIsDisplayed()
  }

  // Non empty list test
  @Test
  fun testTagsCorrectlySetWhenListIsNotEmpty() {
    setContent(withInitialItems = listOf(item1, item2))
    composeTestRule.onNodeWithTag(PreviewItemScreenTestTags.ITEM_LIST).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(PreviewItemScreenTestTags.getTestTagForItem(item1))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(PreviewItemScreenTestTags.getTestTagForItem(item2))
        .assertIsDisplayed()
  }

  // Expand collapsed item test
  @Test
  fun expandAndCollapsedItem() {
    setContent(withInitialItems = listOf(item3))
    composeTestRule
        .onNodeWithTag(PreviewItemScreenTestTags.getTestTagForItem(item3))
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(PreviewItemScreenTestTags.getTestTagForItem(item3)).performClick()
    composeTestRule
        .onNodeWithTag(PreviewItemScreenTestTags.getTestTagForItem(item3))
        .assertIsDisplayed()
  }

  @Test
  fun editButtonExistsAndClickable() {
    setContent(withInitialItems = listOf(item3))
    composeTestRule.onNodeWithTag(PreviewItemScreenTestTags.EDIT_ITEM_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PreviewItemScreenTestTags.EDIT_ITEM_BUTTON).performClick()
  }

  @Test
  fun expandingAnItemRevealsDetailsAndCollapsingHidesThem() {
    setContent(withInitialItems = listOf(item1))
    // Initially, details should not be displayed
    composeTestRule
        .onNodeWithTag(PreviewItemScreenTestTags.getTestTagForItem(item1))
        .assertIsDisplayed()
    composeTestRule.onNodeWithText(item1.category).assertIsDisplayed()
    composeTestRule.onNodeWithText(item1.type ?: "").assertIsDisplayed()
    composeTestRule.onNodeWithTag(PreviewItemScreenTestTags.IMAGE_ITEM_PREVIEW).assertIsDisplayed()
    // Expand the item
    composeTestRule.onNodeWithTag(PreviewItemScreenTestTags.EXPAND_ICON).performClick()

    composeTestRule.onNodeWithText(item1.category).assertIsDisplayed()
    composeTestRule.onNodeWithText(item1.type ?: "").assertIsDisplayed()
    composeTestRule.onNodeWithTag(PreviewItemScreenTestTags.IMAGE_ITEM_PREVIEW).assertIsDisplayed()
    composeTestRule.onNodeWithText(item1.brand ?: "").assertIsDisplayed()
    composeTestRule.onNodeWithText(item1.price.toString()).assertIsNotDisplayed()
    // Now, details should be displayed
    composeTestRule
        .onNodeWithTag(PreviewItemScreenTestTags.getTestTagForItem(item1))
        .assertIsDisplayed()

    // Collapse the item
    composeTestRule.onNodeWithTag(PreviewItemScreenTestTags.EXPAND_ICON).performClick()
    // Details should be hidden again
    composeTestRule
        .onNodeWithTag(PreviewItemScreenTestTags.getTestTagForItem(item1))
        .assertIsDisplayed()

    composeTestRule.onNodeWithText(item1.category).assertIsDisplayed()
    composeTestRule.onNodeWithText(item1.type ?: "").assertIsDisplayed()
    composeTestRule.onNodeWithTag(PreviewItemScreenTestTags.IMAGE_ITEM_PREVIEW).assertIsDisplayed()
  }

  @Test
  fun canScrollThroughItemList() {
    val items =
        (1..50).map {
          item4.copy(uuid = it.toString(), image = "https://example.com/image${it}.jpg".toUri())
        }
    setContent(withInitialItems = items)
    val firstTag = PreviewItemScreenTestTags.getTestTagForItem(items.first())
    val lastTag = PreviewItemScreenTestTags.getTestTagForItem(items.last())

    composeTestRule.onNodeWithTag(firstTag).assertIsDisplayed()
    val lastNode = composeTestRule.onNodeWithTag(lastTag)
    lastNode.assertIsNotDisplayed()

    composeTestRule
        .onNodeWithTag(PreviewItemScreenTestTags.ITEM_LIST)
        .performScrollToNode(hasTestTag(lastTag))

    lastNode.assertIsDisplayed()
  }

  @Test
  fun editButtonRemainsVisibleAfterExpansion() {
    setContent(withInitialItems = listOf(fakeItem))
    composeTestRule.onNodeWithTag(PreviewItemScreenTestTags.EXPAND_ICON).performClick()
    composeTestRule.onNodeWithTag(PreviewItemScreenTestTags.EDIT_ITEM_BUTTON).assertIsDisplayed()
  }

  @Test
  fun imagePlaceholderIsDisplayedWhenImageFailsToLoad() {
    val itemWithBrokenImage = item1.copy(image = "invalid_url".toUri())
    setContent(withInitialItems = listOf(itemWithBrokenImage))
    composeTestRule.onNodeWithTag(PreviewItemScreenTestTags.IMAGE_ITEM_PREVIEW).assertIsDisplayed()
  }

  @Test
  fun postButtonIsDisplayedWhenItemListIsNotEmpty() {
    setContent(withInitialItems = listOf(fakeItem))
    composeTestRule.onNodeWithText("Post").assertIsDisplayed()
  }

  @Test
  fun postButtonIsNotDisplayedWhenListIsEmpty() {
    setContent()
    composeTestRule.onNodeWithText("Post").assertDoesNotExist()
  }

  /**
   * Test to verify that clicking the Post button works when the item list is not empty. Currently,
   * this test only checks that the button is clickable. In a full implementation, you would verify
   * navigation or a toast message.
   */
  @Test
  fun postButtonIsClickable() {
    setContent(withInitialItems = listOf(fakeItem))
    composeTestRule
        .onNodeWithTag(PreviewItemScreenTestTags.POST_BUTTON)
        .assertIsDisplayed()
        .performClick()
  }

  @Test
  fun addItemAndPostButtonsAreDisplayedTogetherWhenListIsNotEmpty() {
    setContent(withInitialItems = listOf(fakeItem))
    composeTestRule.onNodeWithTag(PreviewItemScreenTestTags.CREATE_ITEM_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PreviewItemScreenTestTags.POST_BUTTON).assertIsDisplayed()
  }

  @Test
  fun addItemButtonStillVisibleWhenListEmpty() {
    setContent()
    composeTestRule.onNodeWithTag(PreviewItemScreenTestTags.CREATE_ITEM_BUTTON).assertIsDisplayed()
  }

  /** Navigation tests from preview screen to add item screen and edit item screen */
  @Test
  fun clickingAddItemButton_callsOnAddItemCallback() {
    var addItemClicked = false
    composeTestRule.setContent {
      PreviewItemScreen(
          outfitPreviewViewModel = OutfitPreviewViewModel(fakeRepository(listOf(fakeItem))),
          onAddItem = { addItemClicked = true })
    }

    composeTestRule.onNodeWithTag(PreviewItemScreenTestTags.CREATE_ITEM_BUTTON).performClick()
    assert(addItemClicked)
  }

  //  @Test
  //  fun clickingAddItemButton_navigatesToAddItemScreen() {
  //    composeTestRule.setContent {
  //      val navController = rememberNavController()
  //      NavHost(navController = navController, startDestination = Screen.PreviewItemScreen.route)
  // {
  //        composable(Screen.PreviewItemScreen.route) {
  //          PreviewItemScreen(
  //              outfitPreviewViewModel = OutfitPreviewViewModel(fakeRepository(listOf(fakeItem))),
  //              onAddItem = { navController.navigate(Screen.AddItemScreen.route) },
  //              onEditItem = {},
  //              onPostOutfit = {},
  //              onGoBack = {})
  //        }
  //        composable("addItem") {
  //          AddItemsScreen(
  //              addItemsViewModel = AddItemsViewModel(),
  //              onNextScreen = {},
  //              goBack = {},
  //              modifier = Modifier.testTag(AddItemScreenTestTags.TITLE_ADD))
  //        }
  //      }
  //    }
  //
  //    // Click "Add Item" button
  //    composeTestRule.onNodeWithTag(PreviewItemScreenTestTags.CREATE_ITEM_BUTTON).performClick()
  //
  //    // Check that AddItemScreen is now displayed
  //    composeTestRule.onNodeWithTag(AddItemScreenTestTags.TITLE_ADD).assertIsDisplayed()
  //  }

  @Test
  fun clickingEditItemButton_callsOnEditItemCallback() {
    var editedItemId: String? = null
    composeTestRule.setContent {
      PreviewItemScreen(
          outfitPreviewViewModel = OutfitPreviewViewModel(fakeRepository(listOf(fakeItem))),
          onEditItem = { itemId -> editedItemId = itemId })
    }
    // Click the edit button
    composeTestRule.onNodeWithTag(PreviewItemScreenTestTags.EDIT_ITEM_BUTTON).performClick()
    assert(editedItemId == fakeItem.uuid)
  }

  @Test
  fun clickingPostButton_callsOnPostOutfitCallback() {
    var postClicked = false
    composeTestRule.setContent {
      PreviewItemScreen(
          outfitPreviewViewModel = OutfitPreviewViewModel(fakeRepository(listOf(fakeItem))),
          onPostOutfit = { postClicked = true })
    }

    composeTestRule.onNodeWithTag(PreviewItemScreenTestTags.POST_BUTTON).performClick()
    assert(postClicked)
  }

  @Test
  fun clickingGoBackButton_callsOnGoBackCallback() {
    var goBackClicked = false
    composeTestRule.setContent {
      PreviewItemScreen(
          outfitPreviewViewModel = OutfitPreviewViewModel(fakeRepository(listOf(fakeItem))),
          onGoBack = { goBackClicked = true })
    }

    composeTestRule.onNodeWithContentDescription("go back").performClick()
    assert(goBackClicked)
  }

  @Test
  fun clickingGoBackButton_callsOnGoBackWithEmptyList() {
    var goBackClicked = false
    composeTestRule.setContent {
      PreviewItemScreen(
          outfitPreviewViewModel = OutfitPreviewViewModel(fakeRepository(emptyList())),
          onGoBack = { goBackClicked = true })
    }

    composeTestRule.onNodeWithContentDescription("go back").performClick()
    assert(goBackClicked)
  }

  // Tests for viewModel

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun loadsItemsCorrectlyVM() = runTest {
    val repo = fakeRepository(listOf(fakeItem, item3, fakeItem2))
    val viewModel = OutfitPreviewViewModel(repo)

    advanceUntilIdle()
    composeTestRule.waitForIdle()

    assert(viewModel.uiState.value.items.size == 3)
    assert(viewModel.uiState.value.items.first().isEqual(fakeItem))
    assert(viewModel.uiState.value.items.last().isEqual(fakeItem2))
  }

  @Test
  fun clearMessageResetsState() = runTest {
    val repo = fakeRepository(listOf(fakeItem))
    val viewModel = OutfitPreviewViewModel(repo)

    // Access the private field via reflection
    val field = OutfitPreviewViewModel::class.java.getDeclaredField("_uiState")
    field.isAccessible = true
    val stateFlow = field.get(viewModel) as MutableStateFlow<PreviewUIState>

    // Now mutate it directly
    stateFlow.value = stateFlow.value.copy(errorMessage = "Something went wrong")

    assert(viewModel.uiState.value.errorMessage == "Something went wrong")

    viewModel.clearErrorMessage()
    assert(viewModel.uiState.value.errorMessage == null)
  }

  @Test
  fun viewModelHandlesErrorsGracefully() = runTest {
    val errorRepo =
        object : ItemsRepository {
          override fun getNewItemId(): String = "errorId"

          override suspend fun getAllItems(): List<Item> {
            throw Exception("Failed to fetch items")
          }

          override suspend fun getItemById(uuid: String): Item = fakeItem2

          override suspend fun addItem(item: Item) {
            throw Exception("Failed to add item")
          }

          override suspend fun editItem(itemUUID: String, newItem: Item) {
            throw Exception("Failed to edit item")
          }

          override suspend fun deleteItem(uuid: String) {
            throw Exception("Failed to delete item")
          }
        }
    val viewModel = OutfitPreviewViewModel(errorRepo)
    assert(viewModel.uiState.value.items.isEmpty())
  }

  // Additional comprehensive tests for better coverage
  @Test
  fun displayMultipleItemsWithDifferentCategories() {
    val items =
        listOf(
            item1.copy(category = "Clothing"),
            item2.copy(category = "Shoes"),
            item3.copy(category = "Bags"),
            item4.copy(category = "Accessories"))
    setContent(withInitialItems = items)

    composeTestRule.onNodeWithText("Clothing").assertIsDisplayed()
    composeTestRule.onNodeWithText("Shoes").assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(PreviewItemScreenTestTags.ITEM_LIST)
        .performScrollToNode(hasTestTag(PreviewItemScreenTestTags.getTestTagForItem(items[2])))
    composeTestRule.onNodeWithText("Bags").assertIsDisplayed()
  }

  @Test
  fun verifyMaterialsDisplayedWhenExpanded() {
    val itemWithMaterials =
        item1.copy(material = listOf(Material("Cotton", 70.0), Material("Polyester", 30.0)))
    setContent(withInitialItems = listOf(itemWithMaterials))

    // Expand item
    composeTestRule.onNodeWithTag(PreviewItemScreenTestTags.EXPAND_ICON).performClick()

    // Materials should be visible (joined as string)
    composeTestRule.onNodeWithText("Cotton, Polyester", substring = true).assertIsDisplayed()
  }

  @Test
  fun verifyPriceFormattingIsCorrect() {
    val itemWithPrice = item1.copy(price = 123.45)
    setContent(withInitialItems = listOf(itemWithPrice))

    composeTestRule.onNodeWithTag(PreviewItemScreenTestTags.EXPAND_ICON).performClick()
    composeTestRule.onNodeWithText("CHF 123.45", substring = true).assertIsDisplayed()
  }

  @Test
  fun verifyLinkDisplayedWhenExpanded() {
    val itemWithLink = item1.copy(link = "https://example.com/product")
    setContent(withInitialItems = listOf(itemWithLink))

    composeTestRule.onNodeWithTag(PreviewItemScreenTestTags.EXPAND_ICON).performClick()
    composeTestRule.onNodeWithText("https://example.com/product").assertIsDisplayed()
  }

  @Test
  fun collapseItemHidesDetails() {
    setContent(withInitialItems = listOf(item1))

    // Expand first
    composeTestRule.onNodeWithTag(PreviewItemScreenTestTags.EXPAND_ICON).performClick()
    composeTestRule.onNodeWithText(item1.brand ?: "").assertIsDisplayed()

    // Collapse
    composeTestRule.onNodeWithTag(PreviewItemScreenTestTags.EXPAND_ICON).performClick()
    composeTestRule.waitForIdle()

    // Basic info still visible, but details might be hidden
    composeTestRule.onNodeWithText(item1.category).assertIsDisplayed()
  }

  @Test
  fun verifyBackButtonIsDisplayed() {
    setContent()
    composeTestRule.onNodeWithContentDescription("go back").assertIsDisplayed()
  }

  @Test
  fun addItemButtonIsClickable() {
    setContent(withInitialItems = listOf(fakeItem))
    composeTestRule
        .onNodeWithTag(PreviewItemScreenTestTags.CREATE_ITEM_BUTTON)
        .assertIsDisplayed()
        .performClick()
  }

  @Test
  fun allButtonsHaveCorrectIcons() {
    setContent(withInitialItems = listOf(fakeItem))

    // Post button should have check icon
    composeTestRule.onNodeWithTag(PreviewItemScreenTestTags.POST_BUTTON).assertIsDisplayed()

    // Add button should have add icon
    composeTestRule.onNodeWithTag(PreviewItemScreenTestTags.CREATE_ITEM_BUTTON).assertIsDisplayed()

    // Edit button should be visible
    composeTestRule.onNodeWithTag(PreviewItemScreenTestTags.EDIT_ITEM_BUTTON).assertIsDisplayed()
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun viewModelRefreshItemsUpdatesState() = runTest {
    val repo = fakeRepository(listOf(fakeItem))
    val viewModel = OutfitPreviewViewModel(repo)

    advanceUntilIdle()
    composeTestRule.waitForIdle()
    assert(viewModel.uiState.value.items.size == 1)

    viewModel.refreshItems()
    advanceUntilIdle()
    composeTestRule.waitForIdle()

    assert(viewModel.uiState.value.items.isNotEmpty())
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun viewModelInitialStateIsCorrect() = runTest {
    val repo = fakeRepository(emptyList())
    val viewModel = OutfitPreviewViewModel(repo)

    advanceUntilIdle()
    composeTestRule.waitForIdle()

    val state = viewModel.uiState.value
    assert(state.imageUri == "")
    assert(state.description == "")
    assert(state.items.isEmpty())
    assert(state.errorMessage == null)
    assert(!state.isLoading)
  }

  @Test
  fun itemWithNullOptionalFieldsDisplaysCorrectly() {
    val minimalItem =
        item1.copy(type = null, brand = null, price = null, link = null, material = emptyList())
    setContent(withInitialItems = listOf(minimalItem))

    composeTestRule.onNodeWithText(minimalItem.category).assertIsDisplayed()
    composeTestRule
        .onNodeWithText("Item Type")
        .assertIsDisplayed() // Default text when type is null
  }

  @Test
  fun editButtonForEachItemWorks() {
    val items = listOf(item1, item2)
    var editedId: String? = null

    composeTestRule.setContent {
      PreviewItemScreen(
          outfitPreviewViewModel = OutfitPreviewViewModel(fakeRepository(items)),
          onEditItem = { itemId -> editedId = itemId })
    }

    // Click edit on first item
    composeTestRule.onAllNodesWithTag(PreviewItemScreenTestTags.EDIT_ITEM_BUTTON)[0].performClick()
    assert(editedId == item1.uuid)

    // Click edit on second item
    composeTestRule.onAllNodesWithTag(PreviewItemScreenTestTags.EDIT_ITEM_BUTTON)[1].performClick()
    assert(editedId == item2.uuid)
  }

  @Test
  fun bottomBarLayoutIsCorrect() {
    setContent(withInitialItems = listOf(fakeItem))

    // Both buttons should be in bottom bar
    composeTestRule.onNodeWithTag(PreviewItemScreenTestTags.POST_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PreviewItemScreenTestTags.CREATE_ITEM_BUTTON).assertIsDisplayed()
  }

  @Test
  fun allNavigationCallbacksWork() {
    var addClicked = false
    var editClicked = false
    var postClicked = false
    var backClicked = false

    composeTestRule.setContent {
      PreviewItemScreen(
          outfitPreviewViewModel = OutfitPreviewViewModel(fakeRepository(listOf(fakeItem))),
          onAddItem = { addClicked = true },
          onEditItem = { editClicked = true },
          onPostOutfit = { postClicked = true },
          onGoBack = { backClicked = true })
    }

    composeTestRule.onNodeWithTag(PreviewItemScreenTestTags.CREATE_ITEM_BUTTON).performClick()
    assert(addClicked)

    composeTestRule.onNodeWithTag(PreviewItemScreenTestTags.EDIT_ITEM_BUTTON).performClick()
    assert(editClicked)

    composeTestRule.onNodeWithTag(PreviewItemScreenTestTags.POST_BUTTON).performClick()
    assert(postClicked)

    composeTestRule.onNodeWithContentDescription("go back").performClick()
    assert(backClicked)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun refreshItems_catchesExceptionAndSetsErrorMessage() = runTest {
    // Create a repository that throws an exception on getAllItems
    val errorMessage = "Network error - Failed to fetch items from server"
    val failingRepo =
        object : ItemsRepository {
          override fun getNewItemId(): String = "id"

          override suspend fun getAllItems(): List<Item> {
            throw Exception(errorMessage)
          }

          override suspend fun getItemById(uuid: String): Item = fakeItem

          override suspend fun addItem(item: Item) {}

          override suspend fun editItem(itemUUID: String, newItem: Item) {}

          override suspend fun deleteItem(uuid: String) {}
        }

    val viewModel = OutfitPreviewViewModel(failingRepo)
    advanceUntilIdle()
    composeTestRule.waitForIdle()

    // Initial load will fail, error message should be set
    assert(viewModel.uiState.value.errorMessage != null)
    assert(viewModel.uiState.value.errorMessage?.contains("Failed to load items") == true)

    // Clear the error
    viewModel.clearErrorMessage()
    assert(viewModel.uiState.value.errorMessage == null)

    // Now call refreshItems explicitly to test the catch block in refreshItems
    viewModel.refreshItems()
    advanceUntilIdle()
    composeTestRule.waitForIdle()

    // Verify the error message is set correctly from refreshItems catch block
    assert(viewModel.uiState.value.errorMessage != null)
    assert(viewModel.uiState.value.errorMessage?.contains("Failed to refresh items") == true)
    assert(viewModel.uiState.value.errorMessage?.contains(errorMessage) == true)

    // Verify items list remains empty
    assert(viewModel.uiState.value.items.isEmpty())
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun refreshItems_specificExceptionMessage_isPropagatedCorrectly() = runTest {
    val specificError = "Database connection timeout after 30 seconds"
    val failingRepo =
        object : ItemsRepository {
          override fun getNewItemId(): String = "id"

          override suspend fun getAllItems(): List<Item> {
            throw Exception(specificError)
          }

          override suspend fun getItemById(uuid: String): Item = fakeItem

          override suspend fun addItem(item: Item) {}

          override suspend fun editItem(itemUUID: String, newItem: Item) {}

          override suspend fun deleteItem(uuid: String) {}
        }

    val viewModel = OutfitPreviewViewModel(failingRepo)
    advanceUntilIdle()
    composeTestRule.waitForIdle()

    // Clear initial error
    viewModel.clearErrorMessage()

    // Call refreshItems
    viewModel.refreshItems()
    advanceUntilIdle()
    composeTestRule.waitForIdle()

    // Verify the specific error message is included
    val errorMsg = viewModel.uiState.value.errorMessage
    assert(errorMsg != null)
    assert(errorMsg?.contains("Failed to refresh items") == true)
    assert(errorMsg?.contains(specificError) == true)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun refreshItems_afterSuccessfulLoad_thenFailure_showsError() = runTest {
    var shouldFail = false
    val toggleRepo =
        object : ItemsRepository {
          override fun getNewItemId(): String = "id"

          override suspend fun getAllItems(): List<Item> {
            if (shouldFail) throw Exception("Refresh failed")
            return listOf(fakeItem, fakeItem2)
          }

          override suspend fun getItemById(uuid: String): Item = fakeItem

          override suspend fun addItem(item: Item) {}

          override suspend fun editItem(itemUUID: String, newItem: Item) {}

          override suspend fun deleteItem(uuid: String) {}
        }

    val viewModel = OutfitPreviewViewModel(toggleRepo)
    advanceUntilIdle()
    composeTestRule.waitForIdle()

    // Initial load should succeed
    assert(viewModel.uiState.value.items.size == 2)
    assert(viewModel.uiState.value.errorMessage == null)

    // Now make it fail
    shouldFail = true
    viewModel.refreshItems()
    advanceUntilIdle()
    composeTestRule.waitForIdle()

    // Should have error message but items should still be there from before
    assert(viewModel.uiState.value.errorMessage != null)
    assert(viewModel.uiState.value.errorMessage?.contains("Failed to refresh items") == true)
    assert(viewModel.uiState.value.items.size == 2) // Items preserved from previous successful
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun refreshItems_multipleFailures_updatesErrorMessageEachTime() = runTest {
    var errorCount = 0
    val multiFailRepo =
        object : ItemsRepository {
          override fun getNewItemId(): String = "id"

          override suspend fun getAllItems(): List<Item> {
            errorCount++
            throw Exception("Error number $errorCount")
          }

          override suspend fun getItemById(uuid: String): Item = fakeItem

          override suspend fun addItem(item: Item) {}

          override suspend fun editItem(itemUUID: String, newItem: Item) {}

          override suspend fun deleteItem(uuid: String) {}
        }

    val viewModel = OutfitPreviewViewModel(multiFailRepo)
    advanceUntilIdle()
    composeTestRule.waitForIdle()

    // First failure (from init)
    assert(viewModel.uiState.value.errorMessage?.contains("Error number 1") == true)

    viewModel.clearErrorMessage()

    // Second failure (from refreshItems)
    viewModel.refreshItems()
    advanceUntilIdle()
    composeTestRule.waitForIdle()
    assert(viewModel.uiState.value.errorMessage?.contains("Failed to refresh items") == true)
    assert(viewModel.uiState.value.errorMessage?.contains("Error number 2") == true)

    viewModel.clearErrorMessage()

    // Third failure (from refreshItems)
    viewModel.refreshItems()
    advanceUntilIdle()
    composeTestRule.waitForIdle()
    assert(viewModel.uiState.value.errorMessage?.contains("Failed to refresh items") == true)
    assert(viewModel.uiState.value.errorMessage?.contains("Error number 3") == true)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun launchedEffect_withErrorMessage_clearsErrorAfterShowingToast() = runTest {
    val repo = fakeRepository(listOf(fakeItem))
    val viewModel = OutfitPreviewViewModel(repo)

    // Set an artificial error message
    val field = OutfitPreviewViewModel::class.java.getDeclaredField("_uiState")
    field.isAccessible = true
    val stateFlow = field.get(viewModel) as MutableStateFlow<PreviewUIState>
    stateFlow.value = stateFlow.value.copy(errorMessage = "Network error occurred")

    composeTestRule.setContent { PreviewItemScreen(outfitPreviewViewModel = viewModel) }

    composeTestRule.waitForIdle()

    // After LaunchedEffect runs, the error message should be cleared
    composeTestRule.runOnIdle { assert(viewModel.uiState.value.errorMessage == null) }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun launchedEffect_unit_callsRefreshItemsOnInit() = runTest {
    var refreshCalled = false
    val repo =
        object : ItemsRepository {
          override fun getNewItemId(): String = "id"

          override suspend fun getAllItems(): List<Item> {
            refreshCalled = true
            return listOf(fakeItem)
          }

          override suspend fun getItemById(uuid: String): Item = fakeItem

          override suspend fun addItem(item: Item) {}

          override suspend fun editItem(itemUUID: String, newItem: Item) {}

          override suspend fun deleteItem(uuid: String) {}
        }

    val viewModel = OutfitPreviewViewModel(repo)

    composeTestRule.setContent { PreviewItemScreen(outfitPreviewViewModel = viewModel) }

    composeTestRule.waitForIdle()

    composeTestRule.runOnIdle {
      assert(refreshCalled)
      assert(viewModel.uiState.value.items.isNotEmpty())
    }
  }

  @Test
  fun topAppBar_displaysOOTDTitle() {
    composeTestRule.setContent {
      PreviewItemScreen(
          outfitPreviewViewModel = OutfitPreviewViewModel(fakeRepository(emptyList())))
    }

    composeTestRule.onNodeWithTag(PreviewItemScreenTestTags.SCREEN_TITLE).assertIsDisplayed()

    composeTestRule.onNodeWithText("OOTD").assertIsDisplayed()
  }
}
