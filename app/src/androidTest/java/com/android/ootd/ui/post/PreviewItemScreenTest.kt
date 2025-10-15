package com.android.ootd.ui.post

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
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
}
