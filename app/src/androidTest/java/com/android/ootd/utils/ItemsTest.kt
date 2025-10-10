package com.android.ootd.utils

import android.net.Uri
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.espresso.action.ViewActions.swipeUp
import com.android.ootd.model.Item
import com.android.ootd.model.ItemsRepository
import com.android.ootd.model.ItemsRepositoryProvider
import com.android.ootd.model.Material
import com.android.ootd.ui.post.AddItemScreenTestTags
import com.android.ootd.ui.post.AddItemsViewModel
import org.junit.Before

interface ItemsTest {

  fun createInitializedRepository(): ItemsRepository

  val repository: ItemsRepository
    get() = ItemsRepositoryProvider.repository

  @Before
  fun setUp() {
    ItemsRepositoryProvider.repository = createInitializedRepository()
  }

  // Robustly bring a tagged node into view across devices where the list may or may not be
  // scrollable.
  fun ComposeTestRule.ensureVisible(tag: String) {
    // Already displayed? Nothing to do.
    val alreadyVisible =
        runCatching { onNodeWithTag(tag, useUnmergedTree = true).assertIsDisplayed() }.isSuccess
    if (alreadyVisible) return

    // 1) Try to scroll the node itself (uses bring-into-view chain).
    val scrolledChild =
        runCatching {
              onNodeWithTag(tag, useUnmergedTree = true).performScrollTo()
              waitForIdle()
            }
            .isSuccess
    if (scrolledChild) return

    // 2) Try to scroll the parent LazyColumn to the node.
    val scrolledParent =
        runCatching {
              onNodeWithTag(AddItemScreenTestTags.ALL_FIELDS).performScrollToNode(hasTestTag(tag))
              waitForIdle()
            }
            .isSuccess
    if (scrolledParent) return

    // 3) Fallback: manual swipes a few times (helps when semantics are quirky but visual scroll
    // still works).
    repeat(5) {
      onNodeWithTag(AddItemScreenTestTags.ALL_FIELDS).performTouchInput { swipeUp() }
      if (runCatching { onNodeWithTag(tag, useUnmergedTree = true).assertIsDisplayed() }.isSuccess)
          return
    }
  }

  // UI check for the button upload photo
  fun ComposeTestRule.enterAddItemPhoto(uri: Uri) {
    ensureVisible(AddItemScreenTestTags.IMAGE_PICKER)
    onNodeWithTag(AddItemScreenTestTags.IMAGE_PICKER).assertIsDisplayed()
  }

  fun ComposeTestRule.checkImageUploadButtonIsDisplayed() {
    ensureVisible(AddItemScreenTestTags.IMAGE_PICKER)
    onNodeWithTag(AddItemScreenTestTags.IMAGE_PICKER).assertIsDisplayed()
  }

  fun ComposeTestRule.checkImageUploadButtonClickable() {
    ensureVisible(AddItemScreenTestTags.IMAGE_PICKER)
    onNodeWithTag(AddItemScreenTestTags.IMAGE_PICKER).assertIsDisplayed().performClick()
  }

  fun ComposeTestRule.checkPhotoPreviewDisplayed() {
    ensureVisible(AddItemScreenTestTags.IMAGE_PICKER)
    onNodeWithTag(AddItemScreenTestTags.IMAGE_PREVIEW).assertIsDisplayed()
  }

  fun ComposeTestRule.enterAddItemType(type: String) {
    ensureVisible(AddItemScreenTestTags.INPUT_TYPE)
    onNodeWithTag(AddItemScreenTestTags.INPUT_TYPE, useUnmergedTree = true).performTextInput(type)
  }

  fun ComposeTestRule.enterAddItemCategory(category: String) {
    ensureVisible(AddItemScreenTestTags.IMAGE_PICKER)
    onNodeWithTag(AddItemScreenTestTags.INPUT_CATEGORY, useUnmergedTree = true)
        .performTextInput(category)
  }

  fun ComposeTestRule.enterAddItemBrand(brand: String) {
    ensureVisible(AddItemScreenTestTags.INPUT_BRAND)
    onNodeWithTag(AddItemScreenTestTags.INPUT_BRAND, useUnmergedTree = true).performTextInput(brand)
  }

  fun ComposeTestRule.enterAddItemPrice(price: Double) {
    ensureVisible(AddItemScreenTestTags.INPUT_PRICE)
    onNodeWithTag(AddItemScreenTestTags.INPUT_PRICE, useUnmergedTree = true)
        .performTextInput(price.toString())
  }

  fun ComposeTestRule.enterAddItemLink(link: String) {
    ensureVisible(AddItemScreenTestTags.IMAGE_PICKER)

    onNodeWithTag(AddItemScreenTestTags.INPUT_LINK, useUnmergedTree = true).performTextInput(link)
  }

  fun ComposeTestRule.enterAddItemMaterial(material: String) {

    ensureVisible(AddItemScreenTestTags.INPUT_MATERIAL)

    onNodeWithTag(AddItemScreenTestTags.INPUT_MATERIAL, useUnmergedTree = true)
        .performTextInput(material)
  }

  fun ComposeTestRule.enterAddItemDetails(item: Item) {
    enterAddItemType(item.type)
    enterAddItemCategory(item.category)
    enterAddItemBrand(item.brand)
    enterAddItemPrice(item.price)
    enterAddItemLink(item.link)
    enterAddItemPhoto(item.image)
    item.material.forEach { enterAddItemMaterial(it.name) }
  }

  fun ComposeTestRule.verifyImageUploadFlow(viewModel: AddItemsViewModel, uri: Uri) {
    // Step 1: Verify placeholder icon visible initially
    onNodeWithContentDescription("Placeholder icon").assertExists().assertIsDisplayed()

    // Step 2: Simulate picking a photo (what the gallery launcher would do)
    runOnIdle { viewModel.setPhoto(uri) }

    // Step 3: Wait for recomposition
    waitForIdle()

    // Step 4: Check placeholder disappears
    onAllNodesWithContentDescription("Placeholder icon").assertCountEquals(0)

    // Step 5: Verify selected image preview is now displayed
    onNodeWithContentDescription("Selected photo").assertExists().assertIsDisplayed()
  }

  fun Item.isEqual(other: Item): Boolean {
    return this.category == other.category &&
        this.type == other.type &&
        this.brand == other.brand &&
        this.price == other.price &&
        this.link == other.link &&
        this.material.size == other.material.size &&
        this.material.zip(other.material).all { (m1, m2) ->
          m1.name == m2.name && m1.percentage == m2.percentage
        }
  }

  companion object {
    val item1 =
        Item(
            uuid = "0",
            image = Uri.parse("https://example.com/image1.jpg"),
            category = "Clothes",
            type = "t-shirt",
            brand = "Mango",
            price = 0.0,
            material = listOf(),
            link = "https://example.com/item1")

    val item2 =
        Item(
            uuid = "1",
            image = Uri.parse("https://example.com/image1.jpg"),
            category = "shoes",
            type = "high heels",
            brand = "Zara",
            price = 30.0,
            material = listOf(),
            link = "https://example.com/item2")

    val item3 =
        Item(
            uuid = "2",
            image = Uri.parse("https://example.com/image1.jpg"),
            category = "bags",
            type = "handbag",
            brand = "Vakko",
            price = 0.0,
            material = listOf(),
            link = "https://example.com/item3")

    val item4 =
        Item(
            uuid = "3",
            image = Uri.parse("https://example.com/image1.jpg"),
            category = "accessories",
            type = "sunglasses",
            brand = "Ray-Ban",
            price = 100.0,
            material =
                listOf(
                    Material(name = "Plastic", percentage = 80.0),
                    Material(name = "Metal", percentage = 20.0)),
            link = "https://example.com/item4")
  }
}
